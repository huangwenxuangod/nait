import { Heart, Sparkles } from "lucide-react";
import { PhoneFrame } from "./PhoneFrame";

// 从 src/assets/inspiration/ 自动加载所有图片，文件名即款式名
const imageModules = import.meta.glob<{ default: string }>("../../assets/inspiration/*.{jpg,jpeg,png,webp}", { eager: true });

interface InspirationVideo {
  img: string;
  title: string;
  gender: "female" | "male";
}

const VIDEOS: InspirationVideo[] = Object.entries(imageModules).map(([path, mod]) => {
  const filename = path.split("/").pop()?.replace(/\.[^.]+$/, "") ?? "";
  const gender = filename.includes("男") ? "male" : "female";
  const title = filename.replace(/\[.*\]/, "").trim();
  return { img: mod.default, title, gender };
});

// 添加默认数据以防文件夹为空
if (VIDEOS.length === 0) {
  const fallbackImgs = Object.values(
    import.meta.glob<{ default: string }>("../../assets/insp-*.{jpg,jpeg,png,webp}", { eager: true })
  );
  fallbackImgs.forEach((mod, i) => {
    const defaultTitles = ["温柔裸粉法式", "焦糖琥珀晕染", "透亮冰透", "清新花卉手绘", "猫眼磁石星空紫"];
    VIDEOS.push({
      img: mod.default,
      title: defaultTitles[i] ?? `美甲灵感 ${i + 1}`,
      gender: "female",
    });
  });
}

export function InspirationScreen({ onSelectVideo, genderMode, wishlist, onToggleWishlist }: {
  onSelectVideo: (title: string) => void;
  genderMode: "all" | "male";
  wishlist: string[];
  onToggleWishlist: (title: string) => void;
}) {
  const filtered = VIDEOS.filter((v) => genderMode === "all" || v.gender === "male");

  return (
    <PhoneFrame>
      <div className="flex flex-col min-h-screen pb-20">
        <div className="px-6 pt-12">
          <h1 className="text-[28px] font-semibold tracking-tight text-foreground flex items-center gap-2">
            灵感
            <Sparkles className="w-5 h-5 text-brand animate-sparkle" strokeWidth={2} />
          </h1>
          <p className="mt-1 text-[13px] text-muted-foreground">热门美甲教程 · 粘贴链接即可拆解</p>
        </div>

        {/* Guide banner */}
        <div className="px-5 mt-5">
          <div
            className="rounded-2xl p-4 flex items-center gap-3"
            style={{ background: "linear-gradient(135deg, rgba(212,163,163,0.08) 0%, rgba(235,216,184,0.12) 100%)", border: "1px solid rgba(212,163,163,0.12)" }}
          >
            <div className="w-10 h-10 rounded-xl flex items-center justify-center text-xl shrink-0" style={{ backgroundColor: "rgba(212,163,163,0.12)" }}>
              💡
            </div>
            <div>
              <p className="text-xs font-medium text-foreground">不知道怎么开始？</p>
              <p className="text-[10px] text-muted-foreground mt-0.5">在抖音/小红书找到喜欢的教程 → 复制链接 → 回首页粘贴</p>
            </div>
          </div>
        </div>

        {/* Result count */}
        <div className="px-5 mt-5">
          <p className="text-[10px] text-muted-foreground">{filtered.length} 个灵感</p>
        </div>

        {/* Video grid */}
        <div className="px-5 mt-3 grid grid-cols-2 gap-3">
          {filtered.map((v, i) => (
            <button
              key={v.title + i}
              onClick={() => onSelectVideo(v.title)}
              className="bg-white rounded-2xl overflow-hidden shadow-[0_2px_12px_-4px_rgba(0,0,0,0.04)] hover:shadow-[0_4px_20px_-6px_rgba(0,0,0,0.1)] active:scale-[0.98] transition-all duration-200 text-left"
            >
              <div className="aspect-[4/5] relative overflow-hidden bg-muted">
                <img src={v.img} alt={v.title} loading="lazy" className="w-full h-full object-cover" />
                <button
                  onClick={(e) => { e.stopPropagation(); onToggleWishlist(v.title); }}
                  className="absolute top-2 right-2 w-7 h-7 rounded-full flex items-center justify-center backdrop-blur transition active:scale-90"
                  style={{
                    backgroundColor: wishlist.includes(v.title) ? "rgba(212,163,163,0.3)" : "rgba(0,0,0,0.3)",
                  }}
                >
                  <Heart
                    className="w-3.5 h-3.5"
                    fill={wishlist.includes(v.title) ? "#D4A3A3" : "none"}
                    stroke={wishlist.includes(v.title) ? "#D4A3A3" : "rgba(255,255,255,0.8)"}
                    strokeWidth={2.5}
                  />
                </button>
              </div>
              <div className="p-2.5">
                <p className="text-[12px] font-medium text-foreground truncate">{v.title}</p>
              </div>
            </button>
          ))}
        </div>
      </div>
    </PhoneFrame>
  );
}
