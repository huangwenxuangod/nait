# 指尖 SOP · Nail-It — 开发变更总结

> 项目地址：https://github.com/huangwenxuangod/nait  
> 分支：`demo-final`  
> 基础项目：https://github.com/catarina-xxxcc/nail-ai-guide (TanStack Start 模板)

---

## 一、新增文件 (14 个)

### 核心模块

| 文件 | 说明 |
|------|------|
| `src/lib/types.ts` | 全流程共享类型定义 (`TutorialData`, `SopStep`, `PrepItem`, `PrepSection`) |
| `src/lib/media.ts` | 视频文件引用工具 (`getTutorialVideo`) |
| `src/lib/video-generator.ts` | 视频录制 & 剪辑模块 + 分享接口 |
| `src/lib/api/qwen.ts` | 阿里云百炼 Qwen API 客户端 (`chatCompletion`) |
| `src/lib/api/tutorial.functions.ts` | TanStack Start Server Function — 视频链接解析 (Qwen + 缓存) |

### Remotion 视频合成

| 文件 | 说明 |
|------|------|
| `src/remotion/Root.tsx` | Remotion 注册入口 (`nail-tutorial` 合成) |
| `src/remotion/NailTutorialVideo.tsx` | 抖音风格 9:16 竖屏视频合成组件 |

### 新增页面

| 文件 | 说明 |
|------|------|
| `src/components/nailit/InspirationScreen.tsx` | 灵感页 — 抖音美甲视频合集 (10 卡片 + 分类标签) |
| `src/components/nailit/ProfileScreen.tsx` | 我的页 — 心愿单 + 已完成记录 + 统计 |
| `src/vite-env.d.ts` | Vite 类型声明 (`.mp4` / `.webm`) |

### 文档

| 文件 | 说明 |
|------|------|
| `docs/STEP3-4-TECH.md` | Step 3 & 4 产品设计文档 |
| `docs/STEP3-4-DEV.md` | Step 3 & 4 开发文档 (含 API 接入指引) |

### 配置

| 文件 | 说明 |
|------|------|
| `.env` | 环境变量 (`QWEN_API_KEY`, 已 `.gitignore` 保护) |
| `.gitattributes` | Git LFS 配置 (`.mp4` 文件) |

---

## 二、改造文件 (6 个)

### NailItApp.tsx — 状态机重构
- 新增 `tutorialData: TutorialData | null` — 全流程数据串通
- 新增 `missingItems: string[]` — PrepScreen → FocusScreen 物料缺失传递
- 新增 `wishlist: string[]` & `completed: [...]` — 心愿单 & 完成记录
- 新增页面切换过渡动画 (`screen-fade-in` 0.25s)
- 新增底部 Tab 栏 (首页 / 灵感 / 我的)，制作流程中隐藏
- 完成制作后自动记录到"已完成"列表

### HomeScreen.tsx — 链接解析入口
- 粘贴链接 → 4 步解析动画 (识别来源 → 获取内容 → AI 拆解 → 生成 SOP)
- 调用 `parseTutorialLink()` 服务器函数 (Qwen AI)
- 解析完成自动跳转试戴页并加入心愿单
- 移除内嵌底部 Tab 栏 (提至 NailItApp 层级)

### PrepScreen.tsx — 个性化准备确认系统 (重写，123 → 492 行)
- 每项物料双按钮：`✅ 我有` / `❌ 我没有`
- 点击 `❌` 展开替代方案面板 (工具 → 🔧 替代品 / 色号 → 🎨 替代色)
- 每个工具可点开详情弹窗：📖 是什么？👀 长什么样？💡 新手避坑
- "一键全选"快捷按钮
- 全部确认才解锁 CTA，缺失清单传入 Step 4
- 接受动态 `tutorialData` 渲染 BOM 清单

### FocusScreen.tsx — AI 辅助多步骤制作页 (重写，101 → 493 行)
- 6 步 SOP 流程，每步独立时长 + 视频自动 seek
- 烤灯步骤 Amber 色倒计时环区分
- AI 解读气泡 (`✨ AI 解读`)
- 步骤进度条 (圆点连接线，已完成变绿)
- 拍照核验 (Demo 模式固定通过)
- 学习模式：先看视频 → 点"开始操作"才计时
- 倒计时 + 语音 + 核验三元素一排展示
- 缺失物料顶部提醒条
- Remotion 视频生成 + 抖音分享跳转
- 左上角 `← 退出` 按钮替代长按
- 接受动态 `tutorialData` 渲染步骤

### TryOnScreen.tsx — 视频播放 + AI 解析展示
- 视频播放器 (自动检测 `public/tutorial-video.mp4`)
- "查看原视频" / "AI 试戴效果" 切换
- AI 解析信息卡片 (款名 + 风格描述 + 平台标签)
- 接受 `tutorialData` 渲染

### styles.css — 新增动画
- `@keyframes screen-fade-in` — 页面切换淡入 + 上滑

---

## 三、功能架构

```
用户粘贴抖音链接
  │
  ├─ HomeScreen: 4 步解析动画 (3.2s)
  │   └─ parseTutorialLink() ─→ Qwen API 返回 TutorialData
  │
  ├─ TryOnScreen: 视频播放 + AI 解析卡片
  │   └─ 确认 → PrepScreen
  │
  ├─ PrepScreen: 物料确认 (我有/没有)
  │   └─ 缺失清单传入 FocusScreen
  │
  └─ FocusScreen: 多步骤 SOP
      ├─ 学习模式 (看视频)
      ├─ 开始操作 (倒计时)
      ├─ 拍照核验 (固定通过)
      ├─ 完成 → Remotion 视频生成
      └─ 一键转发到抖音
```

---

## 四、AI 集成

| 环节 | 技术 | 状态 |
|------|------|------|
| 视频链接解析 | 阿里云百炼 Qwen (qwen-plus) | ✅ 真实 API |
| 步骤数据缓存 | 内存 Map (同链接永远相同结果) | ✅ |
| 视频合成 | Remotion (浏览器预览 / CLI 渲染) | ✅ 框架就绪 |
| 拍照核验 | Mock (固定通过) | 🟨 待接真实 API |
| 语音助手 | CSS 动画占位 | 🟨 待接 Web Speech API |
| 视频下载/抽帧 | 无 (千问只做文本) | 🟨 待接视频处理 pipeline |

---

## 五、UI/UX 配色体系

| 用途 | 色值 | CSS 变量/Utility |
|------|------|------------------|
| 背景象牙白 | `#FAFAFA` | `--background` |
| 文字深灰 | `#2C2C2C` | `--foreground` |
| 品牌玫瑰金 | `#D4A3A3` | `--brand` / `text-brand` |
| 成功薄荷绿 | `#A8D5BA` | `--success` |
| 烤灯琥珀色 | `#F5C542` | — |
| CTA 渐变 | `#EBD8B8 → #D4A3A3` | `cta-gradient` |
| 抖音分享红 | `#FF004F` | — |

---

## 六、环境配置

```env
# .env (不提交到 Git)
QWEN_API_KEY=sk-xxx
QWEN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
QWEN_TEXT_MODEL=qwen-plus
```

```bash
# 启动
cd /Users/a123/Desktop/nails && bun dev
# → http://localhost:8080/

# Remotion 渲染 (命令行导出 mp4)
npx remotion render nail-tutorial output.mp4
```

---

## 七、数据流

```
NailItApp (state machine)
├── screen: "home"        → HomeScreen
├── screen: "inspiration" → InspirationScreen  (10 视频卡片)
├── screen: "profile"     → ProfileScreen      (心愿单 + 已完成)
├── screen: "tryon"       → TryOnScreen        (视频 + AI 解析)
├── screen: "prep"        → PrepScreen         (物料确认)
├── screen: "focus"       → FocusScreen        (多步骤 SOP)
│
├── tutorialData: TutorialData | null  ← parseTutorialLink() 返回
├── missingItems: string[]             ← PrepScreen 回传 → FocusScreen 消费
├── wishlist: string[]                 ← 灵感页收藏 / 解析链接自动添加
└── completed: CompletedItem[]         ← FocusScreen 完成时自动记录
```

---

## 八、后续升级 Checklist

| 模块 | 当前 | 升级目标 |
|------|------|---------|
| 视频解析 | Qwen 文本生成 (每次不同) | 接视频下载 + ffmpeg 抽帧 + Qwen-VL 真正看视频 |
| 拍照核验 | Mock 100% 通过 | 接摄像头 + AI 图像识别 |
| 语音助手 | CSS 动画 | Web Speech API TTS 读出步骤指令 |
| 视频生成 | Remotion 预览 | `npx remotion render` 导出 mp4 |
| 分享功能 | 抖音 URL Scheme | 抖音开放平台 SDK + 小红书/微信 |
| 历史记录 | 仅"我的"页展示 | localStorage 持久化 |
| 心愿单 | 内存 state | localStorage 持久化 |
