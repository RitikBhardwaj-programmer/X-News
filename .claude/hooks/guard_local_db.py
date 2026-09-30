"""PreToolUse guard: block commands that start the Spring app or its tests
unless the database they would use is local.

Why: Flyway migrates whatever DATABASE_URL points at. On 2026-09-24 a local
`./mvnw test` migrated a cloud Neon database because the shell's DATABASE_URL
pointed there. Production must only be reached deliberately (XNEWS_PROD_*).

Covers Bash and PowerShell tool calls. Reads the hook JSON from stdin.
"""

import json
import os
import re
import sys

LOCAL_HOSTS = ("localhost", "127.0.0.1", "[::1]")

# Commands that start Spring (and therefore Flyway, schedulers, Kafka):
# Maven test/verify/install/spring-boot:run, `package` unless tests are skipped,
# and running the built jar.
STARTS_APP = re.compile(
    r"(mvnw(\.cmd)?|\bmvn)\b[^\n;&|]*\b(test|verify|install|spring-boot:run)\b"
    r"|(mvnw(\.cmd)?|\bmvn)\b(?![^\n;&|]*-DskipTests)[^\n;&|]*\bpackage\b"
    r"|java\b[^\n;&|]*-jar[^\n;&|]*X-News",
    re.IGNORECASE,
)

# DATABASE_URL set inline: bash `DATABASE_URL=... cmd`, PowerShell `$env:DATABASE_URL = '...'`,
# or a Spring argument `--spring.datasource.url=...`.
INLINE_URL = re.compile(
    r"(?:\$env:DATABASE_URL\s*=\s*|\bDATABASE_URL=|--spring\.datasource\.url=)[\"']?([^\"'\s;]+)",
    re.IGNORECASE,
)


def is_local(url):
    return url is not None and any(host in url for host in LOCAL_HOSTS)


def main():
    try:
        payload = json.load(sys.stdin)
    except ValueError:
        return 0
    command = (payload.get("tool_input") or {}).get("command") or ""
    if not STARTS_APP.search(command):
        return 0

    inline = INLINE_URL.findall(command)
    url = inline[-1] if inline else os.environ.get("DATABASE_URL")
    if is_local(url):
        return 0

    target = "not set" if not url else "a non-local host"
    reason = (
        "Blocked by guard_local_db: this command starts the Spring app/tests "
        f"(Flyway, schedulers, Kafka) and DATABASE_URL is {target}. "
        "Prefix it with DATABASE_URL=jdbc:postgresql://localhost:15432/xnews "
        "DATABASE_USERNAME=postgres DATABASE_PASSWORD=postgres, or ask the user "
        "before running against any other database."
    )
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }))
    return 0


if __name__ == "__main__":
    sys.exit(main())
