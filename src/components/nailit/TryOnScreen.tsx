import { ChevronLeft, Hand } from "lucide-react";
import { useState } from "react";
import { PhoneFrame } from "./PhoneFrame";
import tryOnImg from "@/assets/nail-tryon.jpg";

export function TryOnScreen({ handImage, onBack, onConfirm }: { handImage?: string | null; onBack: () => void; onConfirm: () => void }) {
  const [comparing, setComparing] = useState(false);
  void handImage;


  return (
    <PhoneFrame>
      <div className="flex flex-col min-h-screen">
        {/* Top bar */}
        <div className="flex items-center justify-between px-5 pt-12 pb-4">
          <button onClick={onBack} className="w-9 h-9 rounded-full bg-white shadow-sm flex items-center justify-center active:scale-95 transition">
            <ChevronLeft className="w-5 h-5 text-foreground" strokeWidth={1.5} />
          </button>
          <h2 className="text-base tracking-[0.25em] font-light">AI 虚拟试戴</h2>
          <div className="w-9 h-9" />
        </div>

        {/* Hero image */}
        <div className="px-5">
          <div className="relative w-full rounded-[2rem] overflow-hidden shadow-[0_20px_50px_-20px_rgba(0,0,0,0.25)]" style={{ height: "65vh", maxHeight: 560 }}>
            <img src={tryOnImg} alt="虚拟试戴效果" className="w-full h-full object-cover" />
            {comparing && (
              <div className="absolute inset-0 bg-black/40 backdrop-grayscale flex items-center justify-center">
                <span className="text-white text-xs tracking-[0.3em] uppercase">Original</span>
              </div>
            )}
            <div className="absolute top-3 left-3 bg-white/85 backdrop-blur px-2.5 py-1 rounded-full text-[10px] tracking-widest text-foreground/80">
              · AI MATCH 98%
            </div>
          </div>

          {/* Compare button */}
          <button
            onPointerDown={() => setComparing(true)}
            onPointerUp={() => setComparing(false)}
            onPointerLeave={() => setComparing(false)}
            className="mt-4 mx-auto flex items-center gap-2 px-5 py-2 rounded-full bg-white/80 border border-border text-xs text-muted-foreground active:bg-white"
          >
            <Hand className="w-3.5 h-3.5" strokeWidth={1.5} />
            长按对比原手
          </button>
        </div>

        {/* Bottom actions */}
        <div className="mt-auto px-5 pb-8 pt-6 flex gap-3">
          <button
            onClick={onBack}
            className="flex-1 py-4 rounded-full border border-border bg-transparent text-muted-foreground text-sm tracking-widest active:scale-[0.98] transition"
          >
            不太搭，换一个
          </button>
          <button
            onClick={onConfirm}
            className="flex-1 py-4 rounded-full text-white text-sm tracking-widest shadow-[0_10px_30px_-10px_rgba(212,163,163,0.6)] active:scale-[0.98] transition"
            style={{ backgroundColor: "#D4A3A3" }}
          >
            绝美！看备料
          </button>
        </div>
      </div>
    </PhoneFrame>
  );
}
