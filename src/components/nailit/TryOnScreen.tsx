import { ChevronLeft, Hand, Link2, Play } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { PhoneFrame } from "./PhoneFrame";
import tryOnImg from "@/assets/nail-tryon.jpg";
import type { TutorialData } from "@/lib/types";
import { getTutorialVideo } from "@/lib/media";

export function TryOnScreen({ handImage, onBack, onConfirm, tutorialData }: { handImage?: string | null; onBack: () => void; onConfirm: () => void; tutorialData: TutorialData | null }) {
  const [comparing, setComparing] = useState(false);
  const [showVideo, setShowVideo] = useState(false);
  const videoUrl = getTutorialVideo();
  const videoRef = useRef<HTMLVideoElement>(null);
  void handImage;

  useEffect(() => {
    if (showVideo && videoRef.current) {
      videoRef.current.currentTime = 0;
      videoRef.current.play().catch(() => {});
    }
  }, [showVideo]);

  return (
    <PhoneFrame>
      <div className="flex flex-col min-h-screen">
        {/* Top bar */}
        <div className="flex items-center justify-between px-5 pt-12 pb-4">
          <button onClick={onBack} className="w-9 h-9 rounded-full bg-white shadow-sm flex items-center justify-center active:scale-95 transition">
            <ChevronLeft className="w-5 h-5 text-foreground" strokeWidth={1.5} />
          </button>
          <h2 className="text-base tracking-[0.25em] font-light">
            {showVideo ? "原视频" : "AI 虚拟试戴"}
          </h2>
          <div className="w-9 h-9" />
        </div>

        {/* Hero */}
        <div className="px-5">
          <div className="relative w-full rounded-[2rem] overflow-hidden shadow-[0_20px_50px_-20px_rgba(0,0,0,0.25)]" style={{ height: "65vh", maxHeight: 560, backgroundColor: "#1A1A1A" }}>
            {showVideo && videoUrl ? (
              <video
                ref={videoRef}
                src={videoUrl}
                className="absolute inset-0 w-full h-full object-cover"
                controls
                playsInline
                preload="metadata"
              />
            ) : (
              <>
                <img src={tryOnImg} alt="虚拟试戴效果" className="w-full h-full object-cover" />
                {comparing && (
                  handImage ? (
                    <img src={handImage} alt="原手" className="absolute inset-0 w-full h-full object-cover" />
                  ) : (
                    <div className="absolute inset-0 bg-black/40 backdrop-grayscale flex items-center justify-center">
                      <span className="text-white text-xs tracking-[0.3em] uppercase">Original</span>
                    </div>
                  )
                )}
                <div className="absolute top-3 left-3 bg-white/85 backdrop-blur px-2.5 py-1 rounded-full text-[10px] tracking-widest text-foreground/80">
                  · AI MATCH 98%
                </div>
              </>
            )}
          </div>

          {/* Toggle: Video / TryOn */}
          {videoUrl && (
            <button
              onClick={() => setShowVideo(!showVideo)}
              className="mt-3 mx-auto flex items-center gap-2 px-5 py-2 rounded-full bg-white/80 border border-border text-xs text-muted-foreground active:bg-white transition"
            >
              <Play className="w-3.5 h-3.5" fill={showVideo ? "currentColor" : "none"} strokeWidth={1.5} />
              {showVideo ? "查看 AI 试戴效果" : "查看原视频"}
            </button>
          )}

          {/* Compare button — only when not showing video */}
          {!showVideo && (
            <button
              onPointerDown={() => setComparing(true)}
              onPointerUp={() => setComparing(false)}
              onPointerLeave={() => setComparing(false)}
              className="mt-3 mx-auto flex items-center gap-2 px-5 py-2 rounded-full bg-white/80 border border-border text-xs text-muted-foreground active:bg-white"
            >
              <Hand className="w-3.5 h-3.5" strokeWidth={1.5} />
              长按对比原手
            </button>
          )}

          {/* Tutorial info from parsed link */}
          {tutorialData && (
            <div className="mt-5 mx-1 rounded-2xl px-4 py-3.5" style={{ backgroundColor: "rgba(168,213,186,0.08)", border: "1px solid rgba(168,213,186,0.15)" }}>
              <div className="flex items-center gap-1.5 mb-2">
                <Link2 className="w-3.5 h-3.5" style={{ color: "#A8D5BA" }} />
                <span className="text-[11px] tracking-wider" style={{ color: "#A8D5BA" }}>AI 已解析 · {tutorialData.videoTitle}</span>
              </div>
              <p className="text-xs leading-relaxed" style={{ color: "#5B7B6B" }}>{tutorialData.styleDescription}</p>
              <div className="flex items-center gap-2 mt-2.5">
                <span className="px-2 py-0.5 rounded-full text-[9px]" style={{ backgroundColor: "rgba(168,213,186,0.15)", color: "#5A9B7A" }}>· {tutorialData.platform}</span>
                <span className="px-2 py-0.5 rounded-full text-[9px]" style={{ backgroundColor: "rgba(168,213,186,0.15)", color: "#5A9B7A" }}>· {tutorialData.style}</span>
              </div>
            </div>
          )}
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
