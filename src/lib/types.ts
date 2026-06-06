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

export interface TutorialData {
  videoUrl: string;
  videoTitle: string;
  platform: string;
  style: string;
  styleDescription: string;
  prep: PrepSection[];
  steps: SopStep[];
}
