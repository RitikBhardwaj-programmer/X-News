# Verify X-NEWS changes end to end, not only with unit tests

Learned in V4 (2026-10-02). Unit tests passed while three real bugs were present; each was caught only by running the real pipeline on a local scratch database:
- A model feature (`temporal_score`) that only ever took two values in training extrapolated to near-certain matches on older events.
- Two `@Component` schedulers had two constructors and no `@Autowired`, so **the application would not start** ("No default constructor found"). `contextLoads` is excluded from local test runs, so nothing else catches this.
- A rule-based extractor read "117-ball" as 117 runs.

## What to run before opening a PR
- Unit tests as usual (`xnews-local-run` skill, section 1).
- For anything that touches processing, schedulers, new beans, migrations or AI-service calls: a **scratch-database run** (`xnews-local-run` skill, section 5). It boots the full Spring context (catching wiring errors), applies all migrations to a fresh database, and processes real local articles through the changed code.
- For UI changes: open the page in the browser pane, at desktop width and at 375 px (`resize_window` preset `mobile`), and check there's no horizontal scroll.

## Beans
- A Spring bean with more than one constructor needs `@Autowired` on the one Spring should use (typically the public one; a package-private one takes a `Clock` for tests).

## Models and rules
- Before shipping a model, check every feature's range in training against what production will send (`StandardScaler.mean_` / `scale_`). A feature with almost no spread in training can't be learned; leave it out.
- Before shipping rule-based extraction, run it over the labelled articles and read a sample of its output, not just the tests.
- Pass/fail criteria for a model or matcher change are written down **before** the run, and the result is reported against them, including when it fails.
