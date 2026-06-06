import { Sparkles, Link2 } from "lucide-react";
import { PhoneFrame } from "./PhoneFrame";

export function HomeScreen({ onNext }: { onNext: () => void }) {
  return (
    <PhoneFrame>
      <div className="flex flex-col min-h-screen px-7 pt-16 pb-10">
        {/* Logo */}
        <div className="flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-brand" strokeWidth={1.5} />
          <h1 className="text-lg tracking-[0.3em] text-foreground font-light">指尖 SOP</h1>
        </div>
        <p className="mt-2 text-xs tracking-widest text-muted-foreground uppercase">Nail-It · AI Studio</p>

        {/* Middle */}
        <div className="flex-1 flex flex-col justify-center">
          <div className="relative">
            {/* floating tooltip */}
            <div className="animate-bob absolute -top-12 left-1/2 -translate-x-1/2 cta-gradient text-white text-xs px-4 py-2 rounded-full shadow-md flex items-center gap-1.5 whitespace-nowrap">
              <Link2 className="w-3.5 h-3.5" />
              检测到链接，点击一键粘贴
              <span className="absolute left-1/2 -translate-x-1/2 -bottom-1 w-2 h-2 rotate-45 cta-gradient" />
            </div>

            <div className="rounded-3xl border-2 border-dashed border-brand/50 bg-white/60 backdrop-blur-sm shadow-[0_8px_30px_-12px_rgba(212,163,163,0.25)] px-6 py-10 min-h-[180px] flex items-center justify-center text-center">
              <p className="text-sm text-muted-foreground leading-relaxed">
                在此粘贴
                <span className="text-foreground/80"> 抖音 / 小红书 </span>
                <br />
                美甲教程链接
              </p>
            </div>
          </div>

          <p className="mt-6 text-center text-[11px] text-muted-foreground/70 tracking-wider">
            AI 将为你解析款式 · 备料 · 步骤
          </p>
        </div>

        {/* CTA */}
        <button
          onClick={onNext}
          className="cta-gradient w-full rounded-full py-4 text-white text-sm tracking-[0.25em] shadow-[0_10px_30px_-10px_rgba(212,163,163,0.6)] active:scale-[0.98] transition"
        >
          一键提取款式与步骤
        </button>
        <p className="mt-4 text-center text-[10px] text-muted-foreground/60 tracking-widest">
          POWERED BY 指尖 AI
        </p>
      </div>
    </PhoneFrame>
  );
}
