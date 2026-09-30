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

## Dry run (build and push only)
`gh workflow run deploy-backend.yml --ref master -f dry_run=true` (AI: `deploy.yml -R RitikBhardwaj-programmer/xnews-semantic-experiment`). Still needs approval.

## Verify independently
```bash
az containerapp revision list -n xnews-backend -g xnews-rg --query "[?properties.active].[name, properties.template.containers[0].image, properties.healthState]" -o tsv
curl -s -o /dev/null -w "%{http_code}\n" https://xnews-backend.kindflower-bb4efb59.uaenorth.azurecontainerapps.io/api/v1/events   # expect 403 (Spring)
curl -s https://xnews-ai.kindflower-bb4efb59.uaenorth.azurecontainerapps.io/health                                     # expect healthy
```

## Rollback
Redeploy an earlier image tag: `gh workflow run deploy-backend.yml --ref <earlier-sha>` or, with approval, `az containerapp update -n xnews-backend -g xnews-rg --image xnewsbackendacr.azurecr.io/xnews-backend:sha-<7>`. List tags: `az acr repository show-tags -n xnewsbackendacr --repository xnews-backend -o tsv`.

## Settings and secrets (production change: confirm with the user first)
- Secrets: `az containerapp secret set -n <app> -g xnews-rg --secrets "name=$VALUE"` with the value read from a variable, never typed or printed. Compare secrets by fingerprint (`sha256sum | cut -c1-8`), never by value.
- Env: `--set-env-vars "NAME=secretref:secret-name"` or plain values.
- **Coupled changes (new image + new secrets/env) go in one `az containerapp update`**, so old code never runs with new settings. Secret changes alone do not restart the app.
- Prefix commands that pass resource IDs with `MSYS_NO_PATHCONV=1` (Git Bash).
- Logs: see the `xnews-azure-logs` skill.
