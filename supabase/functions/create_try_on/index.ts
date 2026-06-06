import { decodeBase64 } from "https://deno.land/std@0.224.0/encoding/base64.ts";
import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import { createImageEdit, createJsonResponse, hasOpenAiConfig } from "../_shared/openai.ts";
import type {
  CreateTryOnRequest,
  CreateTryOnResponse,
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

function prefixedError(code: string, message: string, extra?: unknown) {
  const suffix = extra == null ? "" : ` | ${stringifyError(extra)}`;
  return new Error(`${code}: ${message}${suffix}`);
}

function stringifyError(error: unknown) {
  if (error instanceof Error) return error.message;
  if (typeof error === "string") return error;
  if (typeof error === "object" && error !== null && "message" in error) {
    return String((error as { message: unknown }).message);
  }
  return JSON.stringify(error);
}

Deno.serve(async (req) => {
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  try {
    const body = (await req.json()) as CreateTryOnRequest;
    if (!body.session_id) {
      return jsonResponse({ error: "session_id is required" }, { status: 400 });
    }

    const supabase = getAdminClient();

    const { error: pendingError } = await supabase
      .from("sessions")
      .update({ status: "try_on_pending" })
      .eq("id", body.session_id);

    if (pendingError) {
      throw prefixedError("TRYON_SESSION_STATUS_UPDATE_FAILED", "更新会话状态失败", pendingError);
    }

    const { data: session, error: sessionError } = await supabase
      .from("sessions")
      .select("style_name, source_url")
      .eq("id", body.session_id)
      .single();
    if (sessionError) {
      throw prefixedError("TRYON_SESSION_READ_FAILED", "读取 session 失败", sessionError);
    }

    const { data: parseRecord, error: parseError } = await supabase
      .from("source_parses")
      .select("parse_json")
      .eq("session_id", body.session_id)
      .maybeSingle();
    if (parseError) {
      throw prefixedError("TRYON_PARSE_READ_FAILED", "读取款式解析结果失败", parseError);
    }

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

    const { data: handFile, error: handFileError } = await supabase.storage
      .from(BUCKET)
      .download(handAsset.storage_path);

    if (handFileError || !handFile) {
      throw prefixedError(
        "TRYON_HAND_ASSET_DOWNLOAD_FAILED",
        `下载手图失败，path=${handAsset.storage_path}`,
        handFileError ?? "empty file"
      );
    }

    const { data: tutorialAsset } = await supabase
      .from("session_assets")
      .select("storage_path")
      .eq("session_id", body.session_id)
      .eq("asset_type", "tutorial_frame")
      .order("created_at", { ascending: false })
      .limit(1)
      .maybeSingle();

    const handBytes = new Uint8Array(await handFile.arrayBuffer());
    const handBase64 = btoa(String.fromCharCode(...handBytes));
    let tutorialBase64 = "";
    if (tutorialAsset?.storage_path) {
      const { data: tutorialFile, error: tutorialFileError } = await supabase.storage
        .from(BUCKET)
        .download(tutorialAsset.storage_path);
      if (!tutorialFileError && tutorialFile) {
        const tutorialBytes = new Uint8Array(await tutorialFile.arrayBuffer());
        tutorialBase64 = btoa(String.fromCharCode(...tutorialBytes));
      }
    }

    const parseJson = parseRecord?.parse_json ?? {};
    const styleName = session?.style_name ?? "未命名款式";
    const renderPrompt = buildRenderPrompt(styleName, parseJson);

    const resultJson = hasOpenAiConfig()
      ? await createJsonResponse<TryOnPayload>({
        system:
          "你是一个电商级美甲试戴规划器。你会同时看到模板图和用户手图。你的任务是先分析模板款式在每根手指上的设计分布，再结合用户手图的手指方向与甲面位置，输出可直接给图像编辑模型使用的高精度中文 JSON。必须强调：只修改甲面，不修改肤色、手势、背景和光线。",
        user: JSON.stringify({
          style_name: styleName,
          source_url: session?.source_url ?? "",
          parse_json: parseJson,
          nail_position_hints: body.nail_position_hints ?? [],
        }),
        jsonSchema: {
          type: "object",
          additionalProperties: false,
          properties: {
            fit_summary: { type: "string" },
            tone_observation: { type: "string" },
            highlight_points: { type: "array", items: { type: "string" } },
            risk_points: { type: "array", items: { type: "string" } },
            render_variants: { type: "array", items: { type: "string" } },
            nail_layout_summary: { type: "string" },
            finger_design_map: {
              type: "array",
              items: {
                type: "object",
                additionalProperties: false,
                properties: {
                  finger: { type: "string" },
                  design: { type: "string" },
                  placement: { type: "string" },
                },
                required: ["finger", "design", "placement"],
              },
            },
            render_prompt: { type: "string" },
            note: { type: "string" },
          },
          required: [
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
        },
        imageInputs: listImageInputs(tutorialBase64, handBase64),
      }).catch((error) => {
        throw prefixedError("TRYON_PROMPT_PLAN_FAILED", "生成试戴 prompt 失败", error);
      })
      : {
        fit_summary: "这款偏暖调，适合大多数黄皮，整体会更显干净。",
        tone_observation: "建议保留透粉底色和柔和高光，不要把底色做得太冷。",
        highlight_points: ["通勤友好", "手部会显得更细长", "整体气质偏温柔"],
        risk_points: ["高闪过多会显得廉价", "法式边过宽会显手短"],
        render_variants: ["更暖一点", "更自然一点", "更透亮一点"],
        nail_layout_summary: "模板图整体是通透浅底，局部有高光和法式/装饰变化。",
        finger_design_map: [
          { finger: "thumb", design: "通透浅底", placement: "满甲" },
          { finger: "index", design: "法式或高光装饰", placement: "甲尖与中轴" },
          { finger: "middle", design: "主视觉高光", placement: "甲面中轴" },
          { finger: "ring", design: "同模板主色", placement: "满甲" },
          { finger: "pinky", design: "弱化装饰", placement: "甲尖" },
        ],
        render_prompt: renderPrompt,
        note: "Fallback try-on rendered without OPENAI_API_KEY.",
      };

    const imageBase64 = hasOpenAiConfig()
      ? await createImageEdit({
        prompt: `${resultJson.render_prompt}\n\n补充约束：${resultJson.nail_layout_summary}\n每根手指设计映射：${resultJson.finger_design_map.map((item) => `${item.finger}:${item.design}(${item.placement})`).join("；")}。\n用户甲面位置提示：${formatNailHints(body.nail_position_hints ?? [])}。请优先把设计落在这些甲面位置附近，只修改对应甲面区域。`,
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
      })
      : handBase64;

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
        uploadError
      );
    }

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

    const { error: readyError } = await supabase
      .from("sessions")
      .update({ status: "try_on_ready" })
      .eq("id", body.session_id);
    if (readyError) {
      throw prefixedError("TRYON_SESSION_READY_UPDATE_FAILED", "回写试戴完成状态失败", readyError);
    }

    const response: CreateTryOnResponse = {
      session_id: body.session_id,
      status: "try_on_ready",
    };

    return jsonResponse(response);
  } catch (error) {
    console.error("Function error:", error);
    const message = stringifyError(error);
    return jsonResponse(
      { error: message },
      { status: 500 },
    );
  }
});

function buildRenderPrompt(styleName: string, parseJson: Record<string, unknown>) {
  const tags = Array.isArray(parseJson?.style_tags) ? parseJson.style_tags.join("、") : "";
  const visuals = Array.isArray(parseJson?.visual_elements) ? parseJson.visual_elements.join("、") : "";

  return [
    "请把用户手图编辑成真实的美甲试戴图。",
    "你会同时参考模板图和用户手图。",
    "必须保留用户原始手势、手指数量、肤色、背景、透视和光线。",
    "只能修改可见甲面区域，不允许改动皮肤、桌面、电脑、手指结构。",
    `模板款式名称：${styleName}。`,
    tags ? `模板风格标签：${tags}。` : "",
    visuals ? `模板视觉元素：${visuals}。` : "",
    "请把模板图里的颜色关系、法式边、猫眼高光、跳色、饰品位置准确迁移到用户指甲上。",
    "生成效果要像电商试戴图，甲面贴合、边缘自然、反光克制、不要假手假甲。",
    "禁止新增戒指、手链、贴纸、水印、文字、额外手指或额外手部。",
  ].filter(Boolean).join(" ");
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

function formatNailHints(
  hints: Array<{
    finger: string;
    center_x: number;
    center_y: number;
    width_ratio: number;
    height_ratio: number;
    angle_deg: number;
  }>
) {
  if (!hints.length) return "none";
  return hints.map((hint) =>
    `${hint.finger}[cx=${hint.center_x.toFixed(3)}, cy=${hint.center_y.toFixed(3)}, w=${hint.width_ratio.toFixed(3)}, h=${hint.height_ratio.toFixed(3)}, angle=${hint.angle_deg.toFixed(1)}]`
  ).join("; ");
}
