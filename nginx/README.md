# NGINX API Gateway

This directory contains the NGINX/Lua gateway used by Docker Compose. The Helm chart carries a copy under `helm/team-evil-jenkins/files/` that is kept in sync — when changing `nginx.conf` or `oidc.lua`, apply the same change to both copies.

| File | Purpose |
|------|---------|
| `Dockerfile` | Builds the gateway image (OpenResty-compatible base + `lua-resty-openidc`, `lua-cjson`). |
| `nginx.conf` | Routes HTTP and WebSocket traffic to the backend services and handles CORS responses. |
| `oidc.lua` | Validates Auth0 bearer tokens for protected HTTP routes and injects the authenticated identity header. |

## Routing

- `/user` and `/user/` → `user-service`
- `/location` and `/location/` → `location-service`
- `/matching/` → `matching-service`
- `/genai/` → the FastAPI matching service
- `/messaging/`, `/ws`, `/ws/` → `messaging-service`
- `/healthz` → gateway health response
- Unmatched routes return `404`; the React client is served by its own container.

## Authentication boundary

Two modes are supported via the `AUTH_MODE` environment variable:

- **`auth0` (default)** — full JWT verification, described below. Used by the Helm deployment.
- **`dev`** — local development without an Auth0 tenant: `Authorization: Bearer dev-<subject>` sets `X-User-Id` to `<subject>`; anything else gets `401`. Only enable this on a local machine — never expose it publicly.

In `auth0` mode, `oidc.lua` runs on protected HTTP routes:

1. Clears incoming `X-User-Id` and `X-Service-Token` headers so clients cannot spoof the internal identity contract.
2. Answers `OPTIONS` preflight without auth.
3. Extracts the `Authorization: Bearer` token and verifies it via `lua-resty-openidc` (RS256 signature, issuer, audience, expiry) against Auth0 discovery/JWKS.
4. Sets `X-User-Id` to the verified token `sub` and proxies upstream; failures return `401`.

Backend services trust `X-User-Id` only because the gateway strips the client-supplied value first. Internal service-to-service calls authenticate separately with `X-Service-Token` (`INTERNAL_SERVICE_TOKEN`).

## WebSocket (`/ws`)

Browsers cannot send `Authorization` headers on the SockJS handshake, so `/ws` is proxied without gateway auth. Instead, the messaging service validates the JWT on the STOMP `CONNECT` frame (JWKS-verified, `sub` becomes the STOMP principal) and delivers messages to authenticated `/user/queue/messages` destinations. Handshake CORS is origin-reflective and the messaging endpoint restricts allowed origins via `ALLOWED_ORIGINS`.

## Notes

- The gateway binds port 80, so the container entrypoint runs as root; workers run as the unprivileged `nginx` user.
- `AUTH_MODE=dev` is intended only for local development — never expose a dev-mode gateway publicly.
