---
name: xnews-local-run
description: Start the X-NEWS stack locally (Postgres, AI service, Spring Boot backend, React frontend) safely, or run the Java tests, without touching production Kafka, Neon or Azure. Use when asked to run, start or test X-NEWS locally, reproduce a bug end to end, or check the app in a browser.
---

# Run X-NEWS locally (safe defaults)

Local runs must never reach production. Production DB is only under `XNEWS_PROD_DATABASE_*`; Kafka credentials in the shell point at Confluent Cloud.

## 0. Preconditions
- Docker Desktop must be running (the user pauses/unpauses it; never do it yourself). Check: `docker ps --format '{{.Names}} {{.Status}}' | grep xnews-postgres`.
- Local DB: `jdbc:postgresql://localhost:15432/xnews`, user/password `postgres`/`postgres` (docker-compose).

## 1. Java unit tests (always with the local DB inline)
```bash
cd "/c/Users/DELL/IdeaProjects/X News" && DATABASE_URL=jdbc:postgresql://localhost:15432/xnews DATABASE_USERNAME=postgres DATABASE_PASSWORD=postgres ./mvnw -o -q test -Dtest='!XNewsApplicationTests' -Dsurefire.failIfNoSpecifiedTests=false > "$TEMP/mvntest.log" 2>&1; echo exit=$?; python -c "import re,glob; r=[re.search(r'Tests run: (\d+), Failures: (\d+), Errors: (\d+)', open(p).read()) for p in glob.glob('target/surefire-reports/*.txt') if 'XNewsApplication' not in p]; r=[m for m in r if m]; print('tests:', sum(int(m[1]) for m in r), 'failures+errors:', sum(int(m[2]) + int(m[3]) for m in r))"
```
Totals are counted in Python because skill arguments replace `$` followed by a digit in this file. `XNewsApplicationTests.contextLoads` is excluded: it starts the full app (DB, Kafka, keys). Drop `-o` if new dependencies must download.

## 2. AI service (background)
```bash
SP=<scratchpad>; [ -f "$SP/local-ai-key" ] || python -c "import secrets;print(secrets.token_hex(16))" > "$SP/local-ai-key"
cd /c/Users/DELL/PycharmProjects/xnews-semantic-experiment && AI_SERVICE_API_KEY=$(cat "$SP/local-ai-key") HF_HUB_OFFLINE=1 .venv/Scripts/python.exe -m uvicorn app:app --host 127.0.0.1 --port 8000
```
Health: `curl -s http://127.0.0.1:8000/health`. Smoke test: `AI_SERVICE_API_KEY=... .venv/Scripts/python.exe smoke_test.py http://127.0.0.1:8000`.

## 3. Backend (background)
Kafka points at an unused port so the laptop never joins the production consumer group; no Jev key needed.
```bash
cd "/c/Users/DELL/IdeaProjects/X News" && DATABASE_URL=jdbc:postgresql://localhost:15432/xnews DATABASE_USERNAME=postgres DATABASE_PASSWORD=postgres KAFKA_BOOTSTRAP_SERVER=localhost:59092 KAFKA_API_KEY=local-dummy KAFKA_API_SECRET=local-dummy JEV_API_KEY= AI_SERVICE_API_KEY=$(cat "$SP/local-ai-key") AI_EVENT_MATCHER_URL=http://127.0.0.1:8000 ./mvnw -q spring-boot:run
```
Wait for `Started XNewsApplication` in the task output. Expected noise: RSS "Send failed" (no Kafka). Side effect: the lifecycle job closes local events idle for 10+ days. `GEMINI_API_KEY` and `JWT_SECRET` come from the user's environment; Analyze makes a real (paid) Gemini call.

## 4. Frontend (background)
```bash
cd "/c/Users/DELL/IdeaProjects/X News/frontend" && VITE_API_URL=http://localhost:8080/api/v1 npx vite --port 5173 --strictPort
```
Port 5173 is in the backend CORS list. Open in the browser pane.

## 5. Stop everything
Stop the background tasks, then free port 8080 (Maven leaves the Java child running):
```powershell
$c = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue; if ($c) { Stop-Process -Id $c[0].OwningProcess -Confirm:$false }
```
Only stop a PID you started (it appears as `INFO <pid> ---` in the backend log).
