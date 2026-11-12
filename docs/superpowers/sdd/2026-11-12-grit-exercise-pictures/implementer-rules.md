# Implementer rules (read fully)

Binding constraints and environment: /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/constraints.md
Spec (the authority): docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md

1. Implement exactly what your brief specifies, test-first: failing tests, RED for the expected reason, implement, GREEN.
2. Focused tests while iterating; the full suites once before committing.
3. Local WIP commits are fine; finish with ONE commit with the title from the brief (`git reset --soft <BASE>` then commit). Never push.
4. Self-review the diff; leave `git status` clean.
5. Never dispatch subagents. Unclear or wrong brief → NEEDS_CONTEXT or BLOCKED with specifics.
6. Sources: WebSearch with allowed_domains only (constraints.md). Never fetch those sites directly or bypass bot checks. Record each source used in a code comment (URL + what it supports).
7. If resumed with review findings: fix, re-run covering tests, append a fix report to the same report file.

Report: full report to the given report path (implemented, tests with counts, RED/GREEN evidence, files changed, sources used, self-review, concerns, rulings). Reply with ONLY (under 12 lines): Status, commit, one-line test summary, concerns, report path.
