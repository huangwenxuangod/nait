import { Heart, CheckCircle2, Clock, ChevronRight, History, Star } from "lucide-react";
import { PhoneFrame } from "./PhoneFrame";
import insp1 from "@/assets/insp-1.jpg";
import insp3 from "@/assets/insp-3.jpg";
import insp5 from "@/assets/insp-5.jpg";

interface Props {
  wishlist: string[];
  completed: { title: string; date: string; steps: number }[];
}

const demoWishlist = ["温柔裸粉法式美甲", "猫眼星空紫", "圣诞雪花限定款"];
const demoCompleted = [
  { title: "温柔玫瑰豆沙 · 极光法式", date: "2026-06-07", steps: 6 },
  { title: "焦糖琥珀晕染", date: "2026-06-05", steps: 5 },
];

export function ProfileScreen({ wishlist, completed }: Props) {
  const list = wishlist.length > 0 ? wishlist : demoWishlist;
  const done = completed.length > 0 ? completed : demoCompleted;
  const isEmpty = wishlist.length === 0;
  const points = done.length * 100;


  return (
    <PhoneFrame>
      <div className="flex flex-col min-h-screen pb-20">
        <div className="px-6 pt-12">
          <div className="flex items-center justify-between">
            <h1 className="text-[28px] font-semibold tracking-tight text-foreground">我的</h1>
            <span
              className="flex items-center gap-1 px-3.5 py-1.5 rounded-full text-[14px] font-medium tracking-wider"
              style={{
                backgroundColor: points >= 500 ? "rgba(245,197,66,0.15)" : "rgba(212,163,163,0.08)",
                color: points >= 500 ? "#C49B1A" : "#D4A3A3",
              }}
            >
              {points >= 1000 ? "💎" : points >= 500 ? "🌟" : points >= 200 ? "⭐" : "🌱"}
              {points >= 1000 ? "大师" : points >= 500 ? "进阶" : points >= 200 ? "入门" : "新手"}
            </span>
          </div>
          <p className="mt-1 text-[13px] text-muted-foreground">心愿单 · 制作记录</p>
        </div>

        {/* Stats */}
        <div className="px-5 mt-5">
          {isEmpty && (
            <div
              className="rounded-2xl p-4 flex items-center gap-3 mb-3"
              style={{ background: "linear-gradient(135deg, rgba(168,213,186,0.08) 0%, rgba(168,213,186,0.04) 100%)", border: "1px solid rgba(168,213,186,0.15)" }}
            >
              <div className="w-10 h-10 rounded-xl flex items-center justify-center text-xl shrink-0" style={{ backgroundColor: "rgba(168,213,186,0.12)" }}>
                👋
              </div>
              <div>
                <p className="text-xs font-medium text-foreground">欢迎来到指尖 SOP</p>
                <p className="text-[10px] text-muted-foreground mt-0.5">完成第一款美甲后，记录会出现在这里</p>
              </div>
            </div>
          )}
          <div className="bg-white rounded-2xl p-4 shadow-[0_4px_24px_-10px_rgba(0,0,0,0.08)] flex items-center justify-around">
            <Stat icon={<Heart className="w-4 h-4" />} label="心愿单" value={list.length} color="#D4A3A3" />
            <div className="w-px h-8 bg-border" />
            <Stat icon={<CheckCircle2 className="w-4 h-4" />} label="已完成" value={done.length} color="#A8D5BA" />
            <div className="w-px h-8 bg-border" />
            <Stat icon={<Star className="w-4 h-4" />} label="积分" value={points} color="#F5C542" />
          </div>
        </div>

        {/* Wishlist */}
        <div className="px-5 mt-6">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-1.5">
              <Heart className="w-4 h-4 text-brand" strokeWidth={2} />
              <span className="text-[13px] font-medium text-foreground">心愿单</span>
            </div>
            <span className="text-[11px] text-muted-foreground">{list.length} 款</span>
          </div>
          <div className="space-y-2">
            {list.map((name, i) => (
              <div key={i} className="bg-white rounded-xl p-3.5 flex items-center gap-3 shadow-[0_2px_12px_-4px_rgba(0,0,0,0.04)] hover:shadow-[0_4px_16px_-6px_rgba(0,0,0,0.08)] transition-shadow active:scale-[0.99]">
                <div className="w-11 h-11 rounded-lg bg-muted flex items-center justify-center shrink-0 overflow-hidden">
                  <img src={[insp1, insp3, insp5][i % 3]} alt="" className="w-full h-full object-cover" />
                </div>
                <span className="text-[13px] text-foreground flex-1">{name}</span>
                <ChevronRight className="w-4 h-4 text-muted-foreground/40" />
              </div>
            ))}
          </div>
        </div>

        {/* Completed */}
        <div className="px-5 mt-6">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-success" strokeWidth={2} />
              <span className="text-[13px] font-medium text-foreground">已完成</span>
            </div>
          </div>
          <div className="space-y-2">
            {done.map((d, i) => (
              <div key={i} className="bg-white rounded-xl p-3.5 flex items-center gap-3 shadow-[0_2px_12px_-4px_rgba(0,0,0,0.04)] hover:shadow-[0_4px_16px_-6px_rgba(0,0,0,0.08)] transition-shadow active:scale-[0.99]">
                <div className="w-11 h-11 rounded-lg flex items-center justify-center shrink-0" style={{ backgroundColor: "rgba(168,213,186,0.15)" }}>
                  <CheckCircle2 className="w-5 h-5 text-success" strokeWidth={2} />
                </div>
                <div className="flex-1 min-w-0">
                  <span className="text-[13px] text-foreground block truncate">{d.title}</span>
                  <span className="text-[10px] text-muted-foreground flex items-center gap-2 mt-0.5">
                    <Clock className="w-3 h-3" /> {d.date}
                    <span className="text-success">{d.steps} 步完成</span>
                  </span>
                </div>
                <ChevronRight className="w-4 h-4 text-muted-foreground/40" />
              </div>
            ))}
          </div>
        </div>

        {/* History hint */}
        <div className="px-5 mt-6 mb-6">
          <div
            className="rounded-xl p-4 flex items-center gap-3"
            style={{ backgroundColor: "rgba(212,163,163,0.06)", border: "1px dashed rgba(212,163,163,0.2)" }}
          >
            <History className="w-5 h-5 text-brand shrink-0" />
            <div>
              <p className="text-xs font-medium text-foreground">制作记录自动保存</p>
              <p className="text-[10px] text-muted-foreground mt-0.5">每完成一款美甲，自动记录到这里</p>
            </div>
          </div>
        </div>
      </div>
    </PhoneFrame>
  );
}

function Stat({ icon, label, value, color }: { icon: React.ReactNode; label: string; value: number; color: string }) {
  return (
    <div className="flex flex-col items-center gap-1">
      <div className="w-8 h-8 rounded-full flex items-center justify-center" style={{ backgroundColor: `${color}15`, color }}>
        {icon}
      </div>
      <span className="text-lg font-medium text-foreground">{value}</span>
      <span className="text-[10px] text-muted-foreground">{label}</span>
    </div>
  );
}
