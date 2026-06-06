import { Check } from "lucide-react";
import { useMemo, useState } from "react";
import { PhoneFrame } from "./PhoneFrame";
import thumb from "@/assets/nail-thumb.jpg";

type Item = { id: string; label: string; hint?: string };

const SECTIONS: { title: string; items: Item[] }[] = [
  {
    title: "基础工具",
    items: [
      { id: "base", label: "底胶 Base Coat", hint: "持久不脱落" },
      { id: "lamp", label: "UV / LED 烤灯", hint: "60s 固化" },
      { id: "top", label: "封层 Top Coat", hint: "镜面光泽" },
      { id: "file", label: "海绵搓条 + 抛光锉" },
    ],
  },
  {
    title: "专属色号",
    items: [
      { id: "rose", label: "Muted Rose 哑光胶", hint: "#D4A3A3" },
      { id: "aurora", label: "Aurora 极光镭射粉", hint: "焦点亮片" },
    ],
  },
];

export function PrepScreen({ onBack, onStart }: { onBack: () => void; onStart: () => void }) {
  const [checked, setChecked] = useState<Set<string>>(new Set());

  const toggle = (id: string) => {
    setChecked((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const enabled = checked.size > 0;
  const total = useMemo(() => SECTIONS.reduce((s, x) => s + x.items.length, 0), []);

  return (
    <PhoneFrame>
      <div className="flex flex-col min-h-screen pb-32">
        {/* Header */}
        <div className="px-7 pt-14">
          <button onClick={onBack} className="text-xs text-muted-foreground tracking-widest mb-3">← 返回</button>
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
            已勾选 <span className="text-brand">{checked.size}</span> / {total} · 集齐即可开始
          </p>
        </div>

        {/* Lists */}
        <div className="mt-8 px-7 space-y-8">
          {SECTIONS.map((sec) => (
            <div key={sec.title}>
              <p className="text-[11px] tracking-[0.3em] text-muted-foreground/80 uppercase mb-3">{sec.title}</p>
              <div className="bg-white rounded-2xl shadow-[0_4px_20px_-12px_rgba(0,0,0,0.1)] divide-y divide-border/60">
                {sec.items.map((it) => {
                  const isOn = checked.has(it.id);
                  return (
                    <button
                      key={it.id}
                      onClick={() => toggle(it.id)}
                      className="w-full flex items-center gap-4 px-5 py-4 text-left active:bg-muted/40 transition"
                    >
                      <span
                        className={`w-6 h-6 rounded-full flex items-center justify-center border transition-all duration-300 ${
                          isOn ? "border-transparent" : "border-border bg-white"
                        }`}
                        style={isOn ? { backgroundColor: "#A8D5BA" } : undefined}
                      >
                        <Check className={`w-4 h-4 text-white transition-opacity duration-300 ${isOn ? "opacity-100" : "opacity-0"}`} strokeWidth={3} />
                      </span>
                      <div className="flex-1">
                        <p
                          className={`text-sm transition-all duration-300 ${
                            isOn ? "line-through" : ""
                          }`}
                          style={{ color: isOn ? "#9CA3AF" : "#2C2C2C" }}
                        >
                          {it.label}
                        </p>
                        {it.hint && (
                          <p className={`text-[11px] mt-0.5 transition-colors duration-300 ${isOn ? "text-[#9CA3AF]" : "text-muted-foreground"}`}>
                            {it.hint}
                          </p>
                        )}
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </div>

        {/* Sticky CTA */}
        <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[430px] px-7 pb-8 pt-6 bg-gradient-to-t from-[#FAFAFA] via-[#FAFAFA]/95 to-transparent">
          <button
            onClick={enabled ? onStart : undefined}
            disabled={!enabled}
            className={`w-full rounded-full py-4 text-sm tracking-[0.25em] transition ${
              enabled
                ? "cta-gradient text-white shadow-[0_10px_30px_-10px_rgba(212,163,163,0.6)] active:scale-[0.98]"
                : "bg-muted text-muted-foreground/60"
            }`}
          >
            开始沉浸式制作
          </button>
        </div>
      </div>
    </PhoneFrame>
  );
}
