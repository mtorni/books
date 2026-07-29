package com.lunawave.restaurantai.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lunawave.restaurantai.service.RestaurantAskService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "/api/voice/{restaurantId}", produces = MediaType.APPLICATION_XML_VALUE)
public class VoiceController {

    private static final String VOICE = "alice";
    private static final String LANGUAGE = "en-US";

    private final RestaurantAskService askService;

    public VoiceController(RestaurantAskService askService) {
        this.askService = askService;
    }

    @PostMapping("/incoming")
    public String incoming(@PathVariable String restaurantId) {
        String restaurantName = toRestaurantName(restaurantId);
        String prompt = "Welcome to " + restaurantName
            + ". Ask me about the menu, drinks, hours, parking, or specials.";

        return response(
            gather(restaurantId, prompt)
                + redirect(restaurantId)
        );
    }

    @PostMapping("/incoming-realtime")
    public String incomingRealtime(@PathVariable String restaurantId, HttpServletRequest request) {
        return response("""
            <Connect>
                <Stream url="%s">
                    <Parameter name="restaurantId" value="%s" />
                </Stream>
            </Connect>
            """.formatted(realtimeStreamUrl(request, restaurantId), xmlAttribute(restaurantId)));
    }

    @PostMapping("/answer")
    public String answer(
        @PathVariable String restaurantId,
        @RequestParam(name = "SpeechResult", required = false) String speechResult
    ) {
        if (speechResult == null || speechResult.isBlank()) {
            return response(
                say("Sorry, I didn't catch that.")
                    + gather(restaurantId, "Please ask your question again.")
                    + redirect(restaurantId)
            );
        }

        String answer = toVoiceText(askService.answer(speechResult, restaurantId));

        return response(
            say(answer)
                + gather(restaurantId, "What else can I help you with?")
                + say("Thanks for calling. Goodbye.")
        );
    }

    private String gather(String restaurantId, String prompt) {
        return """
            <Gather input="speech" action="/api/voice/%s/answer" method="POST" speechTimeout="auto" timeout="5">
                %s
            </Gather>
            """.formatted(xmlAttribute(restaurantId), say(prompt));
    }

    private String redirect(String restaurantId) {
        return """
            <Redirect method="POST">/api/voice/%s/incoming</Redirect>
            """.formatted(xmlText(restaurantId));
    }

    private String realtimeStreamUrl(HttpServletRequest request, String restaurantId) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String host = forwardedHost == null || forwardedHost.isBlank()
            ? request.getHeader("Host")
            : forwardedHost;

        if (host == null || host.isBlank()) {
            host = request.getServerName();
            int port = request.getServerPort();
            if (port > 0 && port != 80 && port != 443) {
                host += ":" + port;
            }
        }

        boolean secure = "https".equalsIgnoreCase(forwardedProto) || request.isSecure();
        String scheme = secure ? "wss" : "ws";

        return "%s://%s/api/voice/%s/stream".formatted(
            scheme,
            host,
            xmlAttribute(restaurantId)
        );
    }

    private String say(String text) {
        return """
            <Say voice="%s" language="%s">%s</Say>
            """.formatted(VOICE, LANGUAGE, xmlText(text));
    }

    private String response(String body) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Response>
            %s
            </Response>
            """.formatted(body);
    }

    private String toVoiceText(String text) {
        if (text == null || text.isBlank()) {
            return "I'm sorry, I don't have an answer for that right now.";
        }

        return text
            .replaceAll("[\\r\\n]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String toRestaurantName(String restaurantId) {
        if (restaurantId == null || restaurantId.isBlank()) {
            return "the restaurant";
        }

        String[] parts = restaurantId.trim().split("[-_]+");
        StringBuilder name = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                name.append(part.substring(1).toLowerCase());
            }
        }

        return name.isEmpty() ? "the restaurant" : name.toString();
    }

    private String xmlText(String value) {
        return value == null ? "" : value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private String xmlAttribute(String value) {
        return xmlText(value)
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
