import { Sparkles, Link2, Camera, ImagePlus, Check, Wand2 } from "lucide-react";
import { useRef } from "react";
import { PhoneFrame } from "./PhoneFrame";

interface Props {
  handImage: string | null;
  onHandChange: (url: string | null) => void;
  onNext: () => void;
}

export function HomeScreen({ handImage, onHandChange, onNext }: Props) {
  const fileRef = useRef<HTMLInputElement>(null);

  const handlePick = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (f) onHandChange(URL.createObjectURL(f));
  };

  const ready = !!handImage;

  return (
    <PhoneFrame>
      <div className="flex flex-col min-h-screen px-6 pt-14 pb-8">
        {/* Logo */}
        <div className="flex items-center gap-2">
          <div className="relative">
            <Sparkles className="w-5 h-5 text-brand animate-sparkle" strokeWidth={1.5} />
          </div>
          <h1 className="text-lg tracking-[0.3em] text-foreground font-light">指尖 SOP</h1>
        </div>
        <p className="mt-1.5 text-[10px] tracking-[0.25em] text-muted-foreground uppercase">
          Nail-It · AI Studio
        </p>

        {/* Step 1 — Link */}
        <div className="mt-7">
          <div className="flex items-center gap-2 mb-2.5">
            <span className="w-5 h-5 rounded-full cta-gradient text-white text-[10px] flex items-center justify-center font-medium">
              1
            </span>
            <p className="text-xs tracking-widest text-foreground/70">粘贴教程链接</p>
          </div>

          <div className="relative">
            <div className="animate-bob absolute -top-9 right-2 cta-gradient text-white text-[10px] px-3 py-1.5 rounded-full shadow-md flex items-center gap-1 whitespace-nowrap">
              <Link2 className="w-3 h-3" />
              检测到链接
            </div>

            <div className="relative overflow-hidden rounded-2xl border border-dashed border-brand/40 bg-white/70 backdrop-blur-sm px-5 py-5 shadow-[0_6px_24px_-12px_rgba(212,163,163,0.3)]">
              {/* shimmer sweep */}
              <div className="pointer-events-none absolute inset-0 overflow-hidden">
                <div className="absolute inset-y-0 -inset-x-1/2 w-1/3 bg-gradient-to-r from-transparent via-white/60 to-transparent animate-shimmer" />
              </div>
              <div className="relative flex items-center gap-3">
                <div className="w-9 h-9 rounded-full bg-brand/10 flex items-center justify-center shrink-0">
                  <Link2 className="w-4 h-4 text-brand" strokeWidth={1.5} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-[13px] text-foreground/80 truncate">
                    抖音 · 樱花猫眼美甲教学.mp4
                  </p>
                  <p className="text-[10px] text-muted-foreground mt-0.5 tracking-wider">
                    点击粘贴 · 或直接拖入
                  </p>
                </div>
                <Check className="w-4 h-4 text-success" strokeWidth={2} />
              </div>
            </div>
          </div>
        </div>

        {/* Step 2 — Hand upload */}
        <div className="mt-6 flex-1">
          <div className="flex items-center gap-2 mb-2.5">
            <span className={`w-5 h-5 rounded-full text-[10px] flex items-center justify-center font-medium transition ${ready ? "cta-gradient text-white" : "bg-foreground/10 text-foreground/50"}`}>
              2
            </span>
            <p className="text-xs tracking-widest text-foreground/70">上传你的手部照片</p>
          </div>

          <input ref={fileRef} type="file" accept="image/*" capture="environment" className="hidden" onChange={handlePick} />

          <button
            onClick={() => fileRef.current?.click()}
            className="relative w-full aspect-[5/3] rounded-2xl overflow-hidden group active:scale-[0.99] transition"
          >
            {handImage ? (
              <>
                <img src={handImage} alt="你的手" className="absolute inset-0 w-full h-full object-cover" />
                <div className="absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent" />
                <div className="absolute bottom-3 left-3 right-3 flex items-center justify-between">
                  <span className="text-white text-[11px] tracking-widest flex items-center gap-1.5">
                    <Check className="w-3.5 h-3.5" /> 手部已识别
                  </span>
                  <span className="text-white/80 text-[10px] underline underline-offset-2">重拍</span>
                </div>
                {/* scanning line */}
                <div className="absolute inset-x-0 top-0 h-[2px] bg-gradient-to-r from-transparent via-brand to-transparent animate-shimmer" />
              </>
            ) : (
              <div className="absolute inset-0 bg-gradient-to-br from-white to-brand/5 border-2 border-dashed border-brand/40 rounded-2xl flex flex-col items-center justify-center gap-2">
                {/* animated SVG dashed circle */}
                <div className="relative w-14 h-14 flex items-center justify-center">
                  <svg className="absolute inset-0 w-full h-full -rotate-90" viewBox="0 0 56 56">
                    <circle
                      cx="28" cy="28" r="26"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.2"
                      strokeDasharray="4 4"
                      className="text-brand/60 animate-dash"
                    />
                  </svg>
                  <span className="absolute inset-1 rounded-full cta-gradient opacity-20 animate-pulse-ring" />
                  <div className="relative w-10 h-10 rounded-full cta-gradient flex items-center justify-center shadow-md">
                    <Camera className="w-4 h-4 text-white" strokeWidth={1.8} />
                  </div>
                </div>
                <p className="text-[13px] text-foreground/80 tracking-wide">拍一张手部照片</p>
                <p className="text-[10px] text-muted-foreground tracking-wider flex items-center gap-1">
                  <ImagePlus className="w-3 h-3" />
                  或从相册选择 · AI 将贴合上手效果
                </p>
              </div>
            )}
          </button>
        </div>

        {/* CTA */}
        <button
          onClick={onNext}
          disabled={!ready}
          className={`mt-6 relative overflow-hidden w-full rounded-full py-4 text-white text-sm tracking-[0.25em] transition active:scale-[0.98] ${
            ready
              ? "cta-gradient shadow-[0_10px_30px_-10px_rgba(212,163,163,0.6)]"
              : "bg-foreground/20 cursor-not-allowed"
          }`}
        >
          {ready && (
            <span className="pointer-events-none absolute inset-y-0 -inset-x-1/2 w-1/3 bg-gradient-to-r from-transparent via-white/40 to-transparent animate-shimmer" />
          )}
          <span className="relative inline-flex items-center gap-2 justify-center">
            <Wand2 className="w-4 h-4" strokeWidth={1.6} />
            {ready ? "AI 生成上手效果" : "请先上传手部照片"}
          </span>
        </button>
        <p className="mt-3 text-center text-[10px] text-muted-foreground/60 tracking-widest">
          POWERED BY 指尖 AI
        </p>
      </div>
    </PhoneFrame>
  );
}
