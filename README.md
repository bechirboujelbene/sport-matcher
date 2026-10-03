# Sport Matcher

Sport Matcher connects people who want to take part in outdoor sports. Users build a profile with sports, skill level, availability, and location, then discover compatible partners and message their contacts.

The primary engineering focus here is the delivery and operations path for a containerized microservice application: build, test, package, deploy, and observe the workload reproducibly.

## Project status

- The matching service defaults to a deterministic, explainable algorithm with no external LLM dependency.
- A frontend-only demo uses synthetic data and browser storage; it does not connect to backend services.
- Local runs authenticate with `AUTH_MODE=dev` — no external accounts required. Production deployments use Auth0 (`AUTH_MODE=auth0`).
- CI builds and tests every service, builds hardened container images, validates Compose/Helm/Terraform/Ansible, deploys the full chart to Kubernetes with health and rolling-upgrade checks, and publishes the demo to GitHub Pages. See [the workflows documentation](.github/workflows/README.md).

## What I built

- Gateway and security: NGINX gateway with Auth0 JWT validation, verified identity passed to the services, client-supplied identity headers stripped, per-endpoint access checks and an internal token for service-to-service calls
- Matching: the matching and location services, with deterministic, explainable matching by default and an optional LLM adapter
- Delivery: Kubernetes deployment with Helm, AWS infrastructure with Terraform and Ansible, and CI that tests every service, validates the infrastructure code and deploys the chart with health checks
- Demo: the frontend-only demo published to GitHub Pages

## Architecture

| Component | Technology | Responsibility |
|---|---|---|
| Client | React, Vite | Profile, matching, and messaging UI |
| API gateway | NGINX, Lua/OpenID Connect | Routes API traffic and validates Auth0 bearer tokens |
| User service | Spring Boot | User profiles |
| Location service | Spring Boot | User locations |
| Matching service | Spring Boot | Match workflows and history |
| GenAI service | FastAPI | Deterministic candidate ranking by default; optional Open WebUI adapter |
| Messaging service | Spring Boot, WebSocket/STOMP | Contacts and real-time messages |
| Persistence | PostgreSQL | Service data |
| Observability | Prometheus, Grafana | Metrics and dashboards |

## Security model

- **Browser → gateway.** The NGINX gateway validates the Auth0 JWT (RS256, signature + issuer/audience/expiry) using `lua-resty-openidc`. On success it strips any client-supplied identity headers and injects `X-User-Id` with the verified token `sub`. Unauthenticated or invalid requests get `401`. For local development, `AUTH_MODE=dev` instead accepts `dev-<subject>` bearer tokens so the stack runs without an Auth0 tenant; production deployments leave the default `auth0` mode.
- **Gateway → services.** Spring controllers treat `X-User-Id` as the authenticated identity and reject requests whose path/query/body user ID does not match it (`403`). The header only reaches services from the gateway; direct client injection is stripped.
- **Service → service.** Internal calls authenticate with a shared `X-Service-Token` (constant-time comparison), injected from `INTERNAL_SERVICE_TOKEN` env or the `internal-service-auth` Kubernetes secret. Endpoints that expose other users' data (e.g. `GET /user`, `GET /location/all`, `/location/nearby`) only accept the service token.
- **WebSocket.** The SockJS handshake cannot carry an `Authorization` header, so the gateway proxies `/ws` unauthenticated and the messaging service validates the JWT on the STOMP `CONNECT` frame instead (JWKS verified, `sub` becomes the STOMP principal). Messages are delivered to authenticated per-user queues (`/user/queue/messages`), not public conversation topics.
- **Secrets.** No credentials are committed. `.env.example` documents every variable; Auth0 browser values (`VITE_*`) are public configuration, while `INTERNAL_SERVICE_TOKEN`, database passwords, and Open WebUI keys are secrets. Public client config is injected at container startup via `runtime-config.js`, not baked into the image.

- **Network exposure.** Only the gateway is meant to be reachable. Backend services trust `X-User-Id` from the gateway, so their ports are bound to `127.0.0.1` locally (for debugging) and are cluster-internal (`ClusterIP`) in Kubernetes. On the EC2 deployment, pgAdmin, Prometheus and Grafana are bound to localhost; reach them through an SSH tunnel, e.g. `ssh -L 3001:localhost:3001 <host>`.

## Run the frontend demo

The demo runs standalone and uses synthetic fixture profiles. No Auth0 tenant, LLM API, database, or backend is needed.

```bash
docker compose -f docker-compose.demo.yml up --build
```

Open <http://localhost:3000>. Profile edits, matches, contacts, and messages are stored in this browser's local storage. Do not enter real personal information in the demo.

Stop it with `Ctrl+C`. This demo Compose file does not use or remove data volumes from the full-stack Compose setup.

### Hosted demo on GitHub Pages

The `deploy_demo.yml` workflow builds the demo client and publishes it to GitHub Pages at `https://<owner>.github.io/<repo>/` on every `main` push that touches `client/`. Setup:

1. Enable **Settings → Pages → Build and deployment → GitHub Actions** in the repository.
2. Set repository variables (**Settings → Secrets and variables → Actions → Variables**): `AUTH0_DOMAIN`, `AUTH0_CLIENT_ID`, `AUTH0_AUDIENCE`. Optionally set `DEMO_AUTH_MODE=demo` to remove the sign-in gate entirely.
3. In the Auth0 application settings, add `https://<owner>.github.io/<repo>/callback` to **Allowed Callback URLs**, and `https://<owner>.github.io/<repo>/` to **Allowed Logout URLs** and **Allowed Web Origins**.

## Run the full local stack

Requirements: Docker and Docker Compose. No external accounts are needed — local runs use `AUTH_MODE=dev`, where the gateway accepts `dev-<subject>` bearer tokens and the frontend signs in as a synthetic local user. A hosted LLM is not required; deterministic matching is the default.

```bash
cp .env.example .env   # local-only passwords are pre-filled with placeholders
docker compose up --build
```

The client is available at <http://localhost:3000>; the gateway is at <http://localhost:80>. Database, admin, metrics, and dashboard ports are bound to loopback for local development. PostgreSQL initialization scripts run only when its data volume is first created.

For real authentication, set `AUTH_MODE=auth0` and fill in the `AUTH0_*` values in `.env` for a tenant you control. Keep `.env` private and never commit it.

To run the GenAI matching tests from the repository root:

```bash
python -m pip install -r genai/requirements.txt
PYTHONPATH=. python -m pytest genai/tests
```

To run Java service tests:

```bash
cd server
./gradlew test
```

To lint/build the client:

```bash
cd client
npm ci
npm run lint
npm run build
```

## DevOps and infrastructure

The repository contains GitHub Actions, Docker Compose, Helm, Terraform, and Ansible configurations. The pipeline builds and tests every service, validates Helm and IaC, deploys the full Helm release to Kubernetes, and verifies health and rolling upgrades. Terraform and Ansible provision and configure the stack on AWS.

## Further documentation

- [GenAI matching service](genai/README.md)
- [Java services](server/README.md)
- [NGINX gateway](nginx/README.md)
- [Terraform](terraform/README.md)
