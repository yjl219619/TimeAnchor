# 手机端构建（唯一路径）

不需要电脑。用手机把它推到 GitHub，让 Actions 编译，再把 APK 下回来。

## 1 建空仓库
浏览器打开 github.com → 右上 `+` → New repository
- 名字：`TimeAnchor`
- 选 **Private**
- 不要勾 README / .gitignore / license（必须空仓库）

## 2 生成 token
github.com → 头像 → Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token
- Repository access：Only select repositories → `TimeAnchor`
- Permissions → Repository permissions → **Contents: Read and write**
- 生成后复制（只显示一次）

## 3 推送（我给的两行）
把 token 贴给我，我在这台手机上直接：

```sh
cd /storage/emulated/0/TimeAnchor
git init -b main
git add -A && git commit -m "phase1: 时间锚点原型 + 悬浮接管 + CI"
git remote add origin https://github.com/<你的用户名>/TimeAnchor.git
git push https://<你的用户名>:<token>@github.com/<你的用户名>/TimeAnchor.git main
```

token 只用在这条命令里，不写进 .git/config。

## 4 触发构建
推送完成即自动触发。若没触发：
仓库页 → **Actions** → 左侧 `build` → 右侧 **Run workflow** → Run

## 5 下载 APK
Actions → 最新那次运行 → 页面底部 **Artifacts** → `timeanchor-debug-apk` → 下载 zip → 解压得到 `app-debug.apk` → 点击安装
（下载 artifact 需要处于登录状态，手机浏览器先登录 GitHub。）

## 6 装好后
按 README「装好后必做」6 条开权限，再跑测试清单。

---

## 编译失败了怎么发给我

**只复制这两段**，其它不用：

1. 运行日志里以 `FAILURE: Build failed with an exception.` 开头，
   到 `BUILD FAILED` 结尾的**整段**（含 `* What went wrong:` 和它下面的内容）。
2. 再往上翻，第一条以 `e: file:///...` 开头的 Kotlin 报错**整行**（通常带行号和列号）。

粘的时候保留原文，不要截图、不要自己概括——行号错一位我就要重猜一遍。
