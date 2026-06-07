import { useEffect, useState } from "react";
import { Sparkles } from "lucide-react";

export function SplashScreen({ onDone }: { onDone: () => void }) {
  const [show, setShow] = useState(false);
  const [fadeOut, setFadeOut] = useState(false);

  useEffect(() => {
    const t1 = setTimeout(() => setShow(true), 100);
    const t2 = setTimeout(() => setFadeOut(true), 2200);
    const t3 = setTimeout(() => onDone(), 2600);
    return () => { clearTimeout(t1); clearTimeout(t2); clearTimeout(t3); };
  }, [onDone]);

  return (
    <div
      className="fixed inset-0 z-50 flex flex-col items-center justify-center transition-opacity duration-400"
      style={{
        backgroundColor: "#FAFAFA",
        opacity: fadeOut ? 0 : 1,
      }}
    >
      <div
        className="flex flex-col items-center gap-4 transition-all duration-600 ease-out"
        style={{
          opacity: show ? 1 : 0,
          transform: show ? "translateY(0)" : "translateY(12px)",
        }}
      >
        <div className="relative">
          <Sparkles
            className="w-8 h-8 text-brand animate-sparkle"
            strokeWidth={1.5}
            style={{
              position: "absolute",
              top: -8,
              right: -20,
            }}
          />
          <h1
            className="text-[44px] font-semibold tracking-tight"
            style={{
              background: "linear-gradient(135deg, #D4A3A3 0%, #EBD8B8 100%)",
              WebkitBackgroundClip: "text",
              WebkitTextFillColor: "transparent",
            }}
          >
            指尖 SOP
          </h1>
        </div>

        <p
          className="text-[13px] tracking-[0.25em] transition-all duration-500"
          style={{
            color: "rgba(0,0,0,0.25)",
            opacity: show ? 1 : 0,
            transitionDelay: "0.3s",
          }}
        >
          试戴看效果 · AI 拆解教程 · 跟着做
        </p>
      </div>
    </div>
  );
}
