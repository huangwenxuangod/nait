import { useState } from "react";
import { PhoneFrame } from "./PhoneFrame";
import { ChevronLeft, Check } from "lucide-react";

interface ShapeInfo {
  id: string;
  name: string;
}

const SHAPES: ShapeInfo[] = [
  { id: "square-round", name: "方圆形" },
  { id: "oval", name: "椭圆形" },
  { id: "almond", name: "杏仁形" },
  { id: "round", name: "圆形" },
  { id: "hard-square", name: "硬方形" },
  { id: "soft-square", name: "软方形" },
];

interface Props {
  styleName: string;
  primaryColor: string;
  onBack: () => void;
  onConfirm: (shape: string, length: string) => void;
}

export function NailShapeTrialScreen({ styleName, primaryColor, onBack, onConfirm }: Props) {
  const [shapeIdx, setShapeIdx] = useState(0);
  const [length, setLength] = useState<"short" | "medium" | "long">("medium");

  const shape = SHAPES[shapeIdx];
  const color = primaryColor || "#D4A3A3";

  return (
    <PhoneFrame>
      <div className="flex flex-col min-h-screen">
        {/* Header */}
        <div className="flex items-center justify-between px-5 pt-12 pb-4">
          <button onClick={onBack} className="w-9 h-9 rounded-full bg-white shadow-sm flex items-center justify-center active:scale-95 transition">
            <ChevronLeft className="w-5 h-5 text-foreground" strokeWidth={1.5} />
          </button>
          <h2 className="text-base tracking-[0.25em] font-light">选甲型 · 调长度</h2>
          <div className="w-9 h-9" />
        </div>

        <p className="text-center text-[11px] text-muted-foreground mt-1">{styleName}</p>

        {/* Hand preview */}
        <div className="flex-1 flex items-center justify-center px-8">
          <div className="relative w-full max-w-[280px]">
            {/* Hand SVG */}
            <svg viewBox="0 0 240 320" className="w-full" style={{ filter: "drop-shadow(0 4px 12px rgba(0,0,0,0.08))" }}>
              {/* Palm */}
              <path
                d="M80 140 L80 280 Q80 310 110 310 L140 310 Q170 310 170 280 L170 140"
                fill="rgba(212,163,163,0.1)"
                stroke="rgba(212,163,163,0.2)"
                strokeWidth="1"
              />
              {/* Index finger */}
              <path
                d="M80 140 L78 60 Q77 40 92 38 Q107 36 108 55 L108 140"
                fill="rgba(212,163,163,0.08)"
                stroke="rgba(212,163,163,0.2)"
                strokeWidth="1"
              />
              {/* Middle finger */}
              <path
                d="M108 140 L110 45 Q110 25 125 23 Q140 21 140 40 L138 140"
                fill="rgba(212,163,163,0.08)"
                stroke="rgba(212,163,163,0.2)"
                strokeWidth="1"
              />
              {/* Ring finger */}
              <path
                d="M138 140 L140 55 Q140 35 155 33 Q170 31 170 50 L172 140"
                fill="rgba(212,163,163,0.08)"
                stroke="rgba(212,163,163,0.2)"
                strokeWidth="1"
              />
              {/* Pinky */}
              <path
                d="M172 140 L174 75 Q175 60 188 60 Q200 60 200 78 L198 140"
                fill="rgba(212,163,163,0.08)"
                stroke="rgba(212,163,163,0.2)"
                strokeWidth="1"
              />

              {/* Nails */}
              {[
                { x: 82, y: 45, w: 22 },
                { x: 114, y: 30, w: 22 },
                { x: 143, y: 40, w: 22 },
                { x: 177, y: 65, w: 18 },
              ].map((n, i) => {
                const nailH = length === "long" ? 22 : length === "medium" ? 16 : 11;
                const nailW = n.w * (length === "long" ? 0.85 : 1);
                const rx = shape.id === "round" ? 10 : shape.id === "almond" ? 8 : shape.id === "oval" ? 9 : shape.id === "hard-square" ? 3 : 5;
                const ry = shape.id === "round" ? 12 : shape.id === "almond" ? 14 : shape.id === "oval" ? 11 : shape.id === "hard-square" ? 2 : 4;

                return (
                  <rect
                    key={i}
                    x={n.x + (n.w - nailW) / 2}
                    y={n.y}
                    width={nailW}
                    height={nailH}
                    rx={rx}
                    ry={ry}
                    fill={color}
                    opacity={0.85}
                  />
                );
              })}
            </svg>

            {/* Shape name badge */}
            <div className="absolute top-2 right-2 bg-white/90 backdrop-blur rounded-full px-3 py-1 text-[11px] font-medium text-foreground shadow-sm">
              {shape.name}
            </div>
          </div>
        </div>

        {/* Controls */}
        <div className="px-5 pb-8">
          {/* Shape selector */}
          <p className="text-[10px] tracking-widest text-muted-foreground uppercase mb-2 text-center">甲型</p>
          <div className="flex items-center justify-center gap-1.5 mb-4 flex-wrap">
            {SHAPES.map((s, i) => (
              <button
                key={s.id}
                onClick={() => setShapeIdx(i)}
                className={`px-3 py-1.5 rounded-full text-[11px] tracking-wider transition active:scale-95 ${
                  i === shapeIdx
                    ? "cta-gradient text-white shadow-sm"
                    : "bg-white text-muted-foreground border border-border"
                }`}
              >
                {s.name}
              </button>
            ))}
          </div>

          {/* Length selector */}
          <p className="text-[10px] tracking-widest text-muted-foreground uppercase mb-2 text-center">长度</p>
          <div className="flex items-center justify-center gap-2 mb-6">
            {(["short", "medium", "long"] as const).map((l) => (
              <button
                key={l}
                onClick={() => setLength(l)}
                className={`px-5 py-2 rounded-full text-[12px] tracking-wider transition active:scale-95 ${
                  length === l
                    ? "text-white shadow-sm"
                    : "bg-white text-muted-foreground border border-border"
                }`}
                style={length === l ? { backgroundColor: color } : undefined}
              >
                {l === "short" ? "短" : l === "medium" ? "中" : "长"}
              </button>
            ))}
          </div>

          {/* Confirm */}
          <button
            onClick={() => onConfirm(shape.id, length)}
            className="w-full flex items-center justify-center gap-2 py-4 rounded-full text-sm tracking-[0.25em] text-white cta-gradient shadow-[0_10px_30px_-10px_rgba(212,163,163,0.6)] active:scale-[0.98] transition"
          >
            <Check className="w-4 h-4" strokeWidth={2.5} />
            确认甲型 · 继续
          </button>
        </div>
      </div>
    </PhoneFrame>
  );
}
