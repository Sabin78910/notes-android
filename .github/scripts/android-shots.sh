#!/usr/bin/env bash
# Runs inside the cloud emulator: installs the debug APK, screenshots light and dark mode,
# and stress-tests with 300 random taps. Problems are written to problems.md (used by visual-qa.yml).
set -u
mkdir -p shots
adb install -r "$(ls app/build/outputs/apk/debug/*.apk | head -1)"
note() { printf -- "- [ ] %s\n" "$1" >> problems.md; }
launch() {
  adb shell am force-stop "$APP_ID"; adb logcat -c
  adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  sleep 8
  adb exec-out screencap -p > "shots/$1.png"
  if adb logcat -d | grep -qE "FATAL EXCEPTION|ANR in $APP_ID"; then
    note "$1: the app crashed or froze on launch: $(adb logcat -d | grep -m1 -A3 -E 'FATAL EXCEPTION|ANR in' | tr '\n' ' ' | cut -c1-400)"
  fi
}
adb shell cmd uimode night no; launch light
adb shell cmd uimode night yes; launch dark
adb shell cmd uimode night no
adb logcat -c
adb shell monkey -p "$APP_ID" --throttle 120 --pct-syskeys 0 -v 300 > monkey.txt 2>&1 || true
if grep -qE "// CRASH|// NOT RESPONDING" monkey.txt; then
  note "300 random taps and swipes crashed the app: $(grep -m1 -A3 -E '// CRASH|// NOT RESPONDING' monkey.txt | tr '\n' ' ' | cut -c1-400)"
fi
adb exec-out screencap -p > shots/after-random-input.png
true
