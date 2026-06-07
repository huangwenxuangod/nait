export interface PrepItem {
  id: string;
  label: string;
  hint: string;
  type: "tool" | "color";
  alternatives: { label: string; desc: string; emoji: string }[];
}

export interface PrepSection {
  title: string;
  items: PrepItem[];
}

export interface SopStep {
  step: number;
  total: number;
  title: string;
  instruction: string;
  detail: string;
  duration: number;
  isLampCure: boolean;
  originalQuote: string;
  aiTranslation: string;
  videoStart?: number;
}

export interface NailShape {
  id: string;
  name: string;
  emoji: string;
  desc: string;
  fit: string;
}

export const NAIL_SHAPES: Record<string, NailShape[]> = {
  female: [
    { id: "square-round", name: "方圆形", emoji: "💅", desc: "前缘平直带微弧，利落又柔和", fit: "百搭 · 新手首选 · 不勾头发" },
    { id: "oval", name: "椭圆形", emoji: "🤍", desc: "甲身修长椭圆，显手指纤细", fit: "甲床偏短 · 温柔风 · 通勤" },
    { id: "almond", name: "杏仁形", emoji: "🌸", desc: "圆润尖弧收窄，优雅修长", fit: "拉长手型 · 法式复古 · 轻欧美" },
    { id: "round", name: "圆形", emoji: "🌙", desc: "自然圆润短甲，低调日常", fit: "学生党 · 短甲 · 简约裸色" },
  ],
  male: [
    { id: "square-round", name: "方圆形", emoji: "💅", desc: "前缘平直微弧，干净利落", fit: "最自然 · 百搭 · 适合宽甲床" },
    { id: "hard-square", name: "硬方形", emoji: "◼️", desc: "棱角分明平直，飒爽硬朗", fit: "欧美风 · 个性强 · 街头酷飒" },
    { id: "round", name: "圆形", emoji: "🌙", desc: "短圆润弧，低调极简", fit: "短甲首选 · 商务 · 极简风" },
    { id: "soft-square", name: "软方形", emoji: "▫️", desc: "边角柔化的方形，温和有型", fit: "甲床偏短 · 韩系温柔 · 气质" },
  ],
};

export interface TutorialData {
  videoUrl: string;
  videoTitle: string;
  platform: string;
  style: string;
  styleDescription: string;
  nailShape?: string;
  prep: PrepSection[];
  steps: SopStep[];
}
