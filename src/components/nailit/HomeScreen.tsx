import { Sparkles, Link2, History, FileText, ListChecks, ScanLine, Play, Flame, ChevronRight, ChevronLeft } from "lucide-react";
import { useRef, useState } from "react";
import { PhoneFrame } from "./PhoneFrame";
import tutorialCover from "@/assets/tutorial-cover.jpg";
import insp1 from "@/assets/insp-1.jpg";
import insp2 from "@/assets/insp-2.jpg";
import insp3 from "@/assets/insp-3.jpg";
import insp4 from "@/assets/insp-4.jpg";
import insp5 from "@/assets/insp-5.jpg";

interface Props {
  handImage: string | null;
  onHandChange: (url: string | null) => void;
  onNext: () => void;
}

const steps = ["修剪", "底油", "颜色", "封层", "亮油", "完成"];
const inspirations = [
  { img: insp1, name: "温柔裸粉法式", count: "1.2w 人解析" },
  { img: insp2, name: "焦糖琥珀晕染", count: "8567 人解析" },
  { img: insp3, name: "透亮冰透款", count: "1.1w 人解析" },
  { img: insp4, name: "清新花卉款", count: "9234 人解析" },
  { img: insp5, name: "气质豆沙款", count: "7643 人解析" },
];

export function HomeScreen({ onNext }: Props) {
  const [link, setLink] = useState("");

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) setLink(text);
    } catch {
      setLink("https://v.douyin.com/example-nail-tutorial");
    }
    setTimeout(onNext, 350);
  };

  return (
    <PhoneFrame>
      <div className="flex flex-col min-h-screen pb-24">
        {/* Header */}
        <div className="px-6 pt-12 flex items-start justify-between">
          <div>
            <h1 className="text-[32px] font-semibold tracking-tight text-foreground flex items-center gap-2">
              指尖 SOP
              <Sparkles className="w-4 h-4 text-brand animate-sparkle" strokeWidth={2} />
            </h1>
            <p className="mt-1 text-[13px] text-muted-foreground tracking-wide">AI 美甲步骤生成器</p>
          </div>
          <button className="flex flex-col items-center gap-1 mt-2">
            <div className="w-10 h-10 rounded-full bg-white shadow-[0_2px_10px_rgba(0,0,0,0.04)] border border-border/60 flex items-center justify-center">
              <History className="w-4 h-4 text-foreground/70" strokeWidth={1.8} />
            </div>
            <span className="text-[10px] text-muted-foreground tracking-wider">历史记录</span>
          </button>
        </div>

        {/* Paste input */}
        <div className="px-5 mt-6">
          <div className="relative bg-white rounded-2xl border border-brand/30 shadow-[0_4px_20px_-8px_rgba(212,163,163,0.25)] p-2 flex items-center gap-2">
            <div className="pl-3 flex items-center gap-2 flex-1 min-w-0">
              <Link2 className="w-4 h-4 text-brand shrink-0" strokeWidth={2} />
              <input
                value={link}
                onChange={(e) => setLink(e.target.value)}
                placeholder="粘贴美甲教程链接(抖音/小红书/YouTube)"
                className="flex-1 min-w-0 bg-transparent text-[12px] placeholder:text-muted-foreground/70 focus:outline-none py-2"
              />
            </div>
            <button
              onClick={handlePaste}
              className="cta-gradient text-white text-sm px-5 py-2.5 rounded-xl shadow-sm active:scale-[0.97] transition"
            >
              粘贴
            </button>
            {/* shimmer */}
            <div className="pointer-events-none absolute inset-0 overflow-hidden rounded-2xl">
              <div className="absolute inset-y-0 -inset-x-1/2 w-1/3 bg-gradient-to-r from-transparent via-brand/10 to-transparent animate-shimmer" />
            </div>
          </div>
          <p className="mt-2.5 px-1 text-[11px] text-muted-foreground tracking-wide flex items-center gap-1">
            支持抖音、小红书、YouTube 等平台链接
            <span className="w-3.5 h-3.5 rounded-full border border-muted-foreground/40 inline-flex items-center justify-center text-[8px]">i</span>
          </p>
        </div>

        {/* Stats */}
        <div className="px-5 mt-5">
          <div className="flex items-center justify-between">
            <Stat icon={<FileText className="w-4 h-4" />} label="12K+ 教程解析" tint="rose" />
            <div className="w-px h-5 bg-border" />
            <Stat icon={<ListChecks className="w-4 h-4" />} label="6 步拆解" tint="lavender" />
            <div className="w-px h-5 bg-border" />
            <Stat icon={<ScanLine className="w-4 h-4" />} label="AI 智能识别" tint="mint" />
          </div>
        </div>

        {/* Example card */}
        <div className="px-5 mt-5">
          <div className="bg-white rounded-3xl p-4 shadow-[0_4px_24px_-12px_rgba(0,0,0,0.08)] border border-border/60">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-brand" strokeWidth={2} />
                <span className="text-[13px] font-medium text-foreground">教程解析示例</span>
              </div>
              <button className="text-[11px] text-muted-foreground flex items-center gap-0.5">
                查看全部 <ChevronRight className="w-3 h-3" />
              </button>
            </div>

            {/* Step pills */}
            <div className="flex items-center gap-1 mb-3 overflow-x-auto -mx-1 px-1 no-scrollbar">
              {steps.map((s, i) => (
                <div key={s} className="flex items-center gap-1 shrink-0">
                  <span
                    className={`w-5 h-5 rounded-full text-[10px] flex items-center justify-center font-medium ${
                      i === 0 ? "cta-gradient text-white" : "bg-muted text-foreground/60"
                    }`}
                  >
                    {i + 1}
                  </span>
                  <span className={`text-[11px] ${i === 0 ? "text-foreground" : "text-foreground/60"}`}>{s}</span>
                  {i < steps.length - 1 && <span className="text-muted-foreground/40 text-[10px] mx-0.5">···</span>}
                </div>
              ))}
            </div>

            {/* Cover */}
            <button onClick={onNext} className="relative w-full aspect-[16/10] rounded-2xl overflow-hidden group active:scale-[0.99] transition">
              <img src={tutorialCover} alt="教程示例" className="absolute inset-0 w-full h-full object-cover" loading="lazy" width={1024} height={1024} />
              <div className="absolute inset-0 bg-gradient-to-t from-black/10 to-transparent" />
              <span className="absolute inset-0 flex items-center justify-center">
                <span className="w-12 h-12 rounded-full bg-white/85 backdrop-blur flex items-center justify-center shadow-lg">
                  <Play className="w-5 h-5 text-foreground fill-foreground ml-0.5" />
                </span>
              </span>
            </button>

            <p className="mt-3 text-[13px] text-foreground font-medium">在此粘贴 抖音 / 小红书 / YouTube 美甲教程链接</p>
            <p className="mt-1 text-[11px] text-muted-foreground">支持链接自动识别,生成步骤拆解</p>

            {/* Platform chips */}
            <div className="mt-3 flex items-center gap-2 flex-wrap">
              <Platform label="抖音" color="#000" />
              <Platform label="小红书" color="#FF2741" />
              <Platform label="YouTube" color="#FF0000" />
              <span className="ml-auto text-[11px] text-muted-foreground flex items-center gap-1">
                <Link2 className="w-3 h-3" /> 支持长链接
              </span>
            </div>
          </div>
        </div>

        {/* Inspirations */}
        <div className="px-5 mt-5">
          <div className="bg-white rounded-3xl p-4 shadow-[0_4px_24px_-12px_rgba(0,0,0,0.08)] border border-border/60">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-1.5">
                <Flame className="w-4 h-4 text-brand" strokeWidth={2} />
                <span className="text-[13px] font-medium text-foreground">热门美甲灵感</span>
              </div>
              <button className="text-[11px] text-muted-foreground flex items-center gap-0.5">
                更多灵感 <ChevronRight className="w-3 h-3" />
              </button>
            </div>
            <div className="flex gap-2.5 overflow-x-auto -mx-1 px-1 pb-1 no-scrollbar">
              {inspirations.map((i) => (
                <button key={i.name} onClick={onNext} className="shrink-0 w-[88px] text-left active:scale-[0.97] transition">
                  <div className="w-[88px] h-[88px] rounded-2xl overflow-hidden bg-muted">
                    <img src={i.img} alt={i.name} loading="lazy" width={512} height={512} className="w-full h-full object-cover" />
                  </div>
                  <p className="mt-1.5 text-[12px] text-foreground truncate">{i.name}</p>
                  <p className="text-[10px] text-muted-foreground">{i.count}</p>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Bottom tab bar */}
      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[430px] bg-white/95 backdrop-blur-md border-t border-border/60 pt-2 pb-6 px-8 flex items-center justify-between">
        <TabItem icon={<HomeIcon />} label="首页" active />
        <TabItem icon={<Sparkles className="w-5 h-5" strokeWidth={1.6} />} label="灵感" />
        <TabItem icon={<UserIcon />} label="我的" />
      </div>
    </PhoneFrame>
  );
}

function Stat({ icon, label, tint }: { icon: React.ReactNode; label: string; tint: "rose" | "lavender" | "mint" }) {
  const tints = {
    rose: "bg-[#FBE9E9] text-[#C97B7B]",
    lavender: "bg-[#ECE9F7] text-[#7A6FB5]",
    mint: "bg-[#E3F1E8] text-[#5A9B7A]",
  } as const;
  return (
    <div className="flex items-center gap-1.5">
      <span className={`w-6 h-6 rounded-md flex items-center justify-center ${tints[tint]}`}>{icon}</span>
      <span className="text-[11px] text-foreground/80 tracking-wide">{label}</span>
    </div>
  );
}

function Platform({ label, color }: { label: string; color: string }) {
  return (
    <div className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg border border-border bg-white">
      <span className="w-3.5 h-3.5 rounded-sm" style={{ backgroundColor: color }} />
      <span className="text-[11px] text-foreground/80">{label}</span>
    </div>
  );
}

function TabItem({ icon, label, active = false }: { icon: React.ReactNode; label: string; active?: boolean }) {
  return (
    <button className={`flex flex-col items-center gap-0.5 ${active ? "text-brand" : "text-foreground/40"}`}>
      {icon}
      <span className="text-[10px] tracking-wider">{label}</span>
    </button>
  );
}

function HomeIcon() {
  return (
    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 11l9-8 9 8" /><path d="M5 10v10h14V10" />
    </svg>
  );
}
function UserIcon() {
  return (
    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="8" r="4" /><path d="M4 21c1.5-4 5-6 8-6s6.5 2 8 6" />
    </svg>
  );
}
