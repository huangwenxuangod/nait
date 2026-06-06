import { useEffect, useRef, useState } from "react";
import { PhoneFrame } from "./PhoneFrame";
import stepImg from "@/assets/nail-step.jpg";

const DURATION = 60;

export function FocusScreen({ onExit }: { onExit: () => void }) {
  const [seconds, setSeconds] = useState(DURATION);
  const holdRef = useRef<number | null>(null);

  useEffect(() => {
    const id = window.setInterval(() => {
      setSeconds((s) => (s > 0 ? s - 1 : DURATION));
    }, 1000);
    return () => clearInterval(id);
  }, []);

  const startHold = () => {
    holdRef.current = window.setTimeout(() => onExit(), 800);
  };
  const cancelHold = () => {
    if (holdRef.current) {
      clearTimeout(holdRef.current);
      holdRef.current = null;
    }
  };

  const radius = 38;
  const circ = 2 * Math.PI * radius;
  const offset = circ * (1 - seconds / DURATION);

  return (
    <PhoneFrame dark>
      <div
        className="relative flex flex-col min-h-screen text-white select-none"
        onPointerDown={startHold}
        onPointerUp={cancelHold}
        onPointerLeave={cancelHold}
      >
        {/* Top status */}
        <div className="flex items-center justify-between px-6 pt-12 text-[10px] tracking-[0.3em] text-white/50 uppercase">
          <span>· LIVE SOP</span>
          <span>Step 02 / 06</span>
        </div>

        {/* Step image — 50% */}
        <div className="mt-4 mx-5 rounded-[2rem] overflow-hidden" style={{ height: "50vh" }}>
          <img src={stepImg} alt="当前步骤" className="w-full h-full object-cover" />
        </div>

        {/* Step text */}
        <div className="px-7 mt-6">
          <p className="text-xs tracking-[0.3em] text-brand/80 uppercase">Step 02</p>
          <h2 className="mt-2 text-3xl font-light leading-snug">
            薄涂建构底胶<span className="text-white/50">，</span>包边
          </h2>
          <p className="mt-3 text-sm text-white/50 leading-relaxed">
            刷头蘸取少量胶体，沿甲面边缘 0.5mm 处轻扫，避免溢胶。
          </p>
        </div>

        {/* Bottom timer + voice */}
        <div className="mt-auto px-7 pb-10 flex items-end justify-between">
          {/* Countdown */}
          <div className="relative w-24 h-24">
            <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r={radius} stroke="rgba(255,255,255,0.08)" strokeWidth="4" fill="none" />
              <circle
                cx="50" cy="50" r={radius}
                stroke="#D4A3A3" strokeWidth="4" fill="none"
                strokeLinecap="round"
                strokeDasharray={circ}
                strokeDashoffset={offset}
                style={{ transition: "stroke-dashoffset 1s linear" }}
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-2xl font-light tabular-nums">{seconds}<span className="text-xs text-white/40">s</span></span>
              <span className="text-[9px] tracking-[0.2em] text-white/40 uppercase mt-0.5">固化中</span>
            </div>
          </div>

          {/* Voice indicator */}
          <div className="flex items-center gap-2.5">
            <div className="flex items-end gap-[3px] h-5">
              <span className="w-[3px] h-full bg-[#A8D5BA] rounded-full animate-wave" style={{ animationDelay: "0ms" }} />
              <span className="w-[3px] h-full bg-[#A8D5BA] rounded-full animate-wave" style={{ animationDelay: "150ms" }} />
              <span className="w-[3px] h-full bg-[#A8D5BA] rounded-full animate-wave" style={{ animationDelay: "300ms" }} />
              <span className="w-[3px] h-full bg-[#A8D5BA] rounded-full animate-wave" style={{ animationDelay: "150ms" }} />
            </div>
            <span className="text-[11px] tracking-widest text-white/60">语音助手已唤醒</span>
          </div>
        </div>

        <p className="text-center text-[10px] tracking-[0.3em] text-white/25 uppercase pb-6">
          长按屏幕结束制作
        </p>
      </div>
    </PhoneFrame>
  );
}
