---
name: xnews-deploy
description: Deploy the X-NEWS backend or AI service to Azure Container Apps through the approval-gated GitHub Actions workflows, run a dry run, verify a deployment, roll back, or change Container App settings/secrets. Use for any X-NEWS deployment, release, rollback or production configuration change.
---

# Deploy X-NEWS

## Resources
- Resource group `xnews-rg`, environment `xnews-env-uae`, registry `xnewsbackendacr.azurecr.io`.
- Apps: `xnews-backend` (port 8080, always on, minReplicas 1) and `xnews-ai` (port 8000).
- Frontend: Azure Static Web Apps, deploys automatically on every merge to `master`.
- Workflows: `X-News/.github/workflows/deploy-backend.yml`, `xnews-semantic-experiment/.github/workflows/deploy.yml`. Both use the GitHub environment `production` (the user approves) and OIDC (identity `xnews-github-deploy`, no stored password).

## Normal deploy
1. Merge a PR to `master` that touches app code. The workflow starts and waits for approval.
2. Give the user the run URL; **the user clicks Approve**. Never approve on their behalf unless they say so.
3. Wait in the background: `until [ "$(gh run view <id> -R <repo> --json status --jq .status)" = completed ]; do sleep 20; done`.
4. Report each step: `gh run view <id> -R <repo> --json jobs --jq '.jobs[] | select(.name=="deploy") | .steps[] | "\(.name): \(.conclusion)"'`.

## Queued runs (learned 2026-10-02)
- Both workflows use a concurrency group with `cancel-in-progress: false`. A run **waiting for approval blocks every later run**, and GitHub keeps only the newest queued one, silently dropping the ones in between.
- When several merges queue up, the newest run usually contains all of them. Ask the user before cancelling anything; their usual choice has been to cancel the older waiting run (`gh run cancel <id> -R <repo>`) and approve only the newest, so there's one restart.
- Never let an older run deploy known-bad code: if a waiting run contains something since fixed, say so and propose cancelling it.
- After merges, list what is queued: `gh run list -R <repo> --workflow <file> --limit 4 --json databaseId,status,conclusion,headSha`.

## Order between the two apps
- When the backend starts calling a new AI-service endpoint, deploy the AI service first, or make sure the backend tolerates a 404 (it does for `/predict/v2`, `/entities` and `/claims`: the call is caught and logged).
- Every AI-service restart drops its in-memory v2 vocabulary. With `AI_EVENT_MATCHER_MODE` at `shadow` or `v2`, the backend refits it automatically within a minute of the next article. Expect a "Vocabulary refit" log line; it doesn't count as a v2 error.
- Migration numbers on `master` must be in merge order before deploying (`.claude/rules/flyway-migrations.md`).

## Dry run (build and push only)
`gh workflow run deploy-backend.yml --ref master -f dry_run=true` (AI: `deploy.yml -R RitikBhardwaj-programmer/xnews-semantic-experiment`). Still needs approval.

## Verify independently
```bash
az containerapp revision list -n xnews-backend -g xnews-rg --query "[?properties.active].[name, properties.template.containers[0].image, properties.healthState]" -o tsv
curl -s -o /dev/null -w "%{http_code}\n" https://xnews-backend.kindflower-bb4efb59.uaenorth.azurecontainerapps.io/api/v1/events   # expect 403 (Spring)
curl -s https://xnews-ai.kindflower-bb4efb59.uaenorth.azurecontainerapps.io/health                                     # expect healthy
```
- A new revision can take traffic weight 100 while its `healthState` is still `None`; the old revision keeps answering for 20–60 s while the model loads. Poll until the new behaviour is visible before concluding anything. For the AI service, its public endpoint list: `curl -s https://xnews-ai.kindflower-bb4efb59.uaenorth.azurecontainerapps.io/openapi.json` (paths include the new endpoint).
- Backend startup lines (Flyway version, `Event matcher mode`, `Started XNewsApplication`) are easiest to read from Log Analytics filtered by `RevisionName_s` (`xnews-azure-logs` skill); `az containerapp logs show` often returns nothing for a fresh revision.

## Rollback
Redeploy an earlier image tag: `gh workflow run deploy-backend.yml --ref <earlier-sha>` or, with approval, `az containerapp update -n xnews-backend -g xnews-rg --image xnewsbackendacr.azurecr.io/xnews-backend:sha-<7>`. List tags: `az acr repository show-tags -n xnewsbackendacr --repository xnews-backend -o tsv`.

## Settings and secrets (production change: confirm with the user first)
- Secrets: `az containerapp secret set -n <app> -g xnews-rg --secrets "name=$VALUE"` with the value read from a variable, never typed or printed. Compare secrets by fingerprint (`sha256sum | cut -c1-8`), never by value.
- Env: `--set-env-vars "NAME=secretref:secret-name"` or plain values.
- **Coupled changes (new image + new secrets/env) go in one `az containerapp update`**, so old code never runs with new settings. Secret changes alone do not restart the app.
- Prefix commands that pass resource IDs with `MSYS_NO_PATHCONV=1` (Git Bash).
- Logs: see the `xnews-azure-logs` skill.
