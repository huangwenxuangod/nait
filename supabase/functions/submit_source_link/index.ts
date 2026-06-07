import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import { createRequestLogger, stringifyError } from "../_shared/logger.ts";
import { createQwenJsonResponse, hasQwenConfig } from "../_shared/qwen.ts";
import type {
  SubmitSourceLinkRequest,
  SubmitSourceLinkResponse,
} from "../_shared/types.ts";

// Statically import JSON presets so they are fully bundled by esbuild
import polarCatMatte from "./presets/polar-cat-matte.json" with { type: "json" };
import frenchPolarCat from "./presets/french-polar-cat.json" with { type: "json" };
import blushFireworkCat from "./presets/blush-firework-cat.json" with { type: "json" };
import polarCatFrench from "./presets/polar-cat-french.json" with { type: "json" };
import frenchGradientStruct from "./presets/french-gradient-struct.json" with { type: "json" };
import pureYellow from "./presets/pure-yellow.json" with { type: "json" };

const PRESETS_MAP: Record<string, any> = {
  "polar-cat-matte": polarCatMatte,
  "french-polar-cat": frenchPolarCat,
  "blush-firework-cat": blushFireworkCat,
  "polar-cat-french": polarCatFrench,
  "french-gradient-struct": frenchGradientStruct,
  "pure-yellow": pureYellow,
};

interface SourceParsePayload {
  style_name: string;
  style_tags: string[];
  visual_elements: string[];
  techniques: string[];
  total_steps: number;
  materials_hint: string[];
  steps: Array<{
    index: number;
    title: string;
    instruction: string;
    timer_seconds: number;
  }>;
  source_url: string;
  note: string;
}

const DEFAULT_PARSE: Omit<SourceParsePayload, "source_url" | "note"> = {
  style_name: "新手基础裸粉猫眼",
  style_tags: ["裸粉", "猫眼", "新手友好", "通勤"],
  visual_elements: ["透粉底色", "柔和高光", "干净甲面"],
  techniques: ["修甲", "底胶", "两层底色", "猫眼吸光", "封层固化"],
  total_steps: 5,
  materials_hint: ["底胶", "裸粉色胶", "猫眼胶", "磁铁", "封层", "烤灯"],
  steps: [
    { index: 1, title: "修甲清洁", instruction: "先修整甲型并清理甲面油脂。", timer_seconds: 30 },
    { index: 2, title: "薄涂底胶", instruction: "薄薄一层底胶，包边后照灯。", timer_seconds: 60 },
    { index: 3, title: "叠加底色", instruction: "薄涂两层裸粉底色，每层分别照灯。", timer_seconds: 60 },
    { index: 4, title: "做猫眼高光", instruction: "上猫眼胶后用磁铁吸出高光。", timer_seconds: 60 },
    { index: 5, title: "封层完成", instruction: "最后上一层封层并完整固化。", timer_seconds: 90 },
  ],
};

Deno.serve(async (req) => {
  const logger = createRequestLogger("submit_source_link");
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  try {
    const body = (await req.json()) as SubmitSourceLinkRequest;
    logger.log("request_start", {
      session_id: body.session_id,
      source_url: body.source_url,
    });
    if (!body.session_id || !body.source_url) {
      logger.warn("validation_failed", { reason: "session_id or source_url missing" });
      return jsonResponse({ error: "session_id and source_url are required" }, { status: 400 });
    }

    const supabase = getAdminClient();
    logger.log("db_update_session_start", { status: "source_parsing" });
    const { data, error } = await supabase
      .from("sessions")
      .update({
        source_url: body.source_url,
        status: "source_parsing",
      })
      .eq("id", body.session_id)
      .select("id, status")
      .single();

    if (error || !data) {
      throw error ?? new Error("Failed to submit source link");
    }
    logger.log("db_update_session_done", {
      session_id: data.id,
      status: data.status,
    });

    let mockParse: SourceParsePayload;

    if (body.source_url.startsWith("preset://")) {
      const presetId = body.source_url.replace("preset://", "");
      logger.log("parse_mode_preset", { preset_id: presetId });
      const presetData = PRESETS_MAP[presetId];
      if (presetData) {
        mockParse = presetData;
      } else {
        logger.warn("preset_missing_fallback", { preset_id: presetId });
        mockParse = {
          style_name: "极光冰透猫眼",
          style_tags: ["极光猫眼", "冰透水光", "暖调显白"],
          visual_elements: ["粉透底色", "银色高光", "猫眼斜吸"],
          techniques: ["底胶打底", "两层透色叠涂", "磁铁斜吸", "封层固化"],
          total_steps: 4,
          materials_hint: ["底胶", "透粉色胶", "猫眼胶", "磁铁", "封层"],
          steps: [
            { index: 1, title: "修甲并打底", instruction: "修整甲型后薄涂底胶。", timer_seconds: 30 },
            { index: 2, title: "叠加底色", instruction: "薄涂两层透粉底色，每层均匀照灯。", timer_seconds: 60 },
            { index: 3, title: "做猫眼光", instruction: "上猫眼胶后用磁铁斜吸 45 度。", timer_seconds: 60 },
            { index: 4, title: "封层完成", instruction: "加亮面封层并完成最终固化。", timer_seconds: 90 },
          ],
          source_url: body.source_url,
          note: "Fallback parse generated due to missing preset file.",
        };
      }
    } else {
      logger.log("parse_mode_llm", {
        qwen_enabled: hasQwenConfig(),
      });
      const localFallback = buildLocalFallbackParse(body.source_url);
      mockParse = hasQwenConfig()
        ? await createQwenJsonResponse<SourceParsePayload>({
          system:
            "你是一个美甲教程解析器。根据短视频链接或预设标识，推断可能的美甲风格、视觉元素、关键工艺、步骤、计时和材料。只返回严格 JSON，不要解释。",
          user: `请解析这个来源并输出 JSON：${body.source_url}`,
          requiredKeys: [
            "style_name",
            "style_tags",
            "visual_elements",
            "techniques",
            "total_steps",
            "materials_hint",
            "steps",
            "source_url",
            "note",
          ],
        }).catch((error) => {
          logger.warn("parse_llm_failed_fallback", { error: stringifyError(error) });
          return localFallback;
        })
        : localFallback;
    }

    logger.log("db_upsert_source_parse_start", {
      style_name: mockParse.style_name,
      total_steps: mockParse.total_steps,
      materials_count: mockParse.materials_hint?.length ?? 0,
    });
    await supabase.from("source_parses").upsert({
      session_id: body.session_id,
      model: hasQwenConfig() ? "qwen3.7-plus" : "fallback",
      version: "v1",
      parse_json: mockParse,
    });
    logger.log("db_upsert_source_parse_done");

    logger.log("db_finalize_session_start", {
      status: "source_parsed",
      style_name: mockParse.style_name,
    });
    await supabase
      .from("sessions")
      .update({
        status: "source_parsed",
        style_name: mockParse.style_name,
      })
      .eq("id", body.session_id);
    logger.log("db_finalize_session_done");

    const response: SubmitSourceLinkResponse = {
      session_id: data.id,
      status: "source_parsing",
    };

    logger.done("ok", {
      session_id: data.id,
      parsed_style_name: mockParse.style_name,
      returned_status: "source_parsing",
    });
    return jsonResponse(response);
});

function buildLocalFallbackParse(sourceUrl: string): SourceParsePayload {
  const text = sourceUrl.toLowerCase();
  const styleTags = [...DEFAULT_PARSE.style_tags];
  const visualElements = [...DEFAULT_PARSE.visual_elements];
  const techniques = [...DEFAULT_PARSE.techniques];
  const materialsHint = [...DEFAULT_PARSE.materials_hint];
  let styleName = DEFAULT_PARSE.style_name;

  if (text.includes("法式")) {
    styleName = "新手法式裸粉款";
    styleTags.unshift("法式");
    visualElements.push("细法式边");
  }
  if (text.includes("猫眼")) {
    styleTags.unshift("猫眼");
    visualElements.push("磁吸高光");
  }
  if (text.includes("裸粉") || text.includes("豆沙")) {
    styleTags.unshift("裸粉");
    visualElements.push("温柔粉调");
  }
  if (text.includes("渐变")) {
    styleTags.push("渐变");
    visualElements.push("轻渐变过渡");
  }
  if (text.includes("新手")) {
    styleTags.unshift("新手友好");
  }
  if (text.includes("教程")) {
    techniques.push("按步骤固化");
  }

  return {
    style_name: styleName,
    style_tags: Array.from(new Set(styleTags)).slice(0, 6),
    visual_elements: Array.from(new Set(visualElements)).slice(0, 6),
    techniques: Array.from(new Set(techniques)).slice(0, 6),
    total_steps: DEFAULT_PARSE.total_steps,
    materials_hint: Array.from(new Set(materialsHint)).slice(0, 8),
    steps: DEFAULT_PARSE.steps,
    source_url: sourceUrl,
    note: "Local fallback parse generated from source text keywords.",
  };
}
