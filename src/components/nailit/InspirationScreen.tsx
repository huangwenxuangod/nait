import { Sparkles, Flame, Play, ChevronRight, Clapperboard } from "lucide-react";
import { PhoneFrame } from "./PhoneFrame";
import insp1 from "@/assets/insp-1.jpg";
import insp2 from "@/assets/insp-2.jpg";
import insp3 from "@/assets/insp-3.jpg";
import insp4 from "@/assets/insp-4.jpg";
import insp5 from "@/assets/insp-5.jpg";

const VIDEOS = [
  { img: insp1, title: "温柔裸粉法式美甲", author: "美甲师小A", views: "12.3w", tags: ["法式", "裸粉", "通勤"] },
  { img: insp2, title: "焦糖琥珀晕染教程", author: "NailArt Studio", views: "8.5w", tags: ["晕染", "焦糖", "秋冬"] },
  { img: insp3, title: "透亮冰透款 · 夏日清凉", author: "指尖艺术家", views: "11.1w", tags: ["冰透", "夏日", "清透"] },
  { img: insp4, title: "清新花卉手绘美甲", author: "花花美甲", views: "9.2k", tags: ["手绘", "花卉", "春日"] },
  { img: insp5, title: "气质豆沙渐变甲", author: "温柔系美甲", views: "7.6w", tags: ["渐变", "豆沙", "气质"] },
  { img: insp1, title: "猫眼磁石 · 星空紫", author: "猫眼女王", views: "15.8w", tags: ["猫眼", "磁石", "星空"] },
  { img: insp2, title: "极光镭射 · 派对款", author: "Nail Queen", views: "6.3w", tags: ["极光", "派对", "闪"] },
  { img: insp3, title: "雾霾蓝磨砂质感", author: "简约美甲记", views: "4.9w", tags: ["磨砂", "雾霾蓝", "高级"] },
  { img: insp4, title: "圣诞雪花限定款", author: "节日美甲控", views: "18.2w", tags: ["圣诞", "雪花", "节日"] },
  { img: insp5, title: "极简线条 · 性冷淡风", author: "线条控", views: "3.4w", tags: ["极简", "线条", "性冷淡"] },
];

export function InspirationScreen({ onSelectVideo }: { onSelectVideo: (title: string) => void }) {
  return (
    <PhoneFrame>
      <div className="flex flex-col min-h-screen pb-20">
        <div className="px-6 pt-12">
          <h1 className="text-[28px] font-semibold tracking-tight text-foreground flex items-center gap-2">
            灵感
            <Flame className="w-5 h-5 text-brand" strokeWidth={2} />
          </h1>
          <p className="mt-1 text-[13px] text-muted-foreground">热门美甲教程 · 粘贴链接即可拆解</p>
        </div>

        {/* Category chips */}
        <div className="px-5 mt-5 flex items-center gap-2 overflow-x-auto no-scrollbar">
          {["全部", "纯色", "渐变", "猫眼", "法式", "手绘", "贴钻", "节日限定"].map((c, i) => (
            <button
              key={c}
              className={`px-4 py-2 rounded-full text-[11px] tracking-wider shrink-0 transition ${
                i === 0
                  ? "cta-gradient text-white shadow-sm"
                  : "bg-white text-muted-foreground border border-border hover:text-brand"
              }`}
            >
              {c}
            </button>
          ))}
        </div>

        {/* Video grid */}
        <div className="px-5 mt-5 grid grid-cols-2 gap-3">
          {VIDEOS.map((v) => (
            <button
              key={v.title}
              onClick={() => onSelectVideo(v.title)}
              className="bg-white rounded-2xl overflow-hidden shadow-[0_2px_12px_-4px_rgba(0,0,0,0.06)] active:scale-[0.98] transition text-left"
            >
              <div className="aspect-[4/5] relative overflow-hidden bg-muted">
                <img src={v.img} alt={v.title} loading="lazy" className="w-full h-full object-cover" />
                <span className="absolute bottom-2 right-2 bg-black/50 backdrop-blur text-white text-[9px] px-2 py-0.5 rounded-full flex items-center gap-1">
                  <Play className="w-2.5 h-2.5" fill="white" /> {v.views}
                </span>
              </div>
              <div className="p-2.5">
                <p className="text-[12px] font-medium text-foreground truncate">{v.title}</p>
                <p className="text-[10px] text-muted-foreground mt-0.5">{v.author}</p>
                <div className="flex items-center gap-1 mt-1.5 flex-wrap">
                  {v.tags.slice(0, 2).map((t) => (
                    <span key={t} className="text-[9px] px-1.5 py-0.5 rounded-md bg-muted text-muted-foreground">
                      {t}
                    </span>
                  ))}
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>
    </PhoneFrame>
  );
}
