# Flyway migrations in X-NEWS

Learned in V4 (2026-10-02), when two PRs carried V8 and V9 and only V9 was merged first.

## Numbering and merge order
- Every PR that adds a migration takes the next free number on `master` **at the time it is written**, and says in its description which migration it adds.
- Flyway runs with `outOfOrder=false`: once V9 is applied anywhere, a later V8 makes startup fail ("Detected resolved migration not applied to database"). Production then doesn't start.
- When two open PRs carry migrations, state the merge order in both PR descriptions. Before any deploy, check the migrations that are actually on `master`: `git ls-tree --name-only origin/master src/main/resources/db/migration/`.
- If a lower number ends up behind a higher one that has merged, **renumber the unmerged migration above it** (gaps are fine: V7, V9, V10 is valid). Never renumber or edit a migration that any shared database has applied.
- A user saying "merged A and B" is not proof: check with `gh pr view <n> --json state,mergedAt` before building on it.

## Migrations themselves
- Additive only unless the user approves otherwise: new tables, nullable columns, indexes. Dropping or rewriting data needs explicit approval.
- Hibernate runs with `ddl-auto=validate`: every new mapped column needs its migration, but unmapped columns (kept for history) are fine.
- Generated columns and new indexes on `articles` rewrite or scan the table once; say so in the PR (seconds at today's size).
- Dry-run a new migration on a throwaway local database (apply V1..Vn in order with `psql -v ON_ERROR_STOP=1`, then drop it), or inside `BEGIN; ... ROLLBACK;`.

## Local build pitfall
Maven doesn't delete resources that were removed or renamed. After renaming a migration, delete `target/classes/db/migration/` (or run `./mvnw clean`) before a local run, or Flyway applies the stale copy too.
