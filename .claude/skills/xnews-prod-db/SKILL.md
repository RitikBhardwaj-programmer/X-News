---
name: xnews-prod-db
description: Read from or (with explicit user approval) write to the X-NEWS production Neon PostgreSQL database safely, without printing credentials. Use for production counts, checks, data fixes, seeding sources, granting roles, backups, or anything that needs psql against the live database.
---

# X-NEWS production database

Credentials live only in Windows user variables `XNEWS_PROD_DATABASE_URL`, `XNEWS_PROD_DATABASE_USERNAME`, `XNEWS_PROD_DATABASE_PASSWORD` (plain `DATABASE_*` is the local Docker DB). The same values are Azure secrets on `xnews-backend`; the Azure secret is the source of truth for "which DB is production".

## Rules
- Reads: fine. Writes (INSERT/UPDATE/DELETE/DDL): show the exact SQL and get the user's OK first; use a transaction and `-v ON_ERROR_STOP=1`.
- Never print credentials or full connection strings. Print hosts only.
- Never select email/password columns in full; mask emails (`left(email,2) || '***@' || split_part(email,'@',2)`).
- Schema changes go through Flyway migrations in a PR, not ad-hoc SQL.

## Template (PowerShell)
Write longer SQL to a file in the scratchpad and run it with `-f`.
```powershell
$u = [Environment]::GetEnvironmentVariable('XNEWS_PROD_DATABASE_URL','User')
if (-not $u) { throw 'XNEWS_PROD_DATABASE_URL not set' }
$base = $u.Substring(5).Split('?')[0]            # drop "jdbc:" and query params
"host: " + $base.Split('/')[2]                  # no $-digit here: skill arguments would replace it
$env:PGUSER = [Environment]::GetEnvironmentVariable('XNEWS_PROD_DATABASE_USERNAME','User')
$env:PGPASSWORD = [Environment]::GetEnvironmentVariable('XNEWS_PROD_DATABASE_PASSWORD','User')
$env:PGSSLMODE = 'require'
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" $base -tA -F ' | ' -v ON_ERROR_STOP=1 -c "select count(*) from articles"
$env:PGPASSWORD = $null; $env:PGUSER = $null; $env:PGSSLMODE = $null
```

## Useful queries
- Health: `select count(*), count(*) filter (where processed) from articles;`
- Stuck articles: `select id from articles where not processed and created_at < now() - interval '30 minutes';`
- Event sizes: `select member_count, count(*) from news_events group by 1 order by 1;`
- Sources: `select id, name, enabled from news_sources order by id;`

## Backup (read-only)
```powershell
& "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" -Fc --no-owner --no-privileges -d $base -f "C:\Users\DELL\xnews-backups\neon-<name>-<date>.dump"
```
Verify with `pg_restore --list <file>` (count `TABLE DATA` lines). Keep backups outside the repositories.
