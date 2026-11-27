#!/usr/bin/env bash
# 一次性准备 Android SDK + build-tools + 许可，供 Codespaces 直接编译
set -e
SDK=/opt/android-sdk
if [ -d "$SDK/platforms/android-36" ]; then echo "SDK 已就绪，跳过"; exit 0; fi

sudo mkdir -p "$SDK"
sudo chown "$(whoami)":"$(whoami)" "$SDK"
mkdir -p /tmp/cmline && cd /tmp/cmline
find /tmp/cmline -mindepth 1 -delete 2>/dev/null || true

URLS="
https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip
"
ok=0
for u in $URLS; do
  echo "尝试：$u"
  if curl -fsSL "$u" -o cmdline.zip; then ok=1; break; fi
done
[ "$ok" = "1" ] || { echo "！cmdline-tools 全部下载失败，把这一行发给我"; exit 1; }

unzip -q cmdline.zip -d /tmp/cmline/unz
mkdir -p "$SDK/cmdline-tools"
mv /tmp/cmline/unz/cmdline-tools "$SDK/cmdline-tools/latest"

export PATH="$SDK/cmdline-tools/latest/bin:$PATH"
yes | sdkmanager --licenses > /dev/null || true
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

echo "== SDK 安装完成 =="
ls "$SDK/platforms"
