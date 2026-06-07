# 指尖 SOP · Nail-It

> 试戴看效果 · AI 拆解教程 · 跟着做

刷到抖音美甲教程 → 粘贴链接 → AI 自动拆解 → 虚拟试戴 → 分步语音引导 → 真能做出来。

---

## 体验方式

```bash
git clone https://github.com/huangwenxuangod/nait.git
cd nait
bun install
bun dev
```

打开 http://localhost:8080

> 需要配置 Qwen API Key 才能使用 AI 解析：创建 `.env` 文件写入 `QWEN_API_KEY=你的Key`

---

## 核心功能

| 功能 | 说明 |
|------|------|
| 🔗 链接解析 | 粘贴抖音/小红书/YouTube 链接，通义千问自动拆解款式、色号、步骤 |
| 💅 虚拟试戴 | 6 种甲型 + 3 档长度对比，款式"穿"到手上看效果 |
| ✅ 物料确认 | 一键确认有没有，缺的自动推荐替代方案 |
| 🎙 分步引导 | 视频示范 + 子弹点指令 + 倒计时 + 语音播报 |
| 📸 拍照核验 | 每步完成拍照，AI 检查是否做到位 |
| 🤚 镜像模式 | 非惯用手专属提示，左手做右手不再难 |
| 🎬 视频生成 | 完成自动剪辑抖音风格短视频，一键分享 |
| 👤 性别选择 | 男生/女生推荐不同风格和甲型 |

---

## 技术栈

React 19 · TanStack Start · TypeScript · Tailwind CSS v4 · shadcn/ui · 通义千问 Qwen · Remotion · Web Speech API · Bun

---

## 文档

| 文档 | 内容 |
|------|------|
| [使用指引](docs/USAGE_GUIDE.md) | 7 步操作指南 |
| [项目介绍](docs/PROJECT_INTRO.md) | 定位/痛点/创新点 |
| [产品设计](docs/STEP3-4-TECH.md) | Step 3 & 4 设计文档 |
| [开发文档](docs/STEP3-4-DEV.md) | 技术实现 + API 接入 |
| [变更记录](docs/CHANGELOG.md) | 全部开发变更 |
