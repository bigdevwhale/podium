#!/usr/bin/env bash
# Screenshots of the demo build for the READMEs: tools/capture.sh <out-dir> en|ru
# Build it with `./gradlew assembleDebug -Pdemo -Pshots` (app.podium.shots, never someone's demo).
# Reseeds it in that language, then walks the main screens with guarded taps, a heat included.
set -e
cd "$(dirname "$0")/.."
source tools/phone.sh
OUT="$1"; LANG_="${2:-en}"
PKG=app.podium.shots
mkdir -p "$OUT"
SHOTS="$OUT"
case "$PKG" in *.demo|*.shots) ;; *) echo "refusing to clear $PKG"; exit 1 ;; esac
"$ADB" shell pm clear $PKG >/dev/null
"$ADB" shell pm grant $PKG android.permission.POST_NOTIFICATIONS
"$ADB" shell cmd locale set-app-locales $PKG --locales "$LANG_"
"$ADB" shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 16
if [ "$LANG_" = ru ]; then
  T_MORNING="Утро: выйти из дома"; T_KITCHEN="Кухня после ужина"; T_START="Старт"; T_MARKS="На старт"
  T_DONE="Готово"; T_DEL="Удалить"; T_REC="РЕКОРД"; T_RESULTS="Протокол"; T_MEDALS="Медали"; T_COACH="Тренер"; T_STADIUM="Стадион"
else
  T_MORNING="Morning: out of the door"; T_KITCHEN="The kitchen after dinner"; T_START="Start"; T_MARKS="On your marks"
  T_DONE="Done"; T_DEL="Delete"; T_REC="RECORD"; T_RESULTS="Results"; T_MEDALS="Medals"; T_COACH="Coach"; T_STADIUM="Stadium"
fi
up() { guard $PKG && "$ADB" shell input swipe 540 1800 540 700 400; sleep 1; }
back() { guard $PKG && "$ADB" shell input keyevent 4; sleep 2; }
shot 1-stadium
tap_text $PKG "$T_MORNING"; sleep 3; shot 2-event
up; up; shot 3-event-more
back
tap_text $PKG "$T_KITCHEN"; sleep 2
tap_text $PKG "$T_START"; sleep 2; shot 4-start
tap_text $PKG "$T_MARKS"; sleep 9
# The running clock never lets uiautomator settle: the big button is tapped where it always is.
tap_at $PKG 540 1938; sleep 6; shot 5-heat
tap_at $PKG 540 1938; sleep 3
tap_at $PKG 540 1938; sleep 4
# A seconds-long heat would be a silly record to show: throw it away and show a real one.
tap_text $PKG "$T_DEL"; sleep 1; tap_text $PKG "$T_DEL"; sleep 2
tap_text $PKG "$T_RESULTS"; sleep 3; shot 7-results
tap_text $PKG "$T_REC"; sleep 3; shot 6-result
back
tap_text $PKG "$T_MEDALS"; sleep 3; shot 8-medals
tap_text $PKG "$T_COACH"; sleep 3; shot 9-coach
up; shot 10-coach-more
tap_text $PKG "$T_STADIUM"; sleep 1
echo done
