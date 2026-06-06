import { Composition } from "remotion";
import { NailTutorialVideo } from "./NailTutorialVideo";
import type { TutorialData } from "@/lib/types";

// Demo data for CLI rendering
const demoData: TutorialData = {
  videoUrl: "",
  videoTitle: "温柔玫瑰豆沙 · 极光法式",
  platform: "抖音",
  style: "哑光玫瑰豆沙 + 极光镭射粉",
  styleDescription: "低饱和干枯玫瑰色为底，指尖点缀极光镭射偏光",
  steps: [
    { step: 1, total: 6, title: "修形打磨", instruction: "海绵锉单向修出方圆甲型", detail: "45°角单向打磨", duration: 120, isLampCure: false, originalQuote: "", aiTranslation: "锉条呈45°角单向打磨，不要来回拉锯" },
    { step: 2, total: 6, title: "上底胶", instruction: "全甲均匀薄涂一层底胶", detail: "刮掉2/3胶再涂", duration: 60, isLampCure: false, originalQuote: "", aiTranslation: "刷头蘸胶后刮掉2/3，从甲根推到甲尖" },
    { step: 3, total: 6, title: "照灯固化", instruction: "LED灯下固化90秒", detail: "手平放不动", duration: 90, isLampCure: true, originalQuote: "", aiTranslation: "UV/LED灯固化至少90秒，手平放灯内不要动" },
    { step: 4, total: 6, title: "上色胶", instruction: "薄涂两层色胶", detail: "第一层半透明，第二层饱满", duration: 120, isLampCure: false, originalQuote: "", aiTranslation: "第一层薄涂→照灯60s→第二层厚涂" },
    { step: 5, total: 6, title: "封层照灯", instruction: "涂封层后照灯120秒", detail: "全甲覆盖+包边", duration: 120, isLampCure: true, originalQuote: "", aiTranslation: "封层覆盖全甲面+包边，照灯120s" },
    { step: 6, total: 6, title: "精修检查", instruction: "清洁浮胶+指缘油", detail: "擦浮胶，涂营养油", duration: 60, isLampCure: false, originalQuote: "", aiTranslation: "用酒精擦浮胶→涂营养油按摩甲缘" },
  ],
  prep: [],
};

const INTRO_SEC = 2;
const STEP_SEC = 5;
const OUTRO_SEC = 3;
const totalSec = INTRO_SEC + demoData.steps.length * STEP_SEC + OUTRO_SEC;

export const RemotionRoot = () => {
  return (
    <Composition
      id="nail-tutorial"
      component={NailTutorialVideo as any}
      durationInFrames={30 * totalSec}
      fps={30}
      width={1080}
      height={1920}
      defaultProps={{
        data: demoData,
        stepImages: [] as string[],
      }}
    />
  );
};
