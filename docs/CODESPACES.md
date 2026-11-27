# 不用电脑：用 Codespaces 出 APK

## 一次性准备
1. 建仓库 `TimeAnchor`（Private），把工程推上去
2. 仓库页 → 绿色 **Code** → **Codespaces** → **Create codespace on main**
3. 等 2–4 分钟：`postCreate.sh` 自动装 Android SDK 36（装一次，之后这个 codespace 一直可用）

## 每次构建
Codespace 终端里：
```bash
bash make-apk.sh
```

## 下载
左侧文件树展开 `app/build/outputs/apk/debug/` → 右键 `app-debug.apk` → **Download**

## 出错
复制两段发我：`FAILURE: Build failed` 到 `BUILD FAILED` 整段；第一条 `e: file:///` 整行。

## 用完停掉
左下角 **Stop codespace**，否则一直耗免费额度。
