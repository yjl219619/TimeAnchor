#!/bin/sh
# 编译并把错误写进 build-errors.txt 再推送，供远程读取
./gradlew assembleDebug --no-daemon > /tmp/build.log 2>&1
code=$?
{
  grep -E "^e: file|What went wrong|^> Task .*FAILED" -A4 /tmp/build.log | head -60
  tail -3 /tmp/build.log
} > build-errors.txt

git add -f build-errors.txt
git -c user.name="WEP-56" -c user.email="1484413790@qq.com" commit -q -m "ci: 构建错误快照" || true
git push -q origin main || echo "(push 失败，不影响错误文件)"

echo "===== build exit: $code ====="
cat build-errors.txt
