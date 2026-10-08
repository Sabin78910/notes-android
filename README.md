# Notes

Simple notes app with search and pinning, saved on device.

![CI](https://github.com/Sabin78910/notes-android/actions/workflows/ci.yml/badge.svg)

Built with Kotlin and Jetpack Compose (Material 3).

## Open in Android Studio
File → Open → select this folder. Let Gradle sync, then press Run.

## Command line
```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # APK -> app/build/outputs/apk/debug/
```

## Automation (runs on GitHub, no laptop needed)
| Workflow | Trigger | What it does |
|---|---|---|
| Android CI | push / PR to main | lint, unit tests, debug APK artifact |
| Release APK | tag `v*` | creates a GitHub Release with the APK |
| CodeQL | push / PR / weekly | security analysis |
| Dependabot | weekly | dependency update PRs |

Release a version: `git tag v1.0.0 && git push --tags`
