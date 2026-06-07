import { decodeBase64 } from "https://deno.land/std@0.224.0/encoding/base64.ts";
import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import { createRequestLogger, stringifyError } from "../_shared/logger.ts";
import { createImageEdit, hasOpenAiConfig } from "../_shared/openai.ts";
import {
  createQwenStructuredFromText,
  createQwenVisionAnalysis,
  hasQwenConfig,
} from "../_shared/qwen.ts";
import type {
  RenderTryOnRequest,
  RenderTryOnResponse,
} from "../_shared/types.ts";

interface TryOnPayload {
  fit_summary: string;
  tone_observation: string;
  highlight_points: string[];
  risk_points: string[];
  render_variants: string[];
  nail_layout_summary: string;
  finger_design_map: Array<{
    finger: string;
    design: string;
    placement: string;
  }>;
  render_prompt: string;
  note: string;
}

const BUCKET = "nail-it-assets";
const TRY_ON_PLAN_MODE = (Deno.env.get("TRY_ON_PLAN_MODE") ?? "hardcoded").trim().toLowerCase();
const DEFAULT_STYLE_FAMILY = "soft_cat_eye";

function prefixedError(code: string, message: string, extra?: unknown) {
  const suffix = extra == null ? "" : ` | ${stringifyError(extra)}`;
  return new Error(`${code}: ${message}${suffix}`);
}

Deno.serve(async (req) => {
  const logger = createRequestLogger("render_try_on");
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  let requestBody: RenderTryOnRequest | null = null;
  try {
    const body = (await req.json()) as RenderTryOnRequest;
    requestBody = body;
    logger.log("request_start", {
      session_id: body.session_id,
      nail_hint_count: body.nail_position_hints?.length ?? 0,
      qwen_enabled: hasQwenConfig(),
      openai_enabled: hasOpenAiConfig(),
    });

    if (!body.session_id) {
      logger.warn("validation_failed", { reason: "session_id missing" });
      return jsonResponse({ error: "session_id is required" }, { status: 400 });
    }

    const supabase = getAdminClient();

    logger.log("step_1_read_session_start");
    const { data: session, error: sessionError } = await supabase
      .from("sessions")
      .select("style_name, source_url")
      .eq("id", body.session_id)
      .single();
    if (sessionError) {
      throw prefixedError("TRYON_SESSION_READ_FAILED", "读取 session 失败", sessionError);
    }
    logger.log("step_1_read_session_done", {
      style_name: session?.style_name ?? null,
      source_url: session?.source_url ?? null,
    });

    logger.log("step_2_read_parse_start");
    const { data: parseRecord, error: parseError } = await supabase
      .from("source_parses")
      .select("parse_json")
      .eq("session_id", body.session_id)
      .maybeSingle();
    if (parseError) {
      throw prefixedError("TRYON_PARSE_READ_FAILED", "读取款式解析结果失败", parseError);
    }
    logger.log("step_2_read_parse_done", {
      has_parse: !!parseRecord?.parse_json,
    });

    logger.log("step_3_find_hand_asset_start");
    const { data: handAsset, error: handAssetError } = await supabase
      .from("session_assets")
      .select("storage_path")
      .eq("session_id", body.session_id)
      .eq("asset_type", "hand_photo")
      .order("created_at", { ascending: false })
      .limit(1)
      .maybeSingle();
    if (handAssetError) {
      throw prefixedError("TRYON_HAND_ASSET_QUERY_FAILED", "查询手图资产失败", handAssetError);
    }
    if (!handAsset?.storage_path) {
      throw prefixedError("TRYON_HAND_ASSET_MISSING", "缺少 hand_photo 资产");
    }
    logger.log("step_3_find_hand_asset_done", {
      hand_storage_path: handAsset.storage_path,
    });

    logger.log("step_4_download_hand_image_start");
    const { data: handFile, error: handFileError } = await supabase.storage
      .from(BUCKET)
      .download(handAsset.storage_path);
    if (handFileError || !handFile) {
      throw prefixedError(
        "TRYON_HAND_ASSET_DOWNLOAD_FAILED",
        `下载手图失败，path=${handAsset.storage_path}`,
        handFileError ?? "empty file",
      );
    }
    const handBytes = new Uint8Array(await handFile.arrayBuffer());
    const handBase64 = btoa(String.fromCharCode(...handBytes));
    logger.log("step_4_download_hand_image_done", {
      mime_type: handFile.type || "image/jpeg",
      hand_bytes: handBytes.length,
      hand_base64_length: handBase64.length,
    });

    const parseJson = parseRecord?.parse_json ?? {};
    const styleName = session?.style_name ?? "未命名款式";
    const sourceUrl = session?.source_url ?? "";
    const styleFamily = detectStyleFamily(styleName, sourceUrl, parseJson);
    const hardcodedPlan = buildHardcodedTryOnPlan({
      styleName,
      sourceUrl,
      parseJson,
      styleFamily,
    });
    const useHardcodedPlan = shouldUseHardcodedPlan({
      styleName,
      sourceUrl,
      parseJson,
      planMode: TRY_ON_PLAN_MODE,
    });

    let tutorialBase64 = "";
    if (!useHardcodedPlan) {
      logger.log("step_5_download_template_start");
      const { data: tutorialAsset } = await supabase
        .from("session_assets")
        .select("storage_path")
        .eq("session_id", body.session_id)
        .eq("asset_type", "tutorial_frame")
        .order("created_at", { ascending: false })
        .limit(1)
        .maybeSingle();

      if (tutorialAsset?.storage_path) {
        const { data: tutorialFile, error: tutorialFileError } = await supabase.storage
          .from(BUCKET)
          .download(tutorialAsset.storage_path);
        if (!tutorialFileError && tutorialFile) {
          const tutorialBytes = new Uint8Array(await tutorialFile.arrayBuffer());
          tutorialBase64 = btoa(String.fromCharCode(...tutorialBytes));
        }
      }
      logger.log("step_5_download_template_done", {
        tutorial_base64_length: tutorialBase64.length,
      });
    } else {
      logger.log("step_5_download_template_skipped", {
        reason: "hardcoded_plan",
        style_family: styleFamily,
      });
    }

    logger.log("step_6_plan_prompt_start", {
      style_name: styleName,
      parse_keys: Object.keys(parseJson ?? {}),
      plan_mode: TRY_ON_PLAN_MODE,
      use_hardcoded_plan: useHardcodedPlan,
      style_family: styleFamily,
    });
    let resultJson: TryOnPayload;
    if (useHardcodedPlan) {
      resultJson = hardcodedPlan;
      logger.log("step_6_hardcoded_plan_used", {
        style_family: styleFamily,
        note: resultJson.note,
      });
    } else if (hasQwenConfig()) {
      resultJson = await buildTryOnPlanWithQwenTwoStage({
        styleName,
        sourceUrl,
        parseJson,
        nailPositionHints: body.nail_position_hints ?? [],
        tutorialBase64,
        handBase64,
        logger,
      }).catch((error) => {
        logger.warn("step_6_qwen_plan_failed_fallback", {
          error: stringifyError(error),
          style_family: styleFamily,
        });
        return hardcodedPlan;
      });
      logger.log("step_6_plan_prompt_done", {
        planner: "qwen_two_stage",
        variant_count: resultJson.render_variants?.length ?? 0,
      });
    } else {
      resultJson = hardcodedPlan;
      logger.warn("step_6_plan_prompt_fallback", {
        reason: "qwen_unavailable_or_disabled",
        style_family: styleFamily,
      });
    }

    if (!hasOpenAiConfig()) {
      throw prefixedError(
        "TRYON_IMAGE_GENERATION_FAILED",
        "云端未配置 OPENAI_API_KEY / 代理 Key，请在服务器 .env 中配置并重启 functions 容器！",
      );
    }

    const fingerDesignMap = Array.isArray(resultJson?.finger_design_map) ? resultJson.finger_design_map : [];
    const nailLayoutSummary = resultJson?.nail_layout_summary ?? "";
    const renderPromptText = resultJson?.render_prompt ?? hardcodedPlan.render_prompt;
    const finalPrompt = buildFinalImagePrompt({
      renderPromptText,
      nailLayoutSummary,
      fingerDesignMap,
      nailPositionHints: body.nail_position_hints ?? [],
    });

    logger.log("step_7_image_edit_start", {
      image_model: Deno.env.get("OPENAI_IMAGE_MODEL") ?? "gpt-image-2",
      prompt_length: finalPrompt.length,
    });
    const imageBase64 = await createImageEdit({
      prompt: finalPrompt,
      imageBase64: handBase64,
      mimeType: handFile.type || "image/jpeg",
      fileName: handAsset.storage_path.split("/").pop() || "hand-photo.jpg",
      imageInputs: [
        {
          imageBase64: handBase64,
          mimeType: handFile.type || "image/jpeg",
          fileName: handAsset.storage_path.split("/").pop() || "hand-photo.jpg",
        },
        ...(
          tutorialBase64
            ? [{
                imageBase64: tutorialBase64,
                mimeType: "image/jpeg",
                fileName: "tutorial-reference.jpg",
              }]
            : []
        ),
      ],
    }).catch((error) => {
      throw prefixedError("TRYON_IMAGE_GENERATION_FAILED", "生成试戴图片失败", error);
    });
    logger.log("step_7_image_edit_done", {
      image_base64_length: imageBase64.length,
    });

    logger.log("step_8_storage_upload_start");
    const tryOnStoragePath = `${body.session_id}/try_on_result/${crypto.randomUUID()}.png`;
    const { error: uploadError } = await supabase.storage
      .from(BUCKET)
      .upload(tryOnStoragePath, decodeBase64(imageBase64), {
        contentType: "image/png",
        upsert: true,
      });
    if (uploadError) {
      throw prefixedError(
        "TRYON_RESULT_UPLOAD_FAILED",
        `上传试戴结果失败，path=${tryOnStoragePath}`,
        uploadError,
      );
    }
    logger.log("step_8_storage_upload_done", {
      result_image_path: tryOnStoragePath,
    });

    logger.log("step_9_db_upsert_result_start");
    const { error: upsertError } = await supabase.from("try_on_results").upsert({
      session_id: body.session_id,
      model: hasOpenAiConfig() ? "gpt-image-2" : "fallback",
      version: "v2",
      result_image_path: tryOnStoragePath,
      result_json: {
        ...resultJson,
        result_image_path: tryOnStoragePath,
      },
    });
    if (upsertError) {
      throw prefixedError("TRYON_RESULT_UPSERT_FAILED", "写入 try_on_results 失败", upsertError);
    }
    logger.log("step_9_db_upsert_result_done");

    logger.log("step_10_db_update_session_start", { status: "try_on_ready" });
    const { error: readyError } = await supabase
      .from("sessions")
      .update({ status: "try_on_ready" })
      .eq("id", body.session_id);
    if (readyError) {
      throw prefixedError("TRYON_SESSION_READY_UPDATE_FAILED", "回写试戴完成状态失败", readyError);
    }
    logger.log("step_10_db_update_session_done", { status: "try_on_ready" });

    const response: RenderTryOnResponse = {
      session_id: body.session_id,
      status: "try_on_ready",
    };
    logger.done("ok", {
      session_id: body.session_id,
      status: "try_on_ready",
      result_image_path: tryOnStoragePath,
    });
    return jsonResponse(response);
  } catch (error) {
    const message = stringifyError(error);
    logger.error("request_failed", { error: message });
    if (requestBody?.session_id) {
      const supabase = getAdminClient();
      logger.log("failure_status_writeback_start", {
        session_id: requestBody.session_id,
        status: "failed",
      });
      await supabase
        .from("sessions")
        .update({ status: "failed" })
        .eq("id", requestBody.session_id);
      logger.log("failure_status_writeback_done", {
        session_id: requestBody.session_id,
        status: "failed",
      });
    }
});

async function buildTryOnPlanWithQwenTwoStage({
  styleName,
  sourceUrl,
  parseJson,
  nailPositionHints,
  tutorialBase64,
  handBase64,
  logger,
}: {
  styleName: string;
  sourceUrl: string;
  parseJson: Record<string, unknown>;
  nailPositionHints: Array<{
    finger: string;
    center_x: number;
    center_y: number;
    width_ratio: number;
    height_ratio: number;
    angle_deg: number;
  }>;
  tutorialBase64: string;
  handBase64: string;
  logger: ReturnType<typeof createRequestLogger>;
}): Promise<TryOnPayload> {
  logger.log("step_6a_vision_analysis_start");
  const visionAnalysis = await createQwenVisionAnalysis({
    system:
      "你是电商级美甲视觉分析助手。你会同时看到模板图和用户手图。请先识别模板图的配色、法式边、猫眼高光、装饰分布，并结合用户手图判断每根手指甲面的大致适配方式。只输出中文分析文本，不要输出 JSON，不要寒暄。",
    user: [
      `款式名称：${styleName}`,
      sourceUrl ? `来源链接：${sourceUrl}` : "",
      `已有解析：${JSON.stringify(parseJson)}`,
      `甲面位置提示：${formatNailHints(nailPositionHints)}`,
      "请依次输出：1. 款式总结 2. 每根手指建议设计 3. 生图约束 4. 风险点",
    ].filter(Boolean).join("\n"),
    imageInputs: listImageInputs(tutorialBase64, handBase64),
  });
  logger.log("step_6a_vision_analysis_done", {
    analysis_length: visionAnalysis.length,
    analysis_preview: visionAnalysis.slice(0, 180),
  });

  logger.log("step_6b_text_to_json_start");
  const result = await createQwenStructuredFromText<TryOnPayload>({
    system:
      "你是一个美甲试戴 JSON 结构化助手。你会收到上一步的视觉分析文本。你的任务是把它压成严格 JSON 对象，只输出 JSON，不要解释，不要 markdown，不要多余文字。",
    user: JSON.stringify({
      style_name: styleName,
      source_url: sourceUrl,
      parse_json: parseJson,
      nail_position_hints: nailPositionHints,
      vision_analysis: visionAnalysis,
      output_requirements: {
        fit_summary: "一句总结这款是否适合用户",
        tone_observation: "一句色调建议",
        highlight_points: "数组，列出亮点",
        risk_points: "数组，列出风险点",
        render_variants: "数组，列出建议微调方向",
        nail_layout_summary: "一句概述每根手指整体分布",
        finger_design_map: "数组，每项包含 finger design placement",
        render_prompt: "给图像编辑模型的中文提示词",
        note: "补充说明",
      },
    }),
    requiredKeys: [
      "fit_summary",
      "tone_observation",
      "highlight_points",
      "risk_points",
      "render_variants",
      "nail_layout_summary",
      "finger_design_map",
      "render_prompt",
      "note",
    ],
  });
  logger.log("step_6b_text_to_json_done", {
    variant_count: result.render_variants?.length ?? 0,
    finger_design_count: result.finger_design_map?.length ?? 0,
  });
  return result;
}

function listImageInputs(tutorialBase64: string, handBase64: string) {
  const images = [
    {
      imageBase64: handBase64,
      mimeType: "image/jpeg",
    },
  ];
  if (tutorialBase64) {
    images.unshift({
      imageBase64: tutorialBase64,
      mimeType: "image/jpeg",
    });
  }
  return images;
}

function shouldUseHardcodedPlan({
  styleName,
  sourceUrl,
  parseJson,
  planMode,
}: {
  styleName: string;
  sourceUrl: string;
  parseJson: Record<string, unknown>;
  planMode: string;
}) {
  if (planMode !== "qwen") return true;
  if (!styleName || styleName.includes("未命名")) return true;
  if (!hasUsefulParse(parseJson)) return true;
  if (!sourceUrl) return true;
  return false;
}

function hasUsefulParse(parseJson: Record<string, unknown>) {
  const tags = asStringArray(parseJson.style_tags);
  const visuals = asStringArray(parseJson.visual_elements);
  const steps = Array.isArray(parseJson.steps) ? parseJson.steps.length : 0;
  return tags.length > 0 || visuals.length > 0 || steps > 0;
}

function detectStyleFamily(
  styleName: string,
  sourceUrl: string,
  parseJson: Record<string, unknown>,
) {
  const haystack = [
    styleName,
    sourceUrl,
    ...asStringArray(parseJson.style_tags),
    ...asStringArray(parseJson.visual_elements),
  ].join(" ").toLowerCase();

  if (haystack.includes("法式") && haystack.includes("猫眼")) return "french_cat_eye";
  if (haystack.includes("法式")) return "french_clean";
  if (haystack.includes("极光") || haystack.includes("烟花")) return "aurora_glow";
  if (haystack.includes("裸粉") || haystack.includes("豆沙")) return "soft_nude";
  if (haystack.includes("纯欲") || haystack.includes("透粉")) return "soft_nude";
  return DEFAULT_STYLE_FAMILY;
}

function buildHardcodedTryOnPlan({
  styleName,
  sourceUrl,
  parseJson,
  styleFamily,
}: {
  styleName: string;
  sourceUrl: string;
  parseJson: Record<string, unknown>;
  styleFamily: string;
}): TryOnPayload {
  const tags = asStringArray(parseJson.style_tags);
  const visuals = asStringArray(parseJson.visual_elements);
  const displayName = styleName && !styleName.includes("未命名") ? styleName : "通透裸粉猫眼";
  const styleSummary = tags.concat(visuals).slice(0, 6).join("、");
  const familyCopy = getStyleFamilyCopy(styleFamily);

  return {
    fit_summary: familyCopy.fitSummary,
    tone_observation: familyCopy.toneObservation,
    highlight_points: familyCopy.highlightPoints,
    risk_points: familyCopy.riskPoints,
    render_variants: familyCopy.renderVariants,
    nail_layout_summary: familyCopy.nailLayoutSummary,
    finger_design_map: familyCopy.fingerDesignMap,
    render_prompt: [
      "请把这张真实手部照片编辑成电商级美甲试戴结果图。",
      "只允许修改甲面，不允许修改手型、手势、皮肤纹理、背景、桌面、光线和拍摄角度。",
      `款式名：${displayName}。`,
      styleSummary ? `风格关键词：${styleSummary}。` : "",
      sourceUrl ? `来源提示：${sourceUrl}。` : "",
      familyCopy.promptCore,
      "甲面边缘必须贴合真实指甲轮廓，反光自然，不能出现假甲悬浮、皮肤染色、额外手指、额外饰品、文字水印。",
    ].filter(Boolean).join(" "),
    note: `Hardcoded try-on plan used for ${styleFamily}.`,
  };
}

function getStyleFamilyCopy(styleFamily: string) {
  switch (styleFamily) {
    case "french_cat_eye":
      return {
        fitSummary: "这类细法式加猫眼高光更适合做成干净、显手长的效果。",
        toneObservation: "底色保持奶透粉，猫眼高光不要太冷太亮。",
        highlightPoints: ["显手指修长", "近距离看更精致", "通勤和约会都能成立"],
        riskPoints: ["法式边过宽会显厚重", "高光太白会显脏灰"],
        renderVariants: ["法式边更细", "猫眼更柔一点", "整体更奶透一点"],
        nailLayoutSummary: "以奶透底色为主，食指中指增加法式和猫眼主高光，无名指呼应主色。",
        fingerDesignMap: [
          { finger: "thumb", design: "奶透粉底色", placement: "满甲" },
          { finger: "index", design: "细法式边加轻猫眼", placement: "甲尖与中轴" },
          { finger: "middle", design: "主猫眼高光", placement: "甲面中轴" },
          { finger: "ring", design: "奶透底色加弱法式", placement: "满甲与甲尖" },
          { finger: "pinky", design: "极细法式边", placement: "甲尖" },
        ],
        promptCore: "整体做成奶透裸粉底色，保留细法式白边和柔和猫眼高光，重点放在食指和中指，风格克制、精致、显手白。",
      };
    case "french_clean":
      return {
        fitSummary: "细法式本身就很稳定，重点是控制边缘宽度和底色通透度。",
        toneObservation: "底色更偏奶透裸粉，白边保持细且整齐。",
        highlightPoints: ["最不挑肤色", "试戴容错高", "看起来干净高级"],
        riskPoints: ["白边过粗会变笨重", "底色太白会显假"],
        renderVariants: ["更细白边", "更透明底色", "更圆润甲型"],
        nailLayoutSummary: "整体统一奶透底，所有手指使用细法式边，个别手指略加强对比。",
        fingerDesignMap: [
          { finger: "thumb", design: "奶透法式", placement: "甲尖" },
          { finger: "index", design: "奶透法式", placement: "甲尖" },
          { finger: "middle", design: "奶透法式", placement: "甲尖" },
          { finger: "ring", design: "奶透法式", placement: "甲尖" },
          { finger: "pinky", design: "奶透法式", placement: "甲尖" },
        ],
        promptCore: "把所有指甲编辑成奶透裸粉底色配细白法式边，边缘整洁锐利但不要过宽，整体像高级日常款。",
      };
    case "aurora_glow":
      return {
        fitSummary: "极光和烟花类效果更看重局部高光位置，做好会很出片。",
        toneObservation: "底色尽量透，亮片和极光高光控制在局部，不要整片发白。",
        highlightPoints: ["出片感强", "有明显试戴惊艳感", "电商展示效果更好"],
        riskPoints: ["高光过满会显廉价", "冷白闪片过多会压黄皮"],
        renderVariants: ["更暖一点", "闪度低一点", "局部高光更集中"],
        nailLayoutSummary: "透底配局部极光高光，中指无名指作为主视觉，其他手指弱化处理。",
        fingerDesignMap: [
          { finger: "thumb", design: "透底微光", placement: "满甲" },
          { finger: "index", design: "轻极光偏光", placement: "甲面上半部" },
          { finger: "middle", design: "主极光高光", placement: "中轴偏斜" },
          { finger: "ring", design: "主视觉闪光", placement: "中轴与甲尖" },
          { finger: "pinky", design: "弱化极光", placement: "甲尖" },
        ],
        promptCore: "做成透感底色加局部极光或烟花高光，中指和无名指最亮，其余手指只做弱化呼应，避免廉价满闪。",
      };
    case "soft_nude":
      return {
        fitSummary: "裸粉豆沙类最适合先打可靠结果，重点是自然贴甲和肤色协调。",
        toneObservation: "用暖豆沙或透粉，不要偏灰紫。",
        highlightPoints: ["最适合黄皮", "看起来温柔", "失败风险最低"],
        riskPoints: ["底色太实会显闷", "偏灰会显手脏"],
        renderVariants: ["更透一点", "更豆沙一点", "加一点微光"],
        nailLayoutSummary: "整体统一暖豆沙透粉底色，个别手指加轻微水光或细闪。",
        fingerDesignMap: [
          { finger: "thumb", design: "暖豆沙透粉", placement: "满甲" },
          { finger: "index", design: "暖豆沙透粉", placement: "满甲" },
          { finger: "middle", design: "透粉加微光", placement: "甲面中轴" },
          { finger: "ring", design: "暖豆沙透粉", placement: "满甲" },
          { finger: "pinky", design: "暖豆沙透粉", placement: "满甲" },
        ],
        promptCore: "整体做成暖豆沙或透粉裸色甲面，质感自然通透，微光只放在中指附近，保持电商试戴的真实感。",
      };
    default:
      return {
        fitSummary: "这款适合先做成通透、干净、偏暖的保守版试戴结果，保证稳定可看。",
        toneObservation: "优先暖透粉底色，局部高光克制处理。",
        highlightPoints: ["最稳妥", "不容易翻车", "方便继续推进后续 SOP"],
        riskPoints: ["装饰太多会偏假", "高光过强会显廉价"],
        renderVariants: ["更自然", "更暖一点", "更透亮一点"],
        nailLayoutSummary: "整体通透浅底，中指主高光，其他手指做弱装饰呼应。",
        fingerDesignMap: [
          { finger: "thumb", design: "通透浅底", placement: "满甲" },
          { finger: "index", design: "轻装饰", placement: "甲尖与中轴" },
          { finger: "middle", design: "主高光", placement: "甲面中轴" },
          { finger: "ring", design: "同主色弱化版", placement: "满甲" },
          { finger: "pinky", design: "极弱装饰", placement: "甲尖" },
        ],
        promptCore: "把所有指甲做成通透暖粉底色，中指保留主高光，其余手指只做弱化装饰，效果真实、克制、像电商试戴图。",
      };
  }
}

function buildFinalImagePrompt({
  renderPromptText,
  nailLayoutSummary,
  fingerDesignMap,
  nailPositionHints,
}: {
  renderPromptText: string;
  nailLayoutSummary: string;
  fingerDesignMap: Array<{
    finger: string;
    design: string;
    placement: string;
  }>;
  nailPositionHints: Array<{
    finger: string;
    center_x: number;
    center_y: number;
    width_ratio: number;
    height_ratio: number;
    angle_deg: number;
  }>;
}) {
  const mappedDesigns = fingerDesignMap
    .map((item) => `${item?.finger ?? ""}:${item?.design ?? ""}(${item?.placement ?? ""})`)
    .join("；");
  const hintText = formatNailHints(nailPositionHints);

  return [
    renderPromptText,
    nailLayoutSummary ? `布局：${nailLayoutSummary}。` : "",
    mappedDesigns ? `手指映射：${mappedDesigns}。` : "",
    hintText !== "none" ? `甲面定位提示：${hintText}。` : "",
    "必须只在真实甲面范围内编辑，边缘紧贴甲床，禁止改动皮肤和背景。",
  ].filter(Boolean).join("\n");
}

function asStringArray(value: unknown) {
  if (!Array.isArray(value)) return [];
  return value.filter((item): item is string => typeof item === "string" && item.trim().length > 0);
}

function formatNailHints(
  hints: Array<{
    finger: string;
    center_x: number;
    center_y: number;
    width_ratio: number;
    height_ratio: number;
    angle_deg: number;
  }>,
) {
  if (!hints.length) return "none";
  return hints.map((hint) =>
    `${hint.finger}[cx=${hint.center_x.toFixed(3)}, cy=${hint.center_y.toFixed(3)}, w=${hint.width_ratio.toFixed(3)}, h=${hint.height_ratio.toFixed(3)}, angle=${hint.angle_deg.toFixed(1)}]`
  ).join("; ");
}
