# Notes
Purpose: Android notes app with search and pinning (Kotlin, Compose).

## Commands
- Test: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lint`
- Build: `./gradlew assembleDebug`

## Architecture
app/src/main/java/<package>/ — keep game/business logic in plain Kotlin classes with no Android imports; UI in Compose files

## Rules
- Read only the files you need; do not scan the whole repo.
- Every behavior change needs a test. Run tests and lint before finishing.
- No new dependencies, permissions, or signing/secrets changes without asking.
- Never commit secrets, keystores, .env files.
- Keep PRs under ~300 changed lines; one issue per PR.
- Be concise: diffs plus a 3-line summary.
- If tests still fail after 3 attempts, stop and report the blocker.

## Definition of done
Lint clean, tests pass, CI green, short summary, PR opened as draft.
