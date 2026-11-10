#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"
if [[ -f .env.local ]]; then source .env.local; fi
if [[ -z "${JAVA_HOME:-}" ]]; then export JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"; fi
if [[ ! -x "${JAVA_HOME:-}/bin/javac" ]]; then echo '需要 JDK 17，请在 .env.local 设置 JAVA_HOME' >&2; exit 1; fi
export ANDROID_HOME="${ANDROID_HOME:-$PROJECT_DIR/../.tooling/android-sdk}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PROJECT_DIR/../.gradle-home}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
case "${1:-run}" in
  build) ./gradlew --console=plain assembleDebug ;;
  devices) adb devices -l ;;
  run)
    if [[ -n "${ANDROID_SERIAL:-}" ]]; then DEVICE_SERIAL="$ANDROID_SERIAL"; else
      DEVICE_COUNT="$(adb devices | awk 'NR>1 && $2=="device" {n++} END {print n+0}')"
      if [[ "$DEVICE_COUNT" != 1 ]]; then adb devices -l; echo '请连接一台已授权的安卓设备' >&2; exit 1; fi
      DEVICE_SERIAL="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
    fi
    ./gradlew --console=plain assembleDebug
    adb -s "$DEVICE_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
    adb -s "$DEVICE_SERIAL" shell am start -W -n com.example.ringcalendar/.MainActivity
    ;;
  *) echo '用法: android.sh {build|run|devices}' >&2; exit 2 ;;
esac
