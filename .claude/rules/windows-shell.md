# Windows shell rules for X-NEWS

This machine is Windows 11. Claude has Git Bash (the Bash tool) and Windows PowerShell 5.1.

## Git Bash path conversion
Git Bash rewrites arguments that look like Unix paths into Windows paths. This broke Azure scopes (`/subscriptions/...`) and `gh --input` files.
- Prefix `az` commands that pass resource IDs or scopes with `MSYS_NO_PATHCONV=1`.
- With `MSYS_NO_PATHCONV=1`, pass files to Windows programs (`gh`, `az`) as Windows paths: `$(cygpath -w "$file")`.

## Use PowerShell for
- Windows user environment variables: `[Environment]::GetEnvironmentVariable('NAME','User')`. The Bash tool's own environment is a snapshot from session start.
- `file:///` URLs and Windows programs such as Edge (`[Uri]$path).AbsoluteUri`).
- Stopping processes: `Stop-Process -Id <pid> -Confirm:$false`.
- In PowerShell, clear variables with `$env:NAME = $null` (the harness treats `Remove-Item Env:...` next to certain strings as a path deletion).

## Processes
- `./mvnw spring-boot:run` starts a separate Java process. Stopping the task stops Maven, not Java. After stopping, check the port and stop the Java PID (it matches the `INFO <pid> ---` in the app log): `Get-NetTCPConnection -LocalPort 8080 -State Listen`.

## Git
- `master` is protected: work on a branch and open a pull request. Run `git branch --show-current` before committing.
- Warnings like "LF will be replaced by CRLF" are harmless.

## Output size
- Use `--query`/`-o tsv` with `az`, `--jq` with `gh`, and `tail`, `grep -c` or counts for logs. Never print full Maven or container logs.
- Long waits: run in the background and wait for the notification instead of polling in the foreground.
