#!/bin/sh
# 只打印编译错误，不刷屏。用法：sh e.sh
./gradlew assembleDebug --no-daemon 2>&1 \
  | grep -E -A6 "(What went wrong|^e: file|^> Task .*FAILED|^\* Try)" \
  | head -50
