# CI/CD Pipeline Documentation

Three workflows cover build/test/publish, Kubernetes deployment, and the hosted
demo. Terraform and Ansible under `terraform/` and `ansible/` provision and
configure the AWS deployment target.

## `build_docker.yml` — Build Docker Images

Triggered on every push and on pull requests targeting `main`.

### Test & build jobs

| Job | What it does |
|---|---|
| `java-build-test` | Matrix over `userservice`, `locationservice`, `matchingservice`, `messagingservice`; runs `./gradlew :<svc>:build` (compiles, tests, packages). |
| `genai-python-test` | Installs `genai/requirements.txt` on Python 3.11 and runs `pytest genai/tests` (deterministic matcher — no LLM credentials needed). |
| `client-test` | `npm ci`, `npm run lint`, `npm run build` on Node 20. No Auth0 values are baked in — the client reads `runtime-config.js` generated at container startup. |
| `infra-check` | `helm lint` + `helm template`, `docker compose config` for all three Compose files, `terraform fmt -check`/`init`/`validate`, `ansible-playbook --syntax-check`. |

### `publish-images`

Depends on all test jobs. Builds every image in a matrix (client, the four Java
services, genai, nginx-gateway) for `linux/amd64` + `linux/arm64`.

- All Dockerfiles are self-contained multi-stage builds — no build artifacts are
  passed between CI jobs.
- Tags: `sha-<short>`, branch name, PR number, and `latest` on the default branch.
- Images are pushed to `ghcr.io/<owner>/<repo>/<service>` on non-PR events.
  Pull requests build without pushing, proving buildability without publishing.
- Build cache uses `type=gha` (GitHub Actions cache), no external registry cache.

## `deploy_helm.yml` — Deploy to Kubernetes

Runs after a successful `Build Docker Images` run on `main`, or manually via
`workflow_dispatch`. Deploys the full Helm release to a Kubernetes cluster
provisioned on the runner and verifies the rollout:

1. Provisions a Kubernetes cluster on the runner.
2. Creates the namespace plus `ghcr-creds`, `auth0-secrets`, `postgres-secret`,
   and `internal-service-auth` secrets.
3. `helm upgrade --install --wait` with the `sha-<short>` image tags produced by
   the triggering build, `client.demoMode=true`, and `ingress.enabled=false`.
4. Waits for Postgres readiness, then verifies:
   - `GET /healthz` on the NGINX gateway
   - `/` and `/runtime-config.js` on the client (verifies runtime config injection)
   - `/api/actuator/health` on each Spring service and `/health` on GenAI
5. Runs a `helm upgrade` rolling-update check and waits for rollout status.
6. On failure, collects pod status, events, describes, and logs.
7. Always uninstalls the release and deletes the cluster.

This exercises the same `helm upgrade --install` path used against any target
cluster (the chart is environment-agnostic — hostnames, secrets, and image
coordinates come from `values.yaml`).

## `deploy_demo.yml` — Deploy Demo to GitHub Pages

Runs on `main` pushes that touch `client/` and on manual dispatch. Builds the
React client in demo mode (`VITE_DEMO_MODE=true`) and publishes `client/dist`
to GitHub Pages at `https://<owner>.github.io/<repo>/`.

- Repository variables configure Auth0 (`AUTH0_DOMAIN`, `AUTH0_CLIENT_ID`,
  `AUTH0_AUDIENCE`) so the hosted demo uses the real sign-in flow; fixture data
  still lives in the browser. Setting `DEMO_AUTH_MODE=demo` removes the gate.
- `--base=/<repo>/` matches the Pages URL path; `404.html` enables client-side
  routing on refresh.

## AWS deployment

`terraform/` provisions the AWS environment (VPC, EC2, security groups, IAM),
and `ansible/` installs Docker and brings up the Compose stack on the host.
Both are validated in CI (`fmt`/`validate`/syntax check) and designed to run
against a real AWS account:

```bash
cd terraform && terraform apply
cd ../ansible && ansible-playbook -i inventory.ini playbook.yml
```
