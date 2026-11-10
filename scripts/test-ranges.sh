#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "$PROJECT_DIR/.env.local" ]]; then source "$PROJECT_DIR/.env.local"; fi
TEST_DIR="$(mktemp -d)"
trap 'rm -rf "$TEST_DIR"' EXIT
"$JAVA_HOME/bin/javac" -d "$TEST_DIR" "$PROJECT_DIR/app/src/main/java/com/example/ringcalendar/CalendarEvent.java" "$PROJECT_DIR/app/src/main/java/com/example/ringcalendar/RingTimeMapper.java" "$PROJECT_DIR/app/src/main/java/com/example/ringcalendar/RestDayCalendar.java" "$PROJECT_DIR/tests/CalendarEventTest.java"
"$JAVA_HOME/bin/java" -cp "$TEST_DIR" com.example.ringcalendar.CalendarEventTest
