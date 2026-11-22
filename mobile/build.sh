#!/usr/bin/env bash
set -euo pipefail
root=$(pwd)
stage=$(mktemp -d)
cd "$stage"
npm init -y >/dev/null
npm install --save-exact @capacitor/core@7.4.3 @capacitor/android@7.4.3 @capacitor/cli@7.4.3
mkdir www
cp "$root"/{index.html,manifest.webmanifest,icon.svg} www/
npx cap init VanguardNav tr.vanguardnav.field --web-dir www
npx cap add android
target=android/app/src/main
cp "$root"/mobile/*.java "$target/java/tr/vanguardnav/field/"
python3 "$root/mobile/manifest.py" "$target/AndroidManifest.xml"
cd android
./gradlew assembleDebug
mkdir -p "$root/dist"
cp app/build/outputs/apk/debug/app-debug.apk "$root/dist/VanguardNav.apk"
