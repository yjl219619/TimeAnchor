#!/usr/bin/env bash
# 一条命令出 APK（Codespaces / 本机通用）
set -e
export ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk}
export ANDROID_SDK_ROOT=$ANDROID_HOME
echo "sdk.dir=$ANDROID_HOME" > local.properties

if [ ! -f gradlew ]; then
  echo "== 生成 gradle wrapper（8.11.1）=="
  gradle wrapper --gradle-version 8.11.1
fi
./gradlew assembleDebug --no-daemon --stacktrace

echo
echo "== 产物 =="
ls -lh app/build/outputs/apk/debug/
echo "左侧文件树右键 app-debug.apk → Download"
