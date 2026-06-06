import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import { createJsonResponse, hasOpenAiConfig } from "../_shared/openai.ts";
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
  note: string;
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

    if (pendingError) throw pendingError;

    const { data: session } = await supabase
      .from("sessions")
      .select("style_name, source_url")
      .eq("id", body.session_id)
      .single();

    const { data: handAsset } = await supabase
      .from("session_assets")
      .select("storage_path")
      .eq("session_id", body.session_id)
      .eq("asset_type", "hand_photo")
      .order("created_at", { ascending: false })
      .limit(1)
      .maybeSingle();

    const placeholderResult = hasOpenAiConfig()
      ? await createJsonResponse<TryOnPayload>({
        system:
          "You are a nail try-on planning assistant. The real pixel rendering is not available yet. Based on the style name and hand photo path, produce a practical fit analysis in concise Chinese JSON.",
        user:
          `style_name=${session?.style_name ?? "unknown"}; source_url=${session?.source_url ?? "unknown"}; hand_photo_path=${handAsset?.storage_path ?? "missing"}`,
        jsonSchema: {
          type: "object",
          additionalProperties: false,
          properties: {
            fit_summary: { type: "string" },
            tone_observation: { type: "string" },
            highlight_points: { type: "array", items: { type: "string" } },
            risk_points: { type: "array", items: { type: "string" } },
            render_variants: { type: "array", items: { type: "string" } },
            note: { type: "string" },
          },
          required: [
            "fit_summary",
            "tone_observation",
            "highlight_points",
            "risk_points",
            "render_variants",
            "note",
          ],
        },
      })
      : {
        fit_summary: "这款冰透猫眼适合暖黄皮，整体偏显白。",
        tone_observation: "建议底色略偏暖，避免过冷粉调。",
        highlight_points: ["透亮感强", "甲床有拉长效果", "适合日常通勤"],
        risk_points: ["磁吸角度过斜会显脏", "底色过冷可能显手黄"],
        render_variants: ["暖粉底版本", "偏裸色版本", "高闪版本"],
        note: "Fallback try-on analysis generated without OPENAI_API_KEY.",
      };

    const { error: upsertError } = await supabase.from("try_on_results").upsert({
      session_id: body.session_id,
      model: hasOpenAiConfig() ? "gpt-4.1-mini" : "fallback",
      version: "v1",
      result_image_path: null,
      result_json: placeholderResult,
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
    return jsonResponse(
      { error: error instanceof Error ? error.message : "Unknown error" },
      { status: 500 },
    );
  }
});
