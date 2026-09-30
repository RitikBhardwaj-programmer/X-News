---
name: xnews-azure-logs
description: Search X-NEWS production logs (backend or AI service on Azure Container Apps) with Log Analytics, e.g. errors, failed collections, "Article not found", Gemini failures, or what happened to a specific article. Use for any production debugging question.
---

# X-NEWS production logs

`az containerapp logs show` only returns the last few hundred lines. For anything older or filtered, query Log Analytics through `az rest` (no extra CLI extension needed; do not install `log-analytics`).

## Template (Git Bash)
```bash
W=$(az containerapp env show -n xnews-env-uae -g xnews-rg --query properties.appLogsConfiguration.logAnalyticsConfiguration.customerId -o tsv)
cat > "$TEMP/laq.json" <<'EOF'
{"query": "ContainerAppConsoleLogs_CL | where TimeGenerated > ago(6h) and ContainerAppName_s == 'xnews-backend' and Log_s has 'ERROR' | project TimeGenerated, Log_s | order by TimeGenerated desc | take 20"}
EOF
MSYS_NO_PATHCONV=1 az rest --method post --url "https://api.loganalytics.io/v1/workspaces/$W/query" \
  --resource "https://api.loganalytics.io" --body "@$(cygpath -w "$TEMP/laq.json")" \
  --query "tables[0].rows" -o tsv | cut -c1-300
```

## Useful KQL
- Per-article decisions: `Log_s has 'Processed article='`
- Collection failures: `Log_s has 'Failed to collect'` (Indian Express returns 403 from Azure; disabled)
- Consumer misses: `Log_s has 'Article not found' | extend id = toint(extract('Article not found: ([0-9]+)', 1, Log_s)) | summarize count() by id`
- Gemini: `Log_s has 'Gemini call failed' or Log_s has 'AI service unavailable'`
- AI service: `ContainerAppName_s == 'xnews-ai'`

## Notes
- KQL reserves words such as `first`; name columns like `firstSeen`.
- Log lines include multi-line stack traces; filter out lines starting with `at ` when summarising.
- Keep output short: `take`, `summarize`, `cut -c1-300`.
