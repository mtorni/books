package com.lunawave.restaurantai.voice;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunawave.restaurantai.service.RestaurantAskService;

@Component
public class TwilioOpenAiRealtimeWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, CallBridge> bridges = new ConcurrentHashMap<>();
    private final ExecutorService answerExecutor = Executors.newCachedThreadPool();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper;
    private final RestaurantAskService askService;
    private final String openAiApiKey;
    private final String realtimeModel;
    private final String realtimeVoice;

    public TwilioOpenAiRealtimeWebSocketHandler(
        ObjectMapper objectMapper,
        RestaurantAskService askService,
        @Value("${spring.ai.openai.api-key:}") String openAiApiKey,
        @Value("${openai.realtime.model:gpt-realtime}") String realtimeModel,
        @Value("${openai.realtime.voice:marin}") String realtimeVoice
    ) {
        this.objectMapper = objectMapper;
        this.askService = askService;
        this.openAiApiKey = openAiApiKey;
        this.realtimeModel = realtimeModel;
        this.realtimeVoice = realtimeVoice;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String restaurantId = restaurantIdFromPath(session.getUri());
        CallBridge bridge = new CallBridge(session, restaurantId);
        bridges.put(session.getId(), bridge);
        bridge.connectOpenAi();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        CallBridge bridge = bridges.get(session.getId());
        if (bridge != null) {
            bridge.handleTwilioMessage(message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Optional.ofNullable(bridges.remove(session.getId())).ifPresent(CallBridge::close);
    }

    private String restaurantIdFromPath(URI uri) {
        if (uri == null || uri.getPath() == null) {
            return "";
        }

        String[] parts = uri.getPath().split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("voice".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }

        return "";
    }

    private final class CallBridge implements WebSocket.Listener {

        private final WebSocketSession twilioSession;
        private final String restaurantId;
        private final StringBuilder openAiTextBuffer = new StringBuilder();

        private volatile WebSocket openAiSocket;
        private volatile String streamSid;
        private volatile boolean openAiReady;

        private CallBridge(WebSocketSession twilioSession, String restaurantId) {
            this.twilioSession = twilioSession;
            this.restaurantId = restaurantId;
        }

        private void connectOpenAi() {
            if (openAiApiKey == null || openAiApiKey.isBlank() || openAiApiKey.startsWith("${")) {
                closeTwilio(CloseStatus.SERVER_ERROR);
                return;
            }

            String encodedModel = URLEncoder.encode(realtimeModel, StandardCharsets.UTF_8);
            URI uri = URI.create("wss://api.openai.com/v1/realtime?model=" + encodedModel);

            httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("OpenAI-Beta", "realtime=v1")
                .buildAsync(uri, this)
                .thenAccept(socket -> this.openAiSocket = socket)
                .exceptionally(error -> {
                    closeTwilio(CloseStatus.SERVER_ERROR);
                    return null;
                });
        }

        private void handleTwilioMessage(String payload) throws IOException {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText();

            switch (event) {
                case "start" -> {
                    streamSid = root.path("start").path("streamSid").asText();
                    maybeSendSessionUpdate();
                }
                case "media" -> sendOpenAiAudio(root.path("media").path("payload").asText());
                case "stop" -> close();
                default -> {
                    // connected, mark, and other Twilio events do not require action here.
                }
            }
        }

        private void maybeSendSessionUpdate() throws IOException {
            if (openAiSocket == null || streamSid == null || openAiReady) {
                return;
            }

            openAiReady = true;

            Map<String, Object> event = Map.of(
                "type", "session.update",
                "session", Map.of(
                    "type", "realtime",
                    "model", realtimeModel,
                    "instructions", """
                        You are the voice for a restaurant assistant.
                        Wait for the server to provide the restaurant answer text.
                        Speak warmly, clearly, and briefly.
                        """,
                    "output_modalities", java.util.List.of("audio"),
                    "audio", Map.of(
                        "input", Map.of(
                            "format", Map.of("type", "audio/pcmu"),
                            "transcription", Map.of(
                                "model", "gpt-4o-mini-transcribe",
                                "language", "en"
                            ),
                            "turn_detection", Map.of(
                                "type", "server_vad",
                                "threshold", 0.5,
                                "prefix_padding_ms", 300,
                                "silence_duration_ms", 650,
                                "create_response", false,
                                "interrupt_response", true
                            )
                        ),
                        "output", Map.of(
                            "format", Map.of("type", "audio/pcmu"),
                            "voice", realtimeVoice
                        )
                    )
                )
            );

            sendOpenAiJson(event);
        }

        private void sendOpenAiAudio(String base64Audio) throws IOException {
            if (base64Audio == null || base64Audio.isBlank() || openAiSocket == null) {
                return;
            }

            sendOpenAiJson(Map.of(
                "type", "input_audio_buffer.append",
                "audio", base64Audio
            ));
        }

        private void answerTranscript(String transcript) {
            if (transcript == null || transcript.isBlank()) {
                return;
            }

            answerExecutor.submit(() -> {
                try {
                    String answer = askService.answer(transcript, restaurantId);
                    sendOpenAiJson(Map.of(
                        "type", "response.create",
                        "response", Map.of(
                            "conversation", "none",
                            "output_modalities", java.util.List.of("audio"),
                            "instructions", "Speak the provided restaurant answer naturally. Do not add unrelated details.",
                            "input", java.util.List.of(Map.of(
                                "type", "message",
                                "role", "user",
                                "content", java.util.List.of(Map.of(
                                    "type", "input_text",
                                    "text", "Caller asked: " + transcript + "\n\nRestaurant answer to speak: " + answer
                                ))
                            ))
                        )
                    ));
                } catch (Exception e) {
                    try {
                        sendOpenAiJson(Map.of(
                            "type", "response.create",
                            "response", Map.of(
                                "conversation", "none",
                                "output_modalities", java.util.List.of("audio"),
                                "instructions", "Apologize briefly and say the restaurant assistant is having trouble right now."
                            )
                        ));
                    } catch (IOException ignored) {
                        closeTwilio(CloseStatus.SERVER_ERROR);
                    }
                }
            });
        }

        private void sendOpenAiJson(Map<String, Object> event) throws IOException {
            WebSocket socket = openAiSocket;
            if (socket == null) {
                return;
            }

            socket.sendText(objectMapper.writeValueAsString(event), true);
        }

        private void sendTwilioAudio(String base64Audio) throws IOException {
            if (streamSid == null || base64Audio == null || base64Audio.isBlank()) {
                return;
            }

            Map<String, Object> event = Map.of(
                "event", "media",
                "streamSid", streamSid,
                "media", Map.of("payload", base64Audio)
            );

            synchronized (twilioSession) {
                if (twilioSession.isOpen()) {
                    twilioSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
                }
            }
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            this.openAiSocket = webSocket;
            webSocket.request(1);
            try {
                maybeSendSessionUpdate();
            } catch (IOException e) {
                closeTwilio(CloseStatus.SERVER_ERROR);
            }
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            openAiTextBuffer.append(data);

            if (!last) {
                webSocket.request(1);
                return null;
            }

            String payload = openAiTextBuffer.toString();
            openAiTextBuffer.setLength(0);

            try {
                JsonNode root = objectMapper.readTree(payload);
                String type = root.path("type").asText();

                if ("conversation.item.input_audio_transcription.completed".equals(type)) {
                    answerTranscript(root.path("transcript").asText());
                } else if ("response.output_audio.delta".equals(type) || "response.audio.delta".equals(type)) {
                    sendTwilioAudio(root.path("delta").asText());
                } else if ("error".equals(type)) {
                    closeTwilio(CloseStatus.SERVER_ERROR);
                }
            } catch (Exception e) {
                closeTwilio(CloseStatus.SERVER_ERROR);
            } finally {
                webSocket.request(1);
            }

            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closeTwilio(CloseStatus.NORMAL);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            closeTwilio(CloseStatus.SERVER_ERROR);
        }

        private void close() {
            WebSocket socket = openAiSocket;
            if (socket != null) {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "Call ended");
            }
        }

        private void closeTwilio(CloseStatus status) {
            try {
                synchronized (twilioSession) {
                    if (twilioSession.isOpen()) {
                        twilioSession.close(status);
                    }
                }
            } catch (IOException ignored) {
                // Nothing useful to do after the call socket is already closing.
            }
        }
    }
}
