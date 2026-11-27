#!/bin/sh
# 编译；成功就把 APK 推进仓库，失败就把错误行推进仓库
./gradlew assembleDebug --no-daemon > /tmp/build.log 2>&1
code=$?

if [ $code -eq 0 ] && [ -f app/build/outputs/apk/debug/app-debug.apk ]; then
  cp app/build/outputs/apk/debug/app-debug.apk ./app-debug.apk
  ls -lh ./app-debug.apk
fi

{
  grep -E "^e: file|What went wrong|^> Task .*FAILED" -A4 /tmp/build.log | head -60
  tail -3 /tmp/build.log
} > build-errors.txt

git add -f build-errors.txt
[ -f app-debug.apk ] && git add -f app-debug.apk
git -c user.name="WEP-56" -c user.email="1484413790@qq.com" commit -q -m "ci: 构建快照" || true
git push -q origin main || echo "(push 失败)"
echo "===== exit: $code ====="
cat build-errors.txt
