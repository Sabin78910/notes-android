# Notes
Fast colourful notes and checklists with optional app lock and a writing streak.

## Stack
Kotlin · Jetpack Compose · Material 3 Expressive · JUnit. Offline only: the app has no INTERNET permission.

## Commands
- Test: `./gradlew test` · Lint: `./gradlew lint`
- Build: `./gradlew assembleDebug` (CI builds the signed release AAB)

## Architecture
- Pure Kotlin logic (checklist progress, streaks, search) separate from Composables.
- LazyVerticalStaggeredGrid for the note grid; editor state is hoisted.

## Data rules
- All data stays on device. Adding a network call means adding a permission: ask first, it changes the
  Play Data safety answers.
- Backup and restore go through the system file picker, so the user chooses the destination.

## Testing rules
- Pure logic (maths, streaks, formatting) is unit tested. Every behaviour change needs a test.
- Run `./gradlew test` before finishing.

## Design rules
- Material 3 Expressive: amber seed with a warm dark surface, Keep style colourful staggered grid, illustrated empty states
  drawn with Compose Canvas (no image files).
- Respect reduced motion (ANIMATOR_DURATION_SCALE=0), TalkBack labels and 48dp touch targets.
- Money and numbers use tabular figures. Never signal state with colour alone.

## Security and git
- Never commit secrets, keystores, .env files or email addresses.
- Keep the Content-Security-Policy strict; add a host to connect-src only when a feature needs it.
- `main` is protected: branch, open a PR, merge when CI is green. One issue per PR, under ~300 lines.

## Done when
Lint clean, tests pass, CI green, PR opened with a three line summary.
