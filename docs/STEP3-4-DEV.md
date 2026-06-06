# Step 3 & Step 4 开发文档

## 文件清单

```
src/components/nailit/
├── NailItApp.tsx        ← 状态串通（本次修改）
├── PrepScreen.tsx        ← Step 3（本次重写）
├── FocusScreen.tsx       ← Step 4（本次重写）
├── PhoneFrame.tsx        ← 无改动，Step 3/4 均复用
├── HomeScreen.tsx        ← 无改动
└── TryOnScreen.tsx       ← 无改动
```

---

## 一、架构总览

```
NailItApp (state machine)
├── screen: "prep"
│   └── <PrepScreen onStart={(missingItems) => ...} />
│
├── screen: "focus"
│   └── <FocusScreen missingItems={missingItems} />
│
└── missingItems: string[] ← PrepScreen 回传 → FocusScreen 消费
```

### NailItApp 状态（新增）

```ts
// src/components/nailit/NailItApp.tsx

const [missingItems, setMissingItems] = useState<string[]>([]);
```

| 时机 | 操作 |
|------|------|
| TryOn → Prep | `setMissingItems([])` 重置 |
| Prep → Focus | `setMissingItems(missing)` 记录 |
| Focus → Home | 退出时不做清理（下次从首页重新走流程会自然重置） |

---

## 二、Step 3：PrepScreen 开发详解

### 2.1 数据结构

```ts
// PrepScreen.tsx

type PrepItem = {
  id: string;             // key，映射到 missingItems[] 中的值
  label: string;          // 用户可见名称
  hint: string;           // 灰色副标题
  type: "tool" | "color"; // 决定替代方案的图标和风格
  alternatives: string[]; // 1~4 条替代建议
};

interface PrepSection {
  title: string;    // "基础工具" | "专属色号"
  items: PrepItem[];
}
```

`SECTIONS` 常量在当前版本是**静态数据**，但结构已预留为后续从 AI API 替换。接入真实后端时，只需把 `SECTIONS` 改为 `useState` + `useEffect` 拉取接口即可。

### 2.2 状态管理

```ts
const [items, setItems] = useState<Record<string, "have" | "dont-have" | null>>({});
const [expanded, setExpanded] = useState<Record<string, boolean>>({});
```

| State | 类型 | 含义 |
|-------|------|------|
| `items[id]` | `"have"` / `"dont-have"` / `null` | 该项当前选择 |
| `expanded[id]` | `boolean` | 该项替代面板是否展开 |

**派生值：**

```ts
const total = SECTIONS.flatMap(s => s.items).length;
const answered = Object.values(items).filter(v => v !== null).length;
const allAnswered = answered === total;          // → 控制 CTA 解锁
const haveCount = Object.values(items).filter(v => v === "have").length;
const missingItems = Object.entries(items)
  .filter(([,v]) => v === "dont-have")
  .map(([id]) => id);                            // → 传给 onStart
```

### 2.3 交互逻辑

#### 点击「我有」

```ts
const setItem = (id: string, val: "have" | "dont-have") => {
  setItems(prev => ({ ...prev, [id]: val }));
  if (val === "have") {
    setExpanded(prev => ({ ...prev, [id]: false })); // 收起替代面板
  }
};
```

#### 点击「没有」

- 状态设为 `"dont-have"`
- 不自动展开替代面板（用户需手动点 `💡 替代方案` 展开）
- 展开/收起通过 `toggleExpand(id)` 切换

#### CTA 点击

```ts
const handleStart = () => {
  if (!allAnswered) return;
  onStart(missingItems);  // 将缺失项 ID 列表传回父组件
};
```

### 2.4 视觉映射

| UI 元素 | Tailwind / Style | 说明 |
|---------|------------------|------|
| 我有按钮-激活 | `backgroundColor: "#A8D5BA"` + shadow | mint-green |
| 没有按钮-激活 | `backgroundColor: "#D4A3A3"` + shadow | brand rose |
| 两按钮-默认 | `bg-muted text-muted-foreground` | 灰色空心 |
| 替代面板背景 | `rgba(250,245,245,0.6)` | 暖色调淡底 |
| 替代面板分隔线 | `1px dashed rgba(212,163,163,0.25)` | 虚线，暗示非正式 |
| 替代项卡片 | `rounded-lg` + `rgba(255,255,255,0.7)` 背景 | 轻量卡片 |
| CTA 按钮 | `cta-gradient` utility | `#EBD8B8 → #D4A3A3` |
| 固定底部 | `fixed bottom-0 ... bg-gradient-to-t from-[#FAFAFA]` | 渐变遮罩过渡 |

### 2.5 图标使用

从 `lucide-react` 引入：
- `Check` — 我有按钮内勾号
- `X` — 没有按钮内叉号
- `ChevronDown` / `ChevronUp` — 替代面板折叠箭头

### 2.6 接入后端时的改动点

```ts
// 当前：静态数据
const SECTIONS = [ ... ];

// 改为：从 API 获取
const [sections, setSections] = useState<PrepSection[]>([]);

useEffect(() => {
  fetch(`/api/tutorial/${tutorialId}/bom`)
    .then(res => res.json())
    .then(setSections);
}, [tutorialId]);
```

数据结构 `PrepSection` / `PrepItem` 不变，接口返回相同格式即可。

---

## 三、Step 4：FocusScreen 开发详解

### 3.1 组件签名

```ts
export function FocusScreen({
  onExit,
  missingItems,
}: {
  onExit: () => void;
  missingItems: string[];
})
```

### 3.2 步骤数据

```ts
type SopStep = {
  step: number;              // 1~6
  total: number;             // 固定为 6
  title: string;             // "修形打磨" / "上底胶" ...
  instruction: string;       // 主指令（大号字）
  detail: string;            // 详细说明
  duration: number;          // 该步倒计时秒数（非固定，每步独立）
  isLampCure: boolean;       // 是否烤灯步骤
  originalQuote: string;     // 原视频里的原话
  aiTranslation: string;     // AI 翻译后的可操作指令
};

const STEPS: SopStep[] = [ /* 6 个步骤对象 */ ];
```

#### 6 步时长分配

| step | title | duration | isLampCure |
|------|-------|----------|------------|
| 1 | 修形打磨 | 120s | false |
| 2 | 上底胶 | 60s | false |
| 3 | 照灯固化底板 | 90s | true |
| 4 | 上色胶 | 120s | false |
| 5 | 照灯 + 封层 | 120s | true |
| 6 | 精修检查 | 60s | false |

接入后端时，`STEPS` 替换为从 AI 接口获取的动态步骤数据，格式一致。

### 3.3 状态管理

```ts
const [stepIdx, setStepIdx] = useState(0);             // 当前步骤索引
const [seconds, setSeconds] = useState(STEPS[0].duration); // 当前步骤倒计时
const [phase, setPhase] = useState<Phase>("active");    // 当前阶段
const [showAIClip, setShowAIClip] = useState(false);    // 原视频蒙层
const [showMissingBar, setShowMissingBar] = useState(true); // 缺失物料条
const [verifyMsg, setVerifyMsg] = useState("");         // 核验失败消息
const [showFinalVideo, setShowFinalVideo] = useState(false); // 视频预览
const [videoGenerating, setVideoGenerating] = useState(false); // 视频生成中
```

### 3.4 状态机：Phase

```
active ──→ verifying ──→ verified-ok ──→ stepIdx++ / completed
    ↑            │
    │            └──→ verified-fail ──→ 用户点"重试" → active
    │
    └── 用户点"跳过核验"直接 stepIdx++
```

```ts
type Phase = "active" | "verifying" | "verified-ok" | "verified-fail" | "completed";
```

#### 各阶段行为

| phase | 触发 | UI 表现 | 接下来 |
|-------|------|---------|--------|
| `active` | 默认进入步骤 / 从 fail 重试 | 倒计时运行，步骤图+指令+AI气泡可见 | 倒计时归零后拍照按钮高亮 |
| `verifying` | 点击拍照核验 | 旋转 loading 动画 + "AI 正在分析全方位照..." | 2.2s 后进入 ok 或 fail |
| `verified-ok` | AI 判定通过 | 绿色大勾 + "核验通过" + "进入下一步"按钮 | 用户点按钮 → next step |
| `verified-fail` | AI 判定不通过 | 红色叉号 + 具体错误消息 + "调整后重试"按钮 | 用户点按钮 → active |
| `completed` | 最后一步 verified-ok 后点"进入下一步" | 完成页 + 视频生成入口 | 生成视频 → 转发 |

### 3.5 倒计时逻辑

```ts
// 切换步骤时重置计时器
useEffect(() => {
  setSeconds(STEPS[stepIdx].duration);
  setPhase("active");
  setVerifyMsg("");
}, [stepIdx]);

// 倒计时 interval（仅在 active 阶段运行）
useEffect(() => {
  if (phase !== "active") return;
  const id = setInterval(() => {
    setSeconds(s => (s > 0 ? s - 1 : 0));
  }, 1000);
  return () => clearInterval(id);
}, [phase, stepIdx]);
```

**关键细节**：`useEffect` 依赖 `[phase, stepIdx]`，确保：
- 切换步骤时自动重置
- 进入 verifying/fail 阶段时暂停计时
- 从 fail 返回 active 时继续计时（不重置）

### 3.6 烤灯步骤视觉区分

```ts
const timerColor = step.isLampCure ? "#F5C542" : "#D4A3A3";
const timerLabel  = step.isLampCure ? "🔆 烤灯固化中" : "固化中";
```

倒计时 SVG 圆环的 `stroke` 属性和底部标签文案根据 `isLampCure` 切换。

### 3.7 AI 提示气泡

```tsx
<div
  className="rounded-2xl px-4 py-3 ..."
  style={{
    backgroundColor: "rgba(212,163,163,0.08)",
    border: "1px solid rgba(212,163,163,0.2)"
  }}
  onPointerDown={startAIHold}
  onPointerUp={cancelAIHold}
  onPointerLeave={cancelAIHold}
>
  {/* 📹 原视频说 */}
  <div style={{ color: "#D4A3A3" }}>📹 原视频说</div>
  <p>「{step.originalQuote}」</p>

  <div />  {/* 分隔线 */}

  {/* ✨ AI帮你翻译 */}
  <div style={{ color: "#A8D5BA" }}>✨ AI 帮你翻译</div>
  <p>{step.aiTranslation}</p>

  <p>💡 长按查看原视频片段</p>
</div>
```

长按逻辑：
```ts
const aiHoldRef = useRef<number | null>(null);

const startAIHold = () => {
  aiHoldRef.current = window.setTimeout(() => setShowAIClip(true), 600);
};
const cancelAIHold = () => {
  if (aiHoldRef.current) { clearTimeout(aiHoldRef.current); aiHoldRef.current = null; }
};
```

600ms 后弹出全屏暗色蒙层，任意处点击关闭。

### 3.8 拍照核验（Mock 实现）

```ts
const startVerification = () => {
  setPhase("verifying");
  setTimeout(() => {
    const pass = Math.random() > 0.35;  // 65% 通过率
    if (pass) {
      setPhase("verified-ok");
    } else {
      setVerifyMsg(VERIFY_MESSAGES_FAIL[Math.floor(Math.random() * 4)]);
      setPhase("verified-fail");
    }
  }, 2200);
};
```

失败消息池：
```ts
const VERIFY_MESSAGES_FAIL = [
  "甲面边缘检测到溢胶，请用棉签蘸酒精从甲缘外侧向内擦拭",
  "甲面颜色不均匀，左右深浅差异较大，建议薄涂再补一层",
  "检测到甲面有气泡/颗粒，可能是胶体太厚，建议擦掉重新薄涂",
  "包边不完整，指尖/两侧未完全覆盖，请用刷头补涂边缘",
];
```

**接入真实 AI 时的改动：**

```ts
const startVerification = async () => {
  setPhase("verifying");
  const photos = await captureMultiAngle();  // 调用摄像头拍多角度
  const result = await fetch("/api/verify-step", {
    method: "POST",
    body: JSON.stringify({ stepIdx, photos }),
  }).then(r => r.json());

  if (result.pass) {
    setPhase("verified-ok");
  } else {
    setVerifyMsg(result.suggestion);  // AI 返回的具体建议
    setPhase("verified-fail");
  }
};
```

### 3.9 视频生成（Mock 实现）

```ts
const handleGenerateVideo = () => {
  setVideoGenerating(true);
  setTimeout(() => {
    setVideoGenerating(false);
    setShowFinalVideo(true);
  }, 4000);
};
```

视频预览页：
- 尺寸：`aspect-[9/16]`（抖音竖屏比例），`max-w-[320px]`
- 内容：播放按钮占位 + 话题标签（#美甲DIY #指尖SOP #沉浸式）
- 转发按钮：抖音红色 `#FF004F`

**接入真实 AI 时：**

```ts
const handleGenerateVideo = async () => {
  setVideoGenerating(true);
  const videoUrl = await fetch("/api/generate-video", {
    method: "POST",
    body: JSON.stringify({ tutorialId, stepPhotos }),
  }).then(r => r.json()).then(d => d.url);

  setGeneratedVideoUrl(videoUrl);
  setVideoGenerating(false);
  setShowFinalVideo(true);
};
```

### 3.10 缺失物料提醒

来自 Step 3 的 `missingItems: string[]`，在页面顶部渲染：

```tsx
{missingItems.length > 0 && (
  <div className="absolute top-0 left-0 right-0 z-20">
    <button onClick={() => setShowMissingBar(!showMissingBar)}>
      <AlertTriangle />
      替代方案已就绪 · {missingItems.length} 项物料需注意
    </button>
    {showMissingBar && (
      <div>{/* 展开的替代清单 */}</div>
    )}
  </div>
)}
```

**ID 到中文名的映射**是通过局部常量完成（临时方案），后续可以从 Step 3 传入完整的 `PrepItem[]` 替代 `string[]`。

### 3.11 长按退出

全局层级，作用在背景容器上：
```ts
onPointerDown={startExitHold}
onPointerUp={cancelExitHold}
onPointerLeave={cancelExitHold}
```
800ms 后调用 `onExit()`。

### 3.12 语音助手

```tsx
<div className="flex items-end gap-[3px] h-5">
  <span className="animate-wave" style={{ animationDelay: "0ms" }} />
  <span className="animate-wave" style={{ animationDelay: "150ms" }} />
  <span className="animate-wave" style={{ animationDelay: "300ms" }} />
  <span className="animate-wave" style={{ animationDelay: "150ms" }} />
</div>
```

使用 `styles.css` 中已有的 `@keyframes wave-bar` 和 `.animate-wave` utility，4 根竖条错开 150ms 产生波浪效果。当前为纯 CSS 动画占位，接入真实 TTS 后替换为语音状态指示器。

### 3.13 步骤导航按钮

```tsx
// 上一步
<button onClick={goPrev} disabled={stepIdx === 0}>
  <ChevronLeft /> 上一步
</button>

// 下一步（直接跳过核验）
<button onClick={goNext}>
  {isLast ? "完成" : "跳过核验 →"}
  <ChevronRight />
</button>
```

`goPrev` 和 `goNext` 只修改 `stepIdx`，`useEffect` 会自动重置倒计时和 phase。

---

## 四、组件通信图

```
NailItApp
 │
 ├── [screen=prep] ────────────────────────┐
 │   <PrepScreen                           │
 │     onBack={() => setScreen("tryon")}    │
 │     onStart={(missing) => {             │
 │       setMissingItems(missing);         │
 │       setScreen("focus");               │
 │     }}                                  │
 │   />                                    │
 │                                         │
 └── [screen=focus] ───────────────────────┤
     <FocusScreen                          │
       onExit={() => setScreen("home")}    │
       missingItems={missingItems}    ←────┘
     />
```

---

## 五、样式依赖

两个组件无需额外 CSS 文件，全部使用：

1. **Tailwind v4** utility classes
2. **Inline `style`** 属性（用于动态颜色值如 `#A8D5BA`、`#D4A3A3`、`#F5C542`）
3. **`styles.css` 中已有的 utilities**：`cta-gradient`、`animate-wave`
4. **shadcn/ui 的 CSS 变量**：`--color-muted`、`--color-muted-foreground`、`--color-brand` 等
5. **`lucide-react`** 图标组件

---

## 六、后续接入真实 API 的 checklist

| 模块 | 当前实现 | 改为真实 API |
|------|----------|-------------|
| PrepScreen 物料数据 | 静态 `SECTIONS` 常量 | `GET /api/tutorial/:id/bom` |
| PrepScreen 替代方案 | 静态 `alternatives[]` | AI 接口返回 |
| FocusScreen 步骤数据 | 静态 `STEPS` 常量 | `GET /api/tutorial/:id/steps` |
| FocusScreen AI 核验 | `Math.random()` + 静态消息池 | `POST /api/verify-step` 传入多角度照片 |
| FocusScreen 视频生成 | `setTimeout` 模拟 | `POST /api/generate-video` 返回视频 URL |
| FocusScreen 语音助手 | CSS 动画占位 | Web Speech API / TTS 服务 |
| missingItems 传递 | 传 `string[]` id | 改为传完整 `PrepItem[]` 对象以便展示详情 |
