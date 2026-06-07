import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import type { TutorialData } from "../types";
import { chatCompletion } from "./qwen";

const DEMO_TUTORIAL: TutorialData = {
  videoUrl: "",
  videoTitle: "温柔玫瑰豆沙 · 极光法式",
  platform: "抖音",
  style: "哑光玫瑰豆沙 + 极光镭射粉 · 清透法式",
  styleDescription: "低饱和干枯玫瑰色为底，指尖点缀极光镭射偏光，日常不张扬但光线流转间有惊喜。适合通勤约会，短甲长甲都好看。",
  prep: [
    {
      title: "基础工具",
      items: [
        { id: "base", label: "底胶", hint: "让颜色牢牢扒在指甲上", type: "tool", alternatives: [{ label: "水性亮油", desc: "文具店/美妆店有售，临时替代但持久度减半", emoji: "💅" }] },
        { id: "lamp", label: "烤灯", hint: "把胶照干固化", type: "tool", alternatives: [{ label: "手电筒 + 锡纸", desc: "用锡纸卷成锥形聚光，贴近甲面照射 2-3 分钟", emoji: "🔦" }, { label: "户外阳光", desc: "晴天时手伸到阳光直射处 2-3 分钟，紫外线自然固化", emoji: "☀️" }] },
        { id: "top", label: "封层", hint: "最后涂的保护层，亮面效果", type: "tool", alternatives: [{ label: "高光透明指甲油", desc: "普通指甲油店的透明亮油，不需照灯但持久度差一些", emoji: "✨" }] },
        { id: "file", label: "海绵搓条 + 抛光锉", hint: "修指甲形状、打磨甲面", type: "tool", alternatives: [{ label: "厨房海绵（粗面）", desc: "洗碗海绵的粗糙面可以替代粗打磨，注意顺着一个方向", emoji: "🧽" }, { label: "废弃指甲锉", desc: "旧的指甲锉条也可以临时用，细砂面抛光", emoji: "📏" }] },
        { id: "cleaner", label: "清洁液 + 棉片", hint: "擦掉表面浮胶和油脂", type: "tool", alternatives: [{ label: "75% 医用酒精 + 化妆棉", desc: "药店买酒精，搭配化妆棉片擦拭甲面", emoji: "🧴" }, { label: "免洗洗手液", desc: "含酒精的免洗洗手液挤在棉片上擦拭", emoji: "🧼" }] },
      ],
    },
    {
      title: "专属色号",
      items: [
        { id: "rose", label: "哑光玫瑰豆沙胶", hint: "温柔的玫瑰豆沙色", type: "color", alternatives: [{ label: "#D4A0A0 干枯玫瑰", desc: "偏暖调的玫瑰色，上手效果非常接近", emoji: "🌸" }, { label: "#E8C4C4 藕粉色", desc: "颜色偏浅偏粉，适合喜欢淡妆风格的", emoji: "🎀" }, { label: "裸色打底 + 玫瑰金闪粉", desc: "任何裸色甲胶打底，薄撒一层玫瑰金闪粉叠加", emoji: "💫" }] },
        { id: "aurora", label: "极光镭射粉", hint: "极光偏光效果的点睛之笔", type: "color", alternatives: [{ label: "透明闪粉 + 珍珠白叠加", desc: "细闪粉混入透明甲胶，再刷一层珍珠白微光感", emoji: "🌟" }, { label: "眼影盘偏光高光粉", desc: "用化妆刷蘸取眼影盘里的偏光色，轻拍在未干甲面上", emoji: "🎨" }, { label: "银色极光纸碎片", desc: "极光包装纸剪碎后撒在甲面，未干时封层覆盖", emoji: "✂️" }] },
      ],
    },
  ],
  steps: [
    { step: 1, total: 6, title: "修形打磨", instruction: "海绵锉单向修出方圆甲型", detail: "锉条与甲面呈 45°，单向向外打磨 3-5 下，避免来回拉锯损伤甲面", duration: 120, isLampCure: false, videoStart: 0, originalQuote: "先把指甲形状修一修就好了", aiTranslation: "锉条与甲面呈45°角，单向向外打磨，不要来回拉锯。甲缘留1-2mm白边保证受力均匀" },
    { step: 2, total: 6, title: "上底胶", instruction: "全甲均匀薄涂一层底胶", detail: "刷头蘸胶后在瓶口刮掉 2/3，从甲根推到甲尖，两侧留 0.5mm 缝隙包边", duration: 60, isLampCure: false, videoStart: 20, originalQuote: "薄薄涂一层底胶就行", aiTranslation: "刷头蘸胶后务必在瓶口刮掉2/3，从甲根匀速推至甲尖。⚠️ 两侧留0.5mm缝隙避免溢胶起翘" },
    { step: 3, total: 6, title: "照灯固化底胶", instruction: "LED 灯下固化 90 秒", detail: "手平放入灯内，四指并拢，中途不要移动或拿出查看", duration: 90, isLampCure: true, videoStart: 40, originalQuote: "照灯烤一下，干了就行", aiTranslation: "UV/LED灯固化至少90秒，手平放灯内不要动。⚠️ 未完全固化会导致甲面变软起皱" },
    { step: 4, total: 6, title: "上色胶", instruction: "刷头蘸取少量色胶薄涂两层", detail: "第一层半透明即可，照灯60s后上第二层达到饱满显色。避开甲缘皮肤", duration: 120, isLampCure: false, videoStart: 60, originalQuote: "上两遍颜色，涂匀一点", aiTranslation: "第一层薄涂（半透明即可）→照灯60s→第二层厚涂达到饱满。每层从甲根1mm处起笔，避开皮肤" },
    { step: 5, total: 6, title: "照灯 + 封层", instruction: "照灯90s后涂封层再照灯120s", detail: "封层需覆盖全甲面并包边，照灯至完全固化呈镜面光泽", duration: 120, isLampCure: true, videoStart: 85, originalQuote: "最后再照一下，涂个亮油封住", aiTranslation: "色胶照灯90s固化→涂封层覆盖全部甲面+包边→再照灯120s。⚠️ 封层未包边2-3天后会从指尖起翘" },
    { step: 6, total: 6, title: "精修检查", instruction: "清洁浮胶 + 指缘油收尾", detail: "棉片蘸酒精擦去表面浮胶，甲缘涂抹营养油按摩吸收", duration: 60, isLampCure: false, videoStart: 110, originalQuote: "最后擦一擦就好了", aiTranslation: "用清洁液/酒精擦去表面浮胶→检查甲面平整度→甲缘涂抹营养油。⚠️ 浮胶未擦干净会显得甲面发黏不光滑" },
  ],
};

const PARSE_PROMPT = `你是一个专业的美甲教程 AI 拆解助手。用户粘贴了一个美甲视频链接，你需要模拟对该视频内容的分析，生成一份结构化的美甲 SOP。

要求：
1. 所有内容全部使用中文，不要出现英文单词。
2. 根据视频实际内容拆解为 4-8 步，每步包含真实可信的操作指令。
3. 基础工具必须包含以下 5 项（可根据视频额外增减）：底胶、烤灯、封层、海绵搓条、清洁液。每项必须写清用途和 1-3 个替代方案。
4. 专属色号列出该款式的所有颜色/材料，每项写清颜色描述和替代色号或叠加方案。
5. aiTranslation 用"。"分隔每个要点，给出 3-5 条精确可操作的专业步骤和注意事项。
6. 烤灯固化步骤的 isLampCure 必须为 true，时长 60-120 秒。
7. 每次对同一链接必须返回完全一致的内容。直接返回合法 JSON，不要任何额外文字。

返回格式（严格按此 JSON schema）：
{
  "videoTitle": "款式名称",
  "platform": "douyin",
  "style": "风格标签",
  "styleDescription": "一句话描述",
  "prep": [{ "title": "基础工具", "items": [{ "id": "xxx", "label": "名称", "hint": "描述", "type": "tool", "alternatives": [{ "label": "替代品", "desc": "说明", "emoji": "🔧" }] }] }, { "title": "专属色号", "items": [{ "id": "xxx", "label": "色号", "hint": "描述", "type": "color", "alternatives": [{ "label": "替代色", "desc": "说明", "emoji": "🎨" }] }] }],
  "steps": [{ "step": 1, "total": "...根据实际总步数填写", "title": "步骤名", "instruction": "主指令", "detail": "详细说明", "duration": 120, "isLampCure": false, "videoStart": 0, "originalQuote": "原博主口语说法", "aiTranslation": "AI精确翻译含⚠️" }]
}`;

export const parseTutorialLink = createServerFn({ method: "POST" })
  .inputValidator(z.object({ url: z.string().min(1), gender: z.enum(["all", "male"]).optional(), nailShape: z.string().optional() }))
  .handler(async ({ data }) => {
    const cacheKey = `${data.url.trim()}__${data.gender ?? "all"}__${data.nailShape ?? ""}`;
    if (_cache.has(cacheKey)) {
      return { ..._cache.get(cacheKey)!, videoUrl: data.url };
    }

    const genderHint = data.gender === "male"
      ? "这款美甲的目标用户是男性，请侧重推荐适合男性的风格：哑光质感、深色系、灰色系、裸色系、极简线条。"
      : "";

    const shapeHint = data.nailShape
      ? `用户选择了「${data.nailShape}」甲型，请在步骤中给出针对该甲型的特殊提示（如打磨角度、长度建议等）。`
      : "";

    try {
      const content = await chatCompletion(
        [
          { role: "system", content: "你是一个专业的美甲教程 AI 拆解助手。始终返回合法 JSON，不要有任何额外文字。每次对同一个视频链接必须生成完全一致的内容。" },
          { role: "user", content: `请分析这个美甲视频链接并生成 SOP：${data.url}\n\n${genderHint}\n${shapeHint}\n\n${PARSE_PROMPT}` },
        ],
        { temperature: 0.1, jsonMode: true },
      );
      const parsed = JSON.parse(content) as TutorialData;
      const result = { ...parsed, videoUrl: data.url };
      _cache.set(cacheKey, result);
      return result;
    } catch (err) {
      console.error("Qwen 解析失败，使用 Mock 数据:", err);
      return { ...DEMO_TUTORIAL, videoUrl: data.url };
    }
  });

// 内存缓存：同一链接下次直接返回
const _cache = new Map<string, TutorialData>();
