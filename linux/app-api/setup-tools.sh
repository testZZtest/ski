#!/bin/sh
# app-api skill: decompile tools for reading an app's endpoints (see SKILL.md).
# Installs jadx (with its JRE if needed) and dex2jar's dex tools into Ski's Linux.
set -eu
D=$HOME/.ski/tools/app-api
[ -x "$D/jadx/bin/jadx" ] && echo "app-api: tools already installed" && exit 0
mkdir -p "$D" && cd "$D"

have() { command -v "$1" >/dev/null 2>&1; }

# jadx: pure Java, needs a JRE (the android-app toolchain's OpenJDK 17 serves if present)
if ! have java; then
  echo "app-api: installing a Java runtime (apk add)"
  apk add --no-cache openjdk17-jre-headless >/dev/null 2>&1 || apk add --no-cache openjdk17-jre-headless
fi

echo "app-api: downloading jadx"
JADX_VER=1.5.1
curl -sSL -o jadx.zip "https://github.com/skylot/jadx/releases/download/v$JADX_VER/jadx-$JADX_VER.zip"
mkdir -p jadx && cd jadx && unzip -oq ../jadx.zip && cd ..
rm -f jadx.zip
chmod +x jadx/bin/jadx

echo "app-api: done — jadx at $D/jadx/bin/jadx"
