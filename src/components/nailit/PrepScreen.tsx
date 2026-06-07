import { Check, ChevronDown, ChevronUp, HelpCircle, X } from "lucide-react";
import { useMemo, useState } from "react";
import { PhoneFrame } from "./PhoneFrame";
import type { TutorialData, PrepItem, PrepSection } from "@/lib/types";
import thumb from "@/assets/nail-thumb.jpg";

const SECTIONS: PrepSection[] = [
  {
    title: "基础工具",
    items: [
      {
        id: "base", label: "底胶", hint: "让颜色牢牢扒在指甲上",
        type: "tool",
        alternatives: [{ label: "水性亮油", desc: "文具店/美妆店有售，临时替代但持久度减半", emoji: "💅" }],
      },
      {
        id: "lamp", label: "烤灯", hint: "把胶照干固化",
        type: "tool",
        alternatives: [
          { label: "LED 手电筒 + 锡纸", desc: "用锡纸卷成锥形聚光，贴近甲面照射 2-3 分钟", emoji: "🔦" },
          { label: "户外阳光", desc: "晴天时手伸到阳光直射处 2-3 分钟，紫外线自然固化", emoji: "☀️" },
        ],
      },
      {
        id: "top", label: "封层", hint: "最后涂的保护层，亮面效果",
        type: "tool",
        alternatives: [{ label: "高光透明指甲油", desc: "普通指甲油店的透明亮油，不需照灯但持久度差一些", emoji: "✨" }],
      },
      {
        id: "file", label: "海绵搓条 + 抛光锉", hint: "修指甲形状、打磨甲面",
        type: "tool",
        alternatives: [
          { label: "厨房海绵（粗面）", desc: "洗碗海绵的粗糙面可以替代粗打磨，注意顺着一个方向", emoji: "🧽" },
          { label: "废弃指甲锉", desc: "旧的指甲锉条也可以临时用，细砂面抛光", emoji: "📏" },
        ],
      },
      {
        id: "cleaner", label: "清洁液 + 棉片", hint: "擦掉表面浮胶和油脂",
        type: "tool",
        alternatives: [
          { label: "75% 医用酒精 + 化妆棉", desc: "药店买酒精，搭配化妆棉片擦拭甲面", emoji: "🧴" },
          { label: "免洗洗手液", desc: "含酒精的免洗洗手液挤在棉片上擦拭", emoji: "🧼" },
        ],
      },
    ],
  },
  {
    title: "专属色号",
    items: [
      {
        id: "rose", label: "哑光玫瑰豆沙胶", hint: "温柔的玫瑰豆沙色",
        type: "color",
        alternatives: [
          { label: "#D4A0A0 干枯玫瑰", desc: "偏暖调的玫瑰色，上手效果非常接近", emoji: "🌸" },
          { label: "#E8C4C4 藕粉色", desc: "颜色偏浅偏粉，适合喜欢淡妆风格的", emoji: "🎀" },
          { label: "裸色打底 + 玫瑰金闪粉", desc: "任何裸色甲胶打底，薄撒一层玫瑰金闪粉叠加", emoji: "💫" },
        ],
      },
      {
        id: "aurora", label: "极光镭射粉", hint: "极光偏光效果的点睛之笔",
        type: "color",
        alternatives: [
          { label: "透明闪粉 + 珍珠白叠加", desc: "细闪粉混入透明甲胶，再刷一层珍珠白微光感", emoji: "🌟" },
          { label: "眼影盘偏光高光粉", desc: "用化妆刷蘸取眼影盘里的偏光色，轻拍在未干甲面上", emoji: "🎨" },
          { label: "银色极光纸碎片", desc: "极光包装纸剪碎后撒在甲面，未干时封层覆盖", emoji: "✂️" },
        ],
      },
    ],
  },
];

const ITEM_DETAILS: Record<string, { emoji: string; desc: string; lookLike: string; tip: string }> = {
  base: {
    emoji: "💧", desc: "美甲第一步用的透明液体，作用是让后续的颜色牢牢附着在指甲上——相当于化妆的打底/妆前乳。涂完后需要照灯固化。",
    lookLike: "透明、微粘稠的液体。通常装在白色或磨砂玻璃瓶里，瓶盖连着一把小刷子，刷头宽约 1cm。打开后闻起来有一点点化学味。",
    tip: "新手最容易翻车的地方：涂太厚反而容易整片脱落。正确做法是刷子在瓶口刮掉 2/3 的胶，薄薄一层即可。",
  },
  lamp: {
    emoji: "💡", desc: "美甲专用的小型灯箱，发出 UV 或 LED 紫外线光，用来快速照干/固化甲胶。不做美甲胶的话这个步骤没法跳过。",
    lookLike: "一个白色或粉色的小盒子，大小和鞋盒差不多。中间有个凹槽可以把手伸进去。顶部和侧面排列着一排排小灯珠（LED 型号）或者灯管（UV 型号）。通常带一个定时按钮。",
    tip: "手放进去后中途不要拿出来看，否则没固化的胶会沾灰或起皱。一般 60-90 秒就能固化一层。",
  },
  top: {
    emoji: "🛡️", desc: "美甲最后一步涂的透明保护胶，作用是防刮、防褪色，并且给甲面带来漂亮的镜面光泽感。没有封层的话甲胶大概一周就会开始变暗掉色。",
    lookLike: "外观和底胶很像，透明液体装在瓶子里。区别是瓶身通常会标注 Top Coat / 封层。有些品牌还有哑光封层（雾面效果）的选择。",
    tip: "封层一定要包边——刷子沿着指甲尖边缘轻扫一圈，否则 2-3 天后就会从指尖开始翘起来。",
  },
  file: {
    emoji: "📐", desc: "用来修整指甲形状和打磨甲面的工具。通常一组包含粗锉条（修形用）和细抛光锉（让甲面光滑）。",
    lookLike: "海绵搓条大约 15-20cm 长、2-3cm 宽的长条，一面是粗颗粒（浅灰/粉色）、一面是细颗粒。抛光锉更小块，触感接近细砂纸。",
    tip: "打磨要顺着一个方向推，不要来回拉锯——来回拉会损伤甲面产生毛刺，还容易让指甲分层断裂。",
  },
  cleaner: {
    emoji: "🧴", desc: "每次照灯后甲面会残留一层黏黏的浮胶，需要用清洁液蘸棉片擦掉，才能涂下一层。最后一步也要用清洁液擦一遍让甲面干净光滑。",
    lookLike: "通常是指甲油瓶大小的小瓶装液体，透明无色，挥发快，闻到有酒精味。搭配一盒小棉片或无尘化妆棉使用。",
    tip: "不擦浮胶就涂下一层，两层胶之间会粘不牢，容易起泡。用棉片朝一个方向轻擦，不要在甲面上反复摩擦。",
  },
  rose: {
    emoji: "🌷", desc: "一种低饱和度的干枯玫瑰色甲胶，上甲后是温柔高级的豆沙粉调，不挑肤色，是通勤和约会都合适的百搭色。",
    lookLike: "瓶子里看起来是偏灰调的粉色液体，涂在甲面上照灯后会稍微深一点点。哑光封层下呈现丝绒质感，亮面封层下呈奶茶玫瑰色。",
    tip: "薄涂一层是半透明的裸粉感，厚涂两层才会达到瓶身示范色的饱和度。记得每层之间照灯再涂下一层。",
  },
  aurora: {
    emoji: "🌌", desc: '极光镭射粉是一种微细的偏光粉末，撒在甲面上会在不同角度折射出蓝紫绿渐变的光泽，像极光一样。是这款美甲的"亮点担当"。',
    lookLike: "非常细的粉末状，装在透明小圆罐里。本身看起来是接近白色的闪光粉，但涂在甲面上随着光线变化会呈现蓝紫色偏光。",
    tip: "在封层照灯之前撒粉，然后用手指轻轻按压让粉末贴合甲面，再上最后一道封层保护。不能先封层再撒粉，否则粉末粘不住。",
  },
};

export function PrepScreen({ onBack, onStart, tutorialData }: { onBack: () => void; onStart: (missing: string[]) => void; tutorialData: TutorialData | null }) {
  const sections = tutorialData?.prep ?? SECTIONS;
  const [items, setItems] = useState<Record<string, "have" | "dont-have" | null>>({});
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const [detailId, setDetailId] = useState<string | null>(null);

  const setItem = (id: string, val: "have" | "dont-have") => {
    setItems((prev) => ({ ...prev, [id]: val }));
    if (val === "have") setExpanded((prev) => ({ ...prev, [id]: false }));
  };

  const toggleExpand = (id: string) => {
    setExpanded((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const total = useMemo(() => sections.reduce((s, x) => s + x.items.length, 0), [sections]);
  const answered = Object.values(items).filter((v) => v !== null && v !== undefined).length;
  const allAnswered = answered === total;
  const haveCount = Object.values(items).filter((v) => v === "have").length;
  const missingItems = Object.entries(items)
    .filter(([, v]) => v === "dont-have")
    .map(([id]) => id);

  const handleStart = () => {
    if (!allAnswered) return;
    onStart(missingItems);
  };

  const selectAll = () => {
    const next = { ...items };
    for (const sec of sections) {
      for (const it of sec.items) {
        next[it.id] = "have";
      }
    }
    setItems(next);
    setExpanded({});
  };

  const detailData = detailId ? ITEM_DETAILS[detailId] : null;
  const findItem = (id: string) => {
    for (const sec of sections) {
      const found = sec.items.find((x) => x.id === id);
      if (found) return found;
    }
    return null;
  };
  const detailItem = detailId ? findItem(detailId) : null;

  return (
    <PhoneFrame>
      <div className="flex flex-col min-h-screen pb-36">
        {/* Header */}
        <div className="px-7 pt-14">
          <button onClick={onBack} className="text-xs text-muted-foreground tracking-widest mb-3">
            ← 返回
          </button>
          <div className="flex items-end justify-between">
            <div>
              <p className="text-[10px] tracking-[0.4em] text-brand uppercase">Step 01</p>
              <h2 className="mt-2 text-2xl font-light tracking-wide">准备你的物料</h2>
            </div>
            <div className="w-14 h-14 rounded-2xl overflow-hidden shadow-md">
              <img src={thumb} alt="款式" className="w-full h-full object-cover" />
            </div>
          </div>
          <p className="mt-3 text-xs text-muted-foreground">
            {allAnswered ? (
              <>
                已备齐 <span className="text-success font-medium">{haveCount}</span> 项 ·{" "}
                {missingItems.length > 0 && (
                  <span className="text-brand">
                    需替代 <span className="font-medium">{missingItems.length}</span> 项
                  </span>
                )}
                {missingItems.length === 0 && <span className="text-success">全部就绪</span>}
              </>
            ) : (
              <>
                已确认 <span className="text-brand">{answered}</span> / {total} · 不认识的工具点一下看看
              </>
            )}
          </p>

          {!allAnswered && answered < total && (
            <button
              onClick={selectAll}
              className="mt-3 w-full flex items-center justify-center gap-2 py-2.5 rounded-xl text-xs font-medium tracking-wider transition active:scale-[0.98]"
              style={{
                backgroundColor: "rgba(168,213,186,0.12)",
                color: "#5A9B7A",
                border: "1px dashed rgba(168,213,186,0.4)",
              }}
            >
              <Check className="w-3.5 h-3.5" strokeWidth={2.5} />
              一键全选 · 全都准备好了
            </button>
          )}
        </div>

        {/* Lists */}
        <div className="mt-8 px-7 space-y-8">
          {sections.map((sec) => (
            <div key={sec.title}>
              <p className="text-[11px] tracking-[0.3em] text-muted-foreground/80 uppercase mb-3">{sec.title}</p>
              <div className="bg-white rounded-2xl shadow-[0_4px_24px_-10px_rgba(0,0,0,0.08)] hover:shadow-[0_6px_30px_-8px_rgba(0,0,0,0.15)] transition-shadow duration-300 divide-y divide-border/60">
                {sec.items.map((it) => {
                  const status = items[it.id] ?? null;
                  const isExpanded = expanded[it.id] ?? false;
                  const isDontHave = status === "dont-have";
                  const detail = ITEM_DETAILS[it.id];

                  return (
                    <div key={it.id}>
                      <div className="flex items-center gap-3 px-5 py-4">
                        {/* Emoji + touchable label area */}
                        <button
                          onClick={() => setDetailId(it.id)}
                          className="flex-1 flex items-center gap-3 min-w-0 text-left"
                        >
                          <div
                            className="w-11 h-11 rounded-xl flex items-center justify-center text-xl shrink-0"
                            style={{
                              backgroundColor: isDontHave
                                ? "rgba(212,163,163,0.1)"
                                : status === "have"
                                  ? "rgba(168,213,186,0.12)"
                                  : "rgba(0,0,0,0.04)",
                            }}
                          >
                            {detail?.emoji ?? "📦"}
                          </div>
                          <div className="min-w-0">
                            <p
                              className="text-sm"
                              style={{ color: isDontHave ? "#9CA3AF" : "#2C2C2C" }}
                            >
                              {it.label}
                            </p>
                            <p
                              className="text-[11px] mt-0.5"
                              style={{ color: isDontHave ? "#B0B0B0" : "var(--color-muted-foreground)" }}
                            >
                              {it.hint}
                            </p>
                          </div>
                          <HelpCircle
                            className="w-3.5 h-3.5 shrink-0"
                            style={{ color: "var(--color-muted-foreground)", opacity: 0.4 }}
                          />
                        </button>

                        {/* Action buttons */}
                        <div className="flex items-center gap-2 shrink-0">
                          <button
                            onClick={(e) => { e.stopPropagation(); setItem(it.id, "have"); }}
                            className={`w-16 h-8 rounded-full text-xs font-medium tracking-wide transition flex items-center justify-center gap-1 ${
                              status === "have"
                                ? "text-white shadow-[0_2px_10px_-4px_rgba(168,213,186,0.6)]"
                                : "bg-muted text-muted-foreground hover:bg-success/20 hover:text-success"
                            }`}
                            style={status === "have" ? { backgroundColor: "#A8D5BA" } : undefined}
                          >
                            <Check className="w-3.5 h-3.5" strokeWidth={3} />
                            我有
                          </button>
                          <button
                            onClick={(e) => { e.stopPropagation(); setItem(it.id, "dont-have"); }}
                            className={`w-16 h-8 rounded-full text-xs font-medium tracking-wide transition flex items-center justify-center gap-1 ${
                              isDontHave
                                ? "text-white shadow-[0_2px_10px_-4px_rgba(212,163,163,0.6)]"
                                : "bg-muted text-muted-foreground hover:bg-brand/15 hover:text-brand"
                            }`}
                            style={isDontHave ? { backgroundColor: "#D4A3A3" } : undefined}
                          >
                            <X className="w-3.5 h-3.5" strokeWidth={3} />
                            没有
                          </button>
                        </div>
                      </div>

                      {/* Alternatives cards */}
                      {isDontHave && (
                        <div
                          className="px-4 pb-4 pt-1"
                          style={{
                            backgroundColor: "rgba(250, 245, 245, 0.6)",
                            borderTop: "1px dashed rgba(212, 163, 163, 0.2)",
                          }}
                        >
                          <button
                            onClick={() => toggleExpand(it.id)}
                            className="w-full flex items-center justify-between pt-2 pb-1 text-xs"
                            style={{ color: "#D4A3A3" }}
                          >
                            <span className="tracking-widest text-[11px]">
                              💡 {it.type === "color" ? "替代色号" : "替代方案"}（{it.alternatives.length} 个选择）
                            </span>
                            {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                          </button>
                          {isExpanded && (
                            <div className="grid grid-cols-1 gap-2 pt-2 pb-1">
                              {it.alternatives.map((alt, i) => (
                                <div
                                  key={i}
                                  className="flex items-start gap-3 p-3 rounded-xl"
                                  style={{ backgroundColor: "rgba(255,255,255,0.75)" }}
                                >
                                  <div
                                    className="w-10 h-10 rounded-lg flex items-center justify-center text-lg shrink-0"
                                    style={{ backgroundColor: "rgba(212,163,163,0.08)" }}
                                  >
                                    {alt.emoji}
                                  </div>
                                  <div className="min-w-0">
                                    <p className="text-xs font-medium mb-0.5" style={{ color: "#5B4B4B" }}>
                                      {alt.label}
                                    </p>
                                    <p className="text-[11px] leading-relaxed" style={{ color: "#8B7B7B" }}>
                                      {alt.desc}
                                    </p>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>

        {/* Sticky CTA */}
        <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[430px] px-7 pb-8 pt-6 bg-gradient-to-t from-[#FAFAFA] via-[#FAFAFA]/95 to-transparent">
          {!allAnswered && (
            <p className="text-center text-[10px] tracking-widest text-muted-foreground/70 mb-3">
              每一项都需确认 · 还剩 {total - answered} 项
            </p>
          )}
          <button
            onClick={handleStart}
            disabled={!allAnswered}
            className={`w-full rounded-full py-4 text-sm tracking-[0.25em] transition ${
              allAnswered
                ? "cta-gradient text-white shadow-[0_10px_30px_-10px_rgba(212,163,163,0.6)] active:scale-[0.98]"
                : "bg-muted text-muted-foreground/60"
            }`}
          >
            {allAnswered
              ? missingItems.length > 0
                ? "已记录替代方案 · 开始制作"
                : "全部就绪 · 开始沉浸式制作"
              : "请确认每一项物料"}
          </button>
        </div>

        {/* ====== Item Detail Modal ====== */}
        {detailId && detailData && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center p-6"
            style={{ backgroundColor: "rgba(0,0,0,0.45)" }}
            onClick={() => setDetailId(null)}
          >
            <div
              className="w-full max-w-[380px] rounded-3xl overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
              style={{
                backgroundColor: "#FAFAFA",
                maxHeight: "80vh",
                boxShadow: "0 20px 60px rgba(0,0,0,0.2)",
              }}
            >
              {/* Close button */}
              <div className="flex justify-end px-4 pt-4">
                <button
                  onClick={() => setDetailId(null)}
                  className="w-8 h-8 rounded-full flex items-center justify-center"
                  style={{ backgroundColor: "rgba(0,0,0,0.05)" }}
                >
                  <X className="w-4 h-4" style={{ color: "#999" }} />
                </button>
              </div>

              {/* Hero emoji area */}
              <div
                className="mx-5 rounded-2xl flex items-center justify-center mb-6"
                style={{
                  height: "140px",
                  backgroundColor: detailId === "lamp" || detailId === "aurora" || detailId === "rose"
                    ? "rgba(212,163,163,0.08)"
                    : "rgba(168,213,186,0.08)",
                }}
              >
                <span className="text-5xl">{detailData.emoji}</span>
              </div>

              <div className="px-5 pb-8">
                {/* Name + status at top */}
                <div className="flex items-center justify-between mb-5">
                  <h3 className="text-xl font-light tracking-wide" style={{ color: "#2C2C2C" }}>
                    {detailItem?.label ?? detailId}
                  </h3>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => { setItem(detailId, "have"); }}
                      className={`w-14 h-7 rounded-full text-xs font-medium tracking-wide transition flex items-center justify-center gap-0.5 ${
                        items[detailId] === "have"
                          ? "text-white"
                          : "bg-muted text-muted-foreground"
                      }`}
                      style={items[detailId] === "have" ? { backgroundColor: "#A8D5BA" } : undefined}
                    >
                      <Check className="w-3 h-3" strokeWidth={3} /> 我有
                    </button>
                    <button
                      onClick={() => { setItem(detailId, "dont-have"); }}
                      className={`w-14 h-7 rounded-full text-xs font-medium tracking-wide transition flex items-center justify-center gap-0.5 ${
                        items[detailId] === "dont-have"
                          ? "text-white"
                          : "bg-muted text-muted-foreground"
                      }`}
                      style={items[detailId] === "dont-have" ? { backgroundColor: "#D4A3A3" } : undefined}
                    >
                      <X className="w-3 h-3" strokeWidth={3} /> 没有
                    </button>
                  </div>
                </div>

                {/* Description */}
                <div className="mb-5">
                  <p className="text-[11px] tracking-widest uppercase mb-2" style={{ color: "#D4A3A3" }}>
                    📖 这是什么？
                  </p>
                  <p className="text-sm leading-relaxed" style={{ color: "#4B3B3B" }}>
                    {detailData.desc}
                  </p>
                </div>

                {/* What it looks like */}
                <div className="mb-5">
                  <p className="text-[11px] tracking-widest uppercase mb-2" style={{ color: "#D4A3A3" }}>
                    👀 长什么样？
                  </p>
                  <p className="text-sm leading-relaxed" style={{ color: "#4B3B3B" }}>
                    {detailData.lookLike}
                  </p>
                </div>

                {/* Pro tip */}
                <div className="mb-5 rounded-xl p-4" style={{ backgroundColor: "rgba(168,213,186,0.10)" }}>
                  <p className="text-[11px] tracking-widest uppercase mb-1.5" style={{ color: "#A8D5BA" }}>
                    💡 新手避坑
                  </p>
                  <p className="text-sm leading-relaxed" style={{ color: "#3B5B4B" }}>
                    {detailData.tip}
                  </p>
                </div>

                {/* Alternatives section in modal */}
                {items[detailId] === "dont-have" && detailItem && detailItem.alternatives.length > 0 && (
                  <div className="rounded-xl p-4" style={{ backgroundColor: "rgba(212,163,163,0.06)", border: "1px solid rgba(212,163,163,0.12)" }}>
                    <p className="text-[11px] tracking-widest uppercase mb-3" style={{ color: "#D4A3A3" }}>
                      🔄 替代方案（{detailItem.alternatives.length} 种选择）
                    </p>
                    <div className="space-y-2">
                      {detailItem.alternatives.map((alt, i) => (
                        <div
                          key={i}
                          className="flex items-start gap-3 p-3 rounded-lg"
                          style={{ backgroundColor: "rgba(255,255,255,0.7)" }}
                        >
                          <div
                            className="w-9 h-9 rounded-lg flex items-center justify-center text-base shrink-0"
                            style={{ backgroundColor: "rgba(212,163,163,0.08)" }}
                          >
                            {alt.emoji}
                          </div>
                          <div>
                            <p className="text-xs font-medium" style={{ color: "#5B4B4B" }}>{alt.label}</p>
                            <p className="text-[11px] leading-relaxed mt-0.5" style={{ color: "#8B7B7B" }}>{alt.desc}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </PhoneFrame>
  );
}
