# Contributing

This is a portfolio project, but it's built to accept contributions the way a
real project would.

## Workflow
1. Check [Issues](../../issues) / [Milestones](../../milestones) for open, scoped work.
2. Branch from `main`: `feature/<short-description>` or `fix/<short-description>`.
3. Keep module boundaries intact — see `docs/ARCHITECTURE.md`. A change to `pharmacy`
   should not reach into another module's repository/tables.
4. Add or update tests for any behavior change.
5. Open a PR against `main`; CI (`.github/workflows/ci-cd.yml`) must pass.
6. Update the relevant doc under `docs/` if the change affects requirements,
   schema, or API shape — docs are expected to stay in sync with code, not
   trail behind it.

## Commit style
Conventional-ish: `feat(patient): add visit history endpoint`, `fix(auth): rotate refresh token on reuse`.

## Local setup
See the Quick Start sections in the root [README.md](README.md).
