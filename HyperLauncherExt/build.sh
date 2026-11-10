#!/usr/bin/env bash
# Builds HyperLauncherExt.apk with the plain Android build tools (no Gradle):
#   aapt2, javac (JDK 11+), d8, zipalign, keytool + jarsigner (or apksigner).
#
#   ANDROID_JAR  path to android.jar of API 35   (required)
#   OUT          output APK                      (default: build/HyperLauncherExt.apk)
#   KEYSTORE     signing keystore                (default: a throwaway key in build/debug.keystore)
#   KS_PASS      keystore + key password         (default: android)
#   KS_ALIAS     key alias                       (default: hyperlauncherext)
#
# To update an installed copy (e.g. the one from the Magisk module) the APK must be signed with the same key.
set -e; cd "$(dirname "$0")"
SDK=${ANDROID_JAR:?set ANDROID_JAR to an API 35 android.jar}
OUT=${OUT:-build/HyperLauncherExt.apk}
KS_PASS=${KS_PASS:-android}; KS_ALIAS=${KS_ALIAS:-hyperlauncherext}
rm -rf build; mkdir -p build/classes build/dex
KEYSTORE=${KEYSTORE:-build/debug.keystore}
aapt2 compile --dir res -o build/res.zip
aapt2 link -o build/base.apk -I "$SDK" --manifest AndroidManifest.xml -R build/res.zip --auto-add-overlay \
    --min-sdk-version 29 --target-sdk-version 29 --version-code 28 --version-name 3.4
javac -nowarn --release 11 -cp "$SDK" -d build/classes $(find src -name '*.java') > build/javac.log 2>&1 \
    || { cat build/javac.log; echo 'javac failed'; exit 1; }
d8 --lib "$SDK" --min-api 29 --output build/dex $(find build/classes -name '*.class')
(cd build/dex && zip -q ../base.apk classes.dex)
[ -f "$KEYSTORE" ] || keytool -genkeypair -keystore "$KEYSTORE" -storepass "$KS_PASS" -keypass "$KS_PASS" -alias "$KS_ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=HyperLauncherExt" >/dev/null 2>&1
jarsigner -keystore "$KEYSTORE" -storepass "$KS_PASS" -keypass "$KS_PASS" -sigalg SHA256withRSA -digestalg SHA-256 \
    build/base.apk "$KS_ALIAS" >/dev/null
zipalign -f 4 build/base.apk build/aligned.apk
mkdir -p "$(dirname "$OUT")"; cp build/aligned.apk "$OUT"; echo "built $OUT"
