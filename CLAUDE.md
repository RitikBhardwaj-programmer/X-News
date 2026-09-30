## Safety (learned the hard way in V3)

- Before running tests, the app or any database command, confirm the target: `DATABASE_URL` must be `localhost:15432`. Production is only reachable through `XNEWS_PROD_DATABASE_*` (see the `xnews-prod-db` skill). A hook (`.claude/hooks/guard_local_db.py`) blocks Maven test/run commands otherwise.
- Identify production from the deployed Azure configuration, never from a developer machine's environment.
- Never print secrets or full connection strings; print hosts and fingerprints only.
- Changes reach `master` through pull requests with green CI; deploys wait for the user's approval.
- Test from where the code actually runs (a feed that works from home can be blocked from Azure).
- Windows shell pitfalls: `.claude/rules/windows-shell.md`.
- Project skills: `xnews-local-run`, `xnews-prod-db`, `xnews-deploy`, `xnews-azure-logs`, `xnews-release-doc`.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
