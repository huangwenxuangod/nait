import { useEffect, useRef, useState } from "react";
import { PhoneFrame } from "./PhoneFrame";
import { AlertTriangle, Camera, Check, ChevronLeft, ChevronRight, Play, X } from "lucide-react";
import type { TutorialData } from "@/lib/types";
import { getTutorialVideo } from "@/lib/media";
import { generateVideo, shareToDouyin } from "@/lib/video-generator";

type SopStep = {
  step: number;
  total: number;
  title: string;
  instruction: string;
  detail: string;
  duration: number;
  isLampCure: boolean;
  originalQuote: string;
  aiTranslation: string;
  videoStart?: number;
};

const STEPS: SopStep[] = [
  {
    step: 1, total: 6, title: "修形打磨",
    instruction: "海绵锉单向修出方圆甲型",
    detail: "锉条与甲面呈 45°，单向向外打磨 3-5 下，避免来回拉锯损伤甲面",
    duration: 120, isLampCure: false,
    originalQuote: "先把指甲形状修一修就好了",
    aiTranslation: "锉条与甲面呈45°角，单向向外打磨，不要来回拉锯。甲缘留1-2mm白边保证受力均匀",
  },
  {
    step: 2, total: 6, title: "上底胶",
    instruction: "全甲均匀薄涂一层底胶",
    detail: "刷头蘸胶后在瓶口刮掉 2/3，从甲根推到甲尖，两侧留 0.5mm 缝隙包边",
    duration: 60, isLampCure: false, videoStart: 20,
    originalQuote: "薄薄涂一层底胶就行",
    aiTranslation: "刷头蘸胶后务必在瓶口刮掉2/3，从甲根匀速推至甲尖。⚠️ 两侧留0.5mm缝隙避免溢胶起翘",
  },
  {
    step: 3, total: 6, title: "照灯固化底胶",
    instruction: "LED 灯下固化 90 秒",
    detail: "手平放入灯内，四指并拢，中途不要移动或拿出查看",
    duration: 90, isLampCure: true, videoStart: 40,
    originalQuote: "照灯烤一下，干了就行",
    aiTranslation: "UV/LED灯固化至少90秒，手平放灯内不要动。⚠️ 未完全固化会导致甲面变软起皱",
  },
  {
    step: 4, total: 6, title: "上色胶",
    instruction: "刷头蘸取少量色胶薄涂两层",
    detail: "第一层半透明即可，照灯60s后上第二层达到饱满显色。避开甲缘皮肤",
    duration: 120, isLampCure: false, videoStart: 60,
    originalQuote: "上两遍颜色，涂匀一点",
    aiTranslation: "第一层薄涂（半透明即可）→照灯60s→第二层厚涂达到饱满。每层从甲根1mm处起笔，避开皮肤",
  },
  {
    step: 5, total: 6, title: "照灯 + 封层",
    instruction: "照灯90s后涂封层再照灯120s",
    detail: "封层需覆盖全甲面并包边，照灯至完全固化呈镜面光泽",
    duration: 120, isLampCure: true, videoStart: 85,
    originalQuote: "最后再照一下，涂个亮油封住",
    aiTranslation: "色胶照灯90s固化→涂封层覆盖全部甲面+包边→再照灯120s。⚠️ 封层未包边2-3天后会从指尖起翘",
  },
  {
    step: 6, total: 6, title: "精修检查",
    instruction: "清洁浮胶 + 指缘油收尾",
    detail: "棉片蘸酒精擦去表面浮胶，甲缘涂抹营养油按摩吸收",
    duration: 60, isLampCure: false, videoStart: 110,
    originalQuote: "最后擦一擦就好了",
    aiTranslation: "用清洁液/酒精擦去表面浮胶→检查甲面平整度→甲缘涂抹营养油。⚠️ 浮胶未擦干净会显得甲面发黏不光滑",
  },
];

type Phase = "learning" | "active" | "verifying" | "verified-ok" | "verified-fail" | "completed";

const VERIFY_MESSAGES_FAIL = [
  "甲面边缘检测到溢胶，请用棉签蘸酒精从甲缘外侧向内擦拭",
  "甲面颜色不均匀，左右深浅差异较大，建议薄涂再补一层",
  "检测到甲面有气泡/颗粒，可能是胶体太厚，建议擦掉重新薄涂",
  "包边不完整，指尖/两侧未完全覆盖，请用刷头补涂边缘",
];

export function FocusScreen({ onExit, missingItems, tutorialData }: { onExit: () => void; missingItems: string[]; tutorialData: TutorialData | null }) {
  const steps = tutorialData?.steps ?? STEPS;
  const [stepIdx, setStepIdx] = useState(0);
  const [seconds, setSeconds] = useState(steps[0].duration);
  const [phase, setPhase] = useState<Phase>("learning");
  const [showMissingBar, setShowMissingBar] = useState(missingItems.length > 0);
  const [verifyMsg, setVerifyMsg] = useState("");
  const [showFinalVideo, setShowFinalVideo] = useState(false);
  const [videoGenerating, setVideoGenerating] = useState(false);

  const holdRef = useRef<number | null>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const videoUrl = getTutorialVideo();
  const step = steps[stepIdx];
  const isLast = stepIdx === steps.length - 1;

  // Countdown
  useEffect(() => {
    if (phase !== "active") return;
    const id = window.setInterval(() => {
      setSeconds((s) => (s > 0 ? s - 1 : 0));
    }, 1000);
    return () => clearInterval(id);
  }, [phase, stepIdx]);

  // Reset timer on step change
  useEffect(() => {
    setSeconds(steps[stepIdx].duration);
    setPhase("learning");
    setVerifyMsg("");
  }, [stepIdx]);

  // Long press to exit (background)
  const startExitHold = () => {
    holdRef.current = window.setTimeout(() => onExit(), 800);
  };
  const cancelExitHold = () => {
    if (holdRef.current) { clearTimeout(holdRef.current); holdRef.current = null; }
  };

  // Seek video to step's start time when step changes
  useEffect(() => {
    if (videoRef.current && step.videoStart != null) {
      videoRef.current.currentTime = step.videoStart;
    }
  }, [stepIdx, step.videoStart]);

  // Countdown
  const timerColor = step.isLampCure ? "#F5C542" : "#D4A3A3";
  const timerLabel = step.isLampCure ? "🔆 烤灯固化中" : "固化中";

  // Verification
  const startVerification = () => {
    setPhase("verifying");
    window.setTimeout(() => {
      setPhase("verified-ok");
    }, 1800);
  };

  const goNext = () => {
    if (isLast) {
      setPhase("completed");
    } else {
      setStepIdx((i) => i + 1);
    }
  };

  const goPrev = () => {
    if (stepIdx > 0) setStepIdx((i) => i - 1);
  };

  const handleGenerateVideo = async () => {
    if (!tutorialData) return;
    setVideoGenerating(true);
    await generateVideo({
      data: tutorialData,
      stepImages: [],
    });
    setVideoGenerating(false);
    setShowFinalVideo(true);
  };

  return (
    <PhoneFrame>
      {/* Full-screen interaction layer */}
      <div
        className="relative flex flex-col min-h-screen select-none"
        onPointerDown={startExitHold}
        onPointerUp={cancelExitHold}
        onPointerLeave={cancelExitHold}
      >
        {/* Missing items bar */}
        {missingItems.length > 0 && (
          <div className="absolute top-0 left-0 right-0 z-20">
            <button
              onClick={() => setShowMissingBar(!showMissingBar)}
              className="w-full flex items-center justify-between px-6 pt-12 pb-2 text-[10px] tracking-wider"
              style={{ color: "#F5C542" }}
            >
              <span className="flex items-center gap-1.5">
                <AlertTriangle className="w-3 h-3" />
                <span>替代方案已就绪 · {missingItems.length} 项物料需注意</span>
              </span>
              <span className="text-[9px] opacity-70">{showMissingBar ? "收起" : "展开"}</span>
            </button>
            {showMissingBar && (
              <div className="mx-5 mb-2 p-3 rounded-xl text-[10px] leading-relaxed" style={{ backgroundColor: "rgba(245,197,66,0.1)", color: "#F5E0A0" }}>
                <p className="opacity-70 mb-1">以下物料已记录替代方案，制作时请留意：</p>
                {missingItems.map((id) => {
                  let label = id;
                  for (const sec of [
                    { items: [{ id: "base", label: "底胶" }, { id: "lamp", label: "烤灯" }, { id: "top", label: "封层" }, { id: "file", label: "搓条" }, { id: "cleaner", label: "清洁液" }] },
                    { items: [{ id: "rose", label: "玫瑰豆沙胶" }, { id: "aurora", label: "极光粉" }] },
                  ]) {
                    const found = sec.items.find((x: { id: string; label: string }) => x.id === id);
                    if (found) label = found.label;
                  }
                  return (
                    <div key={id} className="flex items-center gap-1.5">
                      <span style={{ color: "#F5C542" }}>·</span>
                      <span>{label} — 已记录替代方案</span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* Top status */}
        <div className="flex items-center justify-between px-6 pt-12 pb-2" style={missingItems.length > 0 ? { paddingTop: showMissingBar ? "6rem" : "4rem" } : undefined}>
          <button onClick={onExit} className="flex items-center gap-1 text-[10px] tracking-wider text-muted-foreground/60 uppercase hover:text-brand transition">
            <ChevronLeft className="w-3.5 h-3.5" />
            退出
          </button>
          <span className="text-[10px] tracking-[0.3em] text-muted-foreground/60 uppercase">
            Step {String(step.step).padStart(2, "0")} / {String(steps.length).padStart(2, "0")}
          </span>
        </div>

        {/* Step progress dots */}
        <div className="px-8 flex items-center justify-center gap-1.5 mt-1">
          {steps.map((s, i) => (
            <div key={i} className="flex items-center">
              <div
                className="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-medium transition"
                style={{
                  backgroundColor: i < stepIdx ? "#A8D5BA" : i === stepIdx ? "#D4A3A3" : "var(--color-muted)",
                  color: i <= stepIdx ? "#fff" : "var(--color-muted-foreground)",
                }}
              >
                {i < stepIdx ? <Check className="w-3 h-3" strokeWidth={3} /> : s.step}
              </div>
              {i < steps.length - 1 && (
                <div
                  className="w-4 h-px mx-0.5 transition"
                  style={{ backgroundColor: i < stepIdx ? "#A8D5BA" : "var(--color-border)" }}
                />
              )}
            </div>
          ))}
        </div>

        {/* Step video / image */}
        <div className="mt-2 mx-5 rounded-[2rem] overflow-hidden" style={{ height: "45vh", backgroundColor: "#000" }}>
          {videoUrl ? (
            <video
              ref={videoRef}
              src={videoUrl}
              className="w-full h-full object-contain"
              playsInline
              controls
              preload="auto"
            />
          ) : (
            <div className="w-full h-full flex flex-col items-center justify-center text-white/20">
              <Play className="w-10 h-10 mb-2" style={{ color: "#D4A3A3" }} />
              <p className="text-xs tracking-wider">将教程视频放入 public/ 目录</p>
            </div>
          )}
        </div>

        {/* Main instruction area */}
        <div className="px-7 mt-4 flex-1 flex flex-col">
          <p className="text-xs tracking-[0.3em] text-brand uppercase">Step {String(step.step).padStart(2, "0")} · {step.title}</p>
          <h2 className="mt-2 text-3xl font-light leading-snug text-foreground">{step.instruction}</h2>
          <p className="mt-2 text-sm text-muted-foreground leading-relaxed">{step.detail}</p>

          {/* AI Tip Bubble */}
          <div
            className="mt-4 rounded-2xl px-4 py-3"
            style={{ backgroundColor: "rgba(168,213,186,0.08)", border: "1px solid rgba(168,213,186,0.2)" }}
          >
            <div className="flex items-center gap-1.5 mb-2 text-[10px] tracking-widest text-success">
              <span>✨</span>
              <span>AI 解读</span>
            </div>
            <p className="text-sm text-foreground/80 leading-relaxed">{step.aiTranslation}</p>
          </div>

          {/* Spacer */}
          <div className="flex-1" />

          {/* Bottom: Timer + Voice + Actions */}
          <div className="pb-4">
            {/* Verification panel */}
            {phase === "verifying" && (
              <div className="mb-4 rounded-2xl px-4 py-4 flex flex-col items-center gap-3" style={{ backgroundColor: "rgba(245,197,66,0.08)", border: "1px solid rgba(245,197,66,0.2)" }}>
                <div className="w-8 h-8 border-2 rounded-full animate-spin" style={{ borderColor: "rgba(245,197,66,0.3)", borderTopColor: "#F5C542" }} />
                <p className="text-xs tracking-wider" style={{ color: "#F5C542" }}>AI 正在分析全方位照...</p>
                <p className="text-[10px] text-muted-foreground">请稍候 · 大约 2 秒</p>
              </div>
            )}

            {phase === "verified-ok" && (
              <div className="mb-4 rounded-2xl px-4 py-4 flex flex-col items-center gap-2" style={{ backgroundColor: "rgba(168,213,186,0.08)", border: "1px solid rgba(168,213,186,0.2)" }}>
                <div className="w-10 h-10 rounded-full flex items-center justify-center" style={{ backgroundColor: "#A8D5BA" }}>
                  <Check className="w-6 h-6 text-white" strokeWidth={3} />
                </div>
                <p className="text-sm tracking-wider" style={{ color: "#A8D5BA" }}>核验通过 · 做的很棒!</p>
                <button
                  onClick={goNext}
                  className="mt-2 px-8 py-2.5 rounded-full text-xs font-medium tracking-widest text-white transition active:scale-95"
                  style={{ backgroundColor: "#A8D5BA" }}
                >
                  {isLast ? "全部完成 🎉" : "进入下一步 →"}
                </button>
              </div>
            )}

            {phase === "verified-fail" && (
              <div className="mb-4 rounded-2xl px-4 py-4 flex flex-col items-center gap-2" style={{ backgroundColor: "rgba(212,163,163,0.1)", border: "1px solid rgba(212,163,163,0.25)" }}>
                <div className="w-10 h-10 rounded-full flex items-center justify-center" style={{ backgroundColor: "#D4A3A3" }}>
                  <X className="w-6 h-6 text-white" strokeWidth={3} />
                </div>
                <p className="text-sm tracking-wider" style={{ color: "#D4A3A3" }}>检测到需要调整</p>
                <p className="text-xs text-center leading-relaxed px-2 text-foreground/70">{verifyMsg}</p>
                <div className="flex gap-3 mt-2">
                  <button
                    onClick={() => setPhase("active")}
                    className="px-6 py-2 rounded-full text-xs tracking-wider transition active:scale-95"
                    style={{ backgroundColor: "rgba(255,255,255,0.1)", color: "rgba(255,255,255,0.6)" }}
                  >
                    调整后重试
                  </button>
                  <button
                    onClick={() => setPhase("active")}
                    className="px-6 py-2 rounded-full text-xs tracking-wider transition active:scale-95"
                    style={{ backgroundColor: "#D4A3A3", color: "#fff" }}
                  >
                    重新拍照
                  </button>
                </div>
              </div>
            )}

            {/* Timer + Voice + Verify row */}
            {(phase === "active" || phase === "learning") && (
              <div className="flex items-center justify-between mb-3 gap-2">
                {/* Countdown */}
                <div className="relative w-20 h-20 shrink-0">
                  <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
                    <circle cx="50" cy="50" r={32} stroke="var(--color-border)" strokeWidth="4" fill="none" />
                    <circle
                      cx="50" cy="50" r={32}
                      stroke={timerColor} strokeWidth="4" fill="none"
                      strokeLinecap="round"
                      strokeDasharray={2 * Math.PI * 32}
                      strokeDashoffset={phase === "learning" ? 0 : 2 * Math.PI * 32 * (1 - seconds / step.duration)}
                      style={{ transition: "stroke-dashoffset 1s linear" }}
                    />
                  </svg>
                  <div className="absolute inset-0 flex flex-col items-center justify-center">
                    <span className="text-lg font-light tabular-nums text-foreground">{seconds}<span className="text-[10px] text-muted-foreground">s</span></span>
                    <span className="text-[7px] tracking-[0.15em] text-muted-foreground/60 uppercase mt-0.5">
                      {phase === "learning" ? "准备好了？" : timerLabel}
                    </span>
                  </div>
                </div>

                {/* Voice indicator */}
                <div className="flex flex-col items-center gap-1 shrink-0">
                  <div className="flex items-end gap-[2px] h-4">
                    <span className="w-[2px] h-full bg-[#A8D5BA] rounded-full animate-wave" style={{ animationDelay: "0ms" }} />
                    <span className="w-[2px] h-full bg-[#A8D5BA] rounded-full animate-wave" style={{ animationDelay: "150ms" }} />
                    <span className="w-[2px] h-full bg-[#A8D5BA] rounded-full animate-wave" style={{ animationDelay: "300ms" }} />
                    <span className="w-[2px] h-full bg-[#A8D5BA] rounded-full animate-wave" style={{ animationDelay: "150ms" }} />
                  </div>
                  <span className="text-[9px] tracking-widest text-muted-foreground">语音助手</span>
                </div>

                {/* Verify / Start button */}
                {phase === "learning" ? (
                  <button
                    onClick={() => setPhase("active")}
                    className="flex items-center gap-1.5 px-5 py-3 rounded-full text-[11px] font-medium tracking-widest text-white transition active:scale-95 shadow-[0_4px_20px_-6px_rgba(168,213,186,0.5)] shrink-0"
                    style={{ backgroundColor: "#A8D5BA" }}
                  >
                    <Play className="w-3.5 h-3.5" />
                    开始操作
                  </button>
                ) : (
                  <button
                    onClick={startVerification}
                    className={`flex items-center gap-1.5 px-4 py-3 rounded-full text-[11px] font-medium tracking-widest transition active:scale-95 shrink-0 ${
                      seconds === 0
                        ? "text-white shadow-[0_4px_20px_-6px_rgba(168,213,186,0.5)]"
                        : "opacity-40"
                    }`}
                    style={seconds === 0 ? { backgroundColor: "#A8D5BA" } : { backgroundColor: "rgba(255,255,255,0.08)" }}
                  >
                    <Camera className="w-3.5 h-3.5" />
                    <span>{seconds === 0 ? "完成 拍照核验" : "拍照核验"}</span>
                  </button>
                )}
              </div>
            )}

            {/* Step navigation */}
            <div className="flex items-center justify-between px-2">
              <button
                onClick={goPrev}
                disabled={stepIdx === 0}
                className="flex items-center gap-1 px-4 py-2 rounded-full text-[11px] tracking-wider transition active:scale-95 disabled:opacity-25 text-muted-foreground"
              >
                <ChevronLeft className="w-3.5 h-3.5" />
                上一步
              </button>
              <p className="text-center text-[10px] tracking-[0.3em] text-muted-foreground/40 uppercase">
                长按退出
              </p>
              <button
                onClick={goNext}
                className="flex items-center gap-1 px-4 py-2 rounded-full text-[11px] tracking-wider transition active:scale-95 text-muted-foreground"
              >
                {isLast ? "完成" : "跳过核验 →"}
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        {/* Completed screen */}
        {phase === "completed" && !showFinalVideo && (
          <div className="absolute inset-0 z-30 flex flex-col items-center justify-center px-8" style={{ backgroundColor: "rgba(26,26,26,0.95)" }}>
            <div className="w-20 h-20 rounded-full flex items-center justify-center mb-6" style={{ backgroundColor: "#A8D5BA" }}>
              <Check className="w-10 h-10 text-white" strokeWidth={3} />
            </div>
            <h2 className="text-2xl font-light tracking-wide mb-2">全部步骤完成!</h2>
            <p className="text-sm text-white/50 mb-10 text-center leading-relaxed">
              恭喜你完成了第一款 AI 辅助美甲<br />
              现在可以生成你的 DIY 视频了
            </p>

            {videoGenerating ? (
              <div className="flex flex-col items-center gap-4">
                <div className="w-12 h-12 border-2 rounded-full animate-spin" style={{ borderColor: "rgba(212,163,163,0.2)", borderTopColor: "#D4A3A3" }} />
                <p className="text-xs tracking-wider" style={{ color: "#D4A3A3" }}>AI 正在生成你的 DIY 视频...</p>
                <p className="text-[10px] text-white/30">正在剪辑步骤 + 配乐 + 字幕</p>
              </div>
            ) : (
              <button
                onClick={handleGenerateVideo}
                className="cta-gradient text-white px-10 py-4 rounded-full text-sm tracking-[0.25em] shadow-[0_10px_30px_-10px_rgba(212,163,163,0.6)] active:scale-[0.98] transition"
              >
                🎬 生成我的 DIY 视频
              </button>
            )}

            <button onClick={onExit} className="mt-8 text-xs tracking-wider text-white/30">返回首页</button>
          </div>
        )}

        {/* Video preview modal */}
        {showFinalVideo && (
          <div className="absolute inset-0 z-40 flex flex-col items-center justify-center px-6" style={{ backgroundColor: "rgba(0,0,0,0.95)" }}>
            <div className="w-full aspect-[9/16] max-w-[320px] rounded-2xl overflow-hidden mb-6 flex items-center justify-center" style={{ backgroundColor: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)" }}>
              <div className="flex flex-col items-center gap-3 text-white/40">
                <Play className="w-12 h-12" style={{ color: "#D4A3A3" }} />
                <p className="text-xs tracking-wider">你的 DIY 美甲视频</p>
                <p className="text-[10px] text-white/20">指尖 SOP · Step 1-6 精选片段</p>
                <div className="flex items-center gap-2 mt-2">
                  <span className="text-[9px] px-2 py-0.5 rounded-full" style={{ backgroundColor: "rgba(212,163,163,0.15)", color: "#D4A3A3" }}>#美甲DIY</span>
                  <span className="text-[9px] px-2 py-0.5 rounded-full" style={{ backgroundColor: "rgba(212,163,163,0.15)", color: "#D4A3A3" }}>#指尖SOP</span>
                  <span className="text-[9px] px-2 py-0.5 rounded-full" style={{ backgroundColor: "rgba(212,163,163,0.15)", color: "#D4A3A3" }}>#沉浸式</span>
                </div>
              </div>
            </div>
            <p className="text-sm font-light tracking-wide mb-2">视频已生成</p>
            <p className="text-[10px] text-white/30 mb-8">一键转发到抖音 / 小红书 / 微信</p>
            <div className="flex gap-4">
              <button
                onClick={() => { setShowFinalVideo(false); onExit(); }}
                className="px-6 py-3 rounded-full text-xs tracking-wider transition active:scale-95"
                style={{ backgroundColor: "rgba(255,255,255,0.08)", color: "rgba(255,255,255,0.5)" }}
              >
                稍后再说
              </button>
              <button
                onClick={() => { shareToDouyin(); }}
                className="px-8 py-3 rounded-full text-xs tracking-wider text-white shadow-[0_4px_20px_-6px_rgba(212,163,163,0.5)] active:scale-95 transition"
                style={{ backgroundColor: "#FF004F" }}
              >
               一键转发到抖音
              </button>
            </div>
          </div>
        )}

      </div>
    </PhoneFrame>
  );
}
