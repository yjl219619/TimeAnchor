#!/usr/bin/env bash
# 一条命令出 APK（Codespaces / 本机通用）
set -e
export ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk}
export ANDROID_SDK_ROOT=$ANDROID_HOME
echo "sdk.dir=$ANDROID_HOME" > local.properties

GRADLE_VER=8.11.1

find_gradle() {
  command -v gradle 2>/dev/null && return 0
  for p in /opt/gradle-${GRADLE_VER}/bin/gradle "$HOME"/.gradle-dist/gradle-${GRADLE_VER}/bin/gradle; do
    [ -x "$p" ] && { echo "$p"; return 0; }
  done
  return 1
}

G=$(find_gradle || true)
if [ -z "$G" ]; then
  echo "== 本机没有 gradle，下载 $GRADLE_VER =="
  mkdir -p "$HOME/.gradle-dist"
  curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VER}-bin.zip" -o /tmp/gradle.zip
  unzip -q -o /tmp/gradle.zip -d "$HOME/.gradle-dist"
  G="$HOME/.gradle-dist/gradle-${GRADLE_VER}/bin/gradle"
fi
echo "使用：$G"

if [ ! -f gradlew ]; then
  echo "== 生成 gradle wrapper =="
  "$G" wrapper --gradle-version "$GRADLE_VER"
fi

./gradlew assembleDebug --no-daemon --stacktrace

echo
echo "== 产物 =="
ls -lh app/build/outputs/apk/debug/ || true
echo "左侧文件树右键 app-debug.apk → Download"
