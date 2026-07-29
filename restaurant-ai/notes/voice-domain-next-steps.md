# Voice App And Domain Next Steps

## Current Voice Direction

The restaurant chatbot now has two phone-call paths:

- Basic Twilio voice fallback:
  - `POST /api/voice/{restaurantId}/incoming`
  - Uses TwiML `<Gather>` speech recognition and `<Say>` text-to-speech.
- Realtime voice prototype:
  - `POST /api/voice/{restaurantId}/incoming-realtime`
  - Returns TwiML `<Connect><Stream>` so Twilio opens a WebSocket.
  - WebSocket endpoint: `/api/voice/{restaurantId}/stream`
  - Bridges Twilio Media Streams audio to OpenAI Realtime.
  - Uses the existing restaurant RAG answer service to answer caller questions.

Twilio webhook for Harbor Steakhouse should eventually point to:

```text
POST https://YOUR_DOMAIN/api/voice/harbor-steakhouse/incoming-realtime
```

## Important Deployment Detail

Twilio does not connect to a raw app process directly. It calls a public HTTPS webhook URL. For the realtime path, Twilio also needs a public secure WebSocket URL:

```text
wss://YOUR_DOMAIN/api/voice/harbor-steakhouse/stream
```

This means the AWS deployment needs:

- A domain name pointing to the EC2 instance or load balancer.
- HTTPS/SSL configured.
- Reverse proxy support for WebSockets if using Nginx/Apache/Caddy.
- Port 443 open publicly.
- The Spring Boot app still running on its internal port, likely 8080.

## Likely Next Task

Point a domain to the current AWS EC2 deployment.

Likely steps:

1. Identify where DNS is managed.
2. Add an `A` record for the app domain/subdomain to the EC2 public IP, or use an alias/CNAME if behind a load balancer.
3. Configure HTTPS with a certificate.
4. Configure the reverse proxy to forward:
   - HTTP requests to Spring Boot on `localhost:8080`.
   - WebSocket upgrade requests for `/api/voice/*/stream`.
5. Test:
   - `https://YOUR_DOMAIN/api/health`
   - `https://YOUR_DOMAIN/widget-test.html`
   - Twilio webhook to `/api/voice/harbor-steakhouse/incoming-realtime`.

## Cost Notes

Approximate Twilio US costs checked May 29, 2026:

- Local phone number: about `$1.15/month`.
- Toll-free number: about `$2.15/month`.
- Local inbound voice: about `$0.0085/min`.
- Media Streams: about `$0.0040/min`.
- Local inbound + Media Streams: about `$0.0125/min`, plus number rental.

OpenAI Realtime uses the existing `OPENAI_API_KEY` if that project has realtime access. Cost is token-based and depends on caller/assistant audio length.
