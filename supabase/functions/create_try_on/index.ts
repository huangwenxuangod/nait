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
  render_prompt: string;
  note: string;
}

const BUCKET = "nail-it-assets";

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

    if (pendingError) throw pendingError;

    const { data: session } = await supabase
      .from("sessions")
      .select("style_name, source_url")
      .eq("id", body.session_id)
      .single();

    const { data: parseRecord } = await supabase
      .from("source_parses")
      .select("parse_json")
      .eq("session_id", body.session_id)
      .maybeSingle();

    const { data: handAsset } = await supabase
      .from("session_assets")
      .select("storage_path")
      .eq("session_id", body.session_id)
      .eq("asset_type", "hand_photo")
      .order("created_at", { ascending: false })
      .limit(1)
      .maybeSingle();

    if (!handAsset?.storage_path) {
      throw new Error("Missing hand photo asset");
    }

    const { data: handFile, error: handFileError } = await supabase.storage
      .from(BUCKET)
      .download(handAsset.storage_path);

    if (handFileError || !handFile) {
      throw handFileError ?? new Error("Failed to download hand photo");
    }

    const handBytes = new Uint8Array(await handFile.arrayBuffer());
    const handBase64 = btoa(String.fromCharCode(...handBytes));

    const parseJson = parseRecord?.parse_json ?? {};
    const styleName = session?.style_name ?? "未命名款式";
    const renderPrompt = buildRenderPrompt(styleName, parseJson);

    const resultJson = hasOpenAiConfig()
      ? await createJsonResponse<TryOnPayload>({
        system:
          "You are a nail try-on planner. Given a nail style and hand context, output concise Chinese JSON describing fit and a high quality image-edit prompt. The image edit should preserve the original hand and only add realistic nail design.",
        user: JSON.stringify({
          style_name: styleName,
          source_url: session?.source_url ?? "",
          parse_json: parseJson,
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
            render_prompt: { type: "string" },
            note: { type: "string" },
          },
          required: [
            "fit_summary",
            "tone_observation",
            "highlight_points",
            "risk_points",
            "render_variants",
            "render_prompt",
            "note",
          ],
        },
      })
      : {
        fit_summary: "这款偏暖调，适合大多数黄皮，整体会更显干净。",
        tone_observation: "建议保留透粉底色和柔和高光，不要把底色做得太冷。",
        highlight_points: ["通勤友好", "手部会显得更细长", "整体气质偏温柔"],
        risk_points: ["高闪过多会显得廉价", "法式边过宽会显手短"],
        render_variants: ["更暖一点", "更自然一点", "更透亮一点"],
        render_prompt: renderPrompt,
        note: "Fallback try-on rendered without OPENAI_API_KEY.",
      };

    const imageBase64 = hasOpenAiConfig()
      ? await createImageEdit({
        prompt: resultJson.render_prompt,
        imageBase64: handBase64,
        mimeType: handFile.type || "image/jpeg",
        fileName: handAsset.storage_path.split("/").pop() || "hand-photo.jpg",
      })
      : handBase64;

    const tryOnStoragePath = `${body.session_id}/try_on_result/${crypto.randomUUID()}.png`;
    const { error: uploadError } = await supabase.storage
      .from(BUCKET)
      .upload(tryOnStoragePath, decodeBase64(imageBase64), {
        contentType: "image/png",
        upsert: true,
      });

    if (uploadError) throw uploadError;

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

    if (upsertError) throw upsertError;

    await supabase
      .from("sessions")
      .update({ status: "try_on_ready" })
      .eq("id", body.session_id);

    const response: CreateTryOnResponse = {
      session_id: body.session_id,
      status: "try_on_ready",
    };

    return jsonResponse(response);
  } catch (error) {
    console.error("Function error:", error);
    const message = error instanceof Error
      ? error.message
      : typeof error === "object" && error !== null && "message" in error
        ? (error as any).message
        : JSON.stringify(error);
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
    "Edit the uploaded hand photo into a realistic nail try-on preview.",
    "Keep the original hand, skin tone, finger pose, background, and lighting unchanged.",
    "Only change the nail area.",
    `Apply nail design style: ${styleName}.`,
    tags ? `Style tags: ${tags}.` : "",
    visuals ? `Visual elements: ${visuals}.` : "",
    "Make the manicure photorealistic, elegant, e-commerce ready, polished, natural edge fit, consistent perspective, subtle reflections.",
    "Do not add rings, extra fingers, extra hands, text, stickers, watermarks, or unrelated decorations.",
  ].filter(Boolean).join(" ");
}
