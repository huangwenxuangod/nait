import { Play, Sparkles } from "lucide-react";
import { useState } from "react";
import { PhoneFrame } from "./PhoneFrame";

// 从 src/assets/inspiration/ 自动加载所有图片，文件名即款式名
const imageModules = import.meta.glob<{ default: string }>("../../../assets/inspiration/*.{jpg,jpeg,png,webp}", { eager: true });

interface InspirationVideo {
  img: string;
  title: string;
  tags: string[];
  gender: "female" | "male";
}

const VIDEOS: InspirationVideo[] = Object.entries(imageModules).map(([path, mod]) => {
  const filename = path.split("/").pop()?.replace(/\.[^.]+$/, "") ?? "";
  // 匹配文件名后面的标签如 [女-法式-裸粉]
  const tagMatch = filename.match(/\[(.+)\]$/);
  const tags = tagMatch ? tagMatch[1].split("-") : [];
  const gender = tags.includes("男") ? "male" : "female";
  const title = tagMatch ? filename.replace(tagMatch[0], "").trim() : filename;
  return { img: mod.default, title, tags, gender };
});

// 添加默认数据以防文件夹为空
if (VIDEOS.length === 0) {
  const fallbackImgs = Object.values(
    import.meta.glob<{ default: string }>("../../../assets/insp-*.{jpg,jpeg,png,webp}", { eager: true })
  );
  fallbackImgs.forEach((mod, i) => {
    const defaultTitles = ["温柔裸粉法式", "焦糖琥珀晕染", "透亮冰透", "清新花卉手绘", "猫眼磁石星空紫"];
    VIDEOS.push({
      img: mod.default,
      title: defaultTitles[i] ?? `美甲灵感 ${i + 1}`,
      tags: [],
      gender: "female",
    });
  });
}

const ALL_TAGS_FEMALE = ["纯色", "渐变", "猫眼", "法式", "手绘", "磨砂", "极简"];
const ALL_TAGS_MALE = ["磨砂", "极简", "深色", "哑光", "裸色", "渐变"];

export function InspirationScreen({ onSelectVideo, genderMode }: { onSelectVideo: (title: string) => void; genderMode: "all" | "male" }) {
  const [activeTag, setActiveTag] = useState<string | null>(null);
  const allTags = genderMode === "male" ? ALL_TAGS_MALE : ALL_TAGS_FEMALE;

  const filtered = VIDEOS
    .filter((v) => genderMode === "all" || v.gender === "male")
    .filter((v) => !activeTag || v.tags.includes(activeTag));

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

        {/* Category chips */}
        <div className="px-5 mt-5 flex items-center gap-2 overflow-x-auto no-scrollbar">
          {allTags.map((tag) => (
            <button
              key={tag}
              onClick={() => setActiveTag(activeTag === tag ? null : tag)}
              className={`px-4 py-2 rounded-full text-[11px] tracking-wider shrink-0 transition active:scale-95 ${
                activeTag === tag
                  ? "cta-gradient text-white shadow-sm"
                  : "bg-white text-muted-foreground border border-border hover:text-brand"
              }`}
            >
              {tag}
            </button>
          ))}
        </div>

        {/* Result count */}
        <div className="px-5 mt-3">
          <p className="text-[10px] text-muted-foreground">
            {activeTag ? `「${activeTag}」 ${filtered.length} 个结果` : `${filtered.length} 个灵感`}
          </p>
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
              </div>
              <div className="p-2.5">
                <p className="text-[12px] font-medium text-foreground truncate">{v.title}</p>
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
