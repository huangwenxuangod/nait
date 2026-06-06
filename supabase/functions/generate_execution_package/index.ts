import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import { createJsonResponse, hasOpenAiConfig } from "../_shared/openai.ts";
import type {
  GenerateExecutionPackageRequest,
  GenerateExecutionPackageResponse,
} from "../_shared/types.ts";

interface BomPayload {
  basic_tools: string[];
  style_specific_items: string[];
  optional_substitutes: string[];
  warnings: string[];
}

interface SopPayload {
  steps: Array<{
    index: number;
    title: string;
    instruction: string;
    media_asset_path: string | null;
    timer_seconds: number;
    voice_shortcut: string;
  }>;
  completion_tip: string;
}

Deno.serve(async (req) => {
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  try {
    const body = (await req.json()) as GenerateExecutionPackageRequest;
    if (!body.session_id) {
      return jsonResponse({ error: "session_id is required" }, { status: 400 });
    }

    const supabase = getAdminClient();

    const { data: parseRecord } = await supabase
      .from("source_parses")
      .select("parse_json")
      .eq("session_id", body.session_id)
      .single();

    const parseJson = parseRecord?.parse_json ?? {};
    const parseString = JSON.stringify(parseJson);

    const bom = hasOpenAiConfig()
      ? await createJsonResponse<BomPayload>({
        system:
          "You generate nail-art BOM lists in concise Chinese. Infer base tools, style items, substitutes, and warnings from the parsed tutorial JSON. Return JSON only.",
        user: parseString,
        jsonSchema: {
          type: "object",
          additionalProperties: false,
          properties: {
            basic_tools: { type: "array", items: { type: "string" } },
            style_specific_items: { type: "array", items: { type: "string" } },
            optional_substitutes: { type: "array", items: { type: "string" } },
            warnings: { type: "array", items: { type: "string" } },
          },
          required: [
            "basic_tools",
            "style_specific_items",
            "optional_substitutes",
            "warnings",
          ],
        },
      })
      : {
        basic_tools: ["底胶", "封层", "UV/LED 烤灯"],
        style_specific_items: ["透粉底色胶", "银色猫眼胶", "双头磁铁"],
        optional_substitutes: ["裸粉底色可替代透粉色", "高光封层可替代钢化封层"],
        warnings: ["磁吸前不要堆胶过厚", "每层都要薄涂均匀"],
      };

    const sop = hasOpenAiConfig()
      ? await createJsonResponse<SopPayload>({
        system:
          "You rewrite nail tutorials into short SOP cards in Chinese. Each step should be short, concrete, and suitable for hands-busy execution. Return JSON only.",
        user: parseString,
        jsonSchema: {
          type: "object",
          additionalProperties: false,
          properties: {
            steps: {
              type: "array",
              items: {
                type: "object",
                additionalProperties: false,
                properties: {
                  index: { type: "integer" },
                  title: { type: "string" },
                  instruction: { type: "string" },
                  media_asset_path: { type: ["string", "null"] },
                  timer_seconds: { type: "integer" },
                  voice_shortcut: { type: "string" },
                },
                required: [
                  "index",
                  "title",
                  "instruction",
                  "media_asset_path",
                  "timer_seconds",
                  "voice_shortcut",
                ],
              },
            },
            completion_tip: { type: "string" },
          },
          required: ["steps", "completion_tip"],
        },
      })
      : {
        steps: [
          {
            index: 1,
            title: "薄涂底胶",
            instruction: "修甲后全甲薄涂底胶，不要碰皮。",
            media_asset_path: null,
            timer_seconds: 30,
            voice_shortcut: "下一步",
          },
          {
            index: 2,
            title: "两层底色",
            instruction: "透粉底色分两层薄涂，每层都照灯。",
            media_asset_path: null,
            timer_seconds: 60,
            voice_shortcut: "涂好了",
          },
          {
            index: 3,
            title: "磁吸猫眼",
            instruction: "上猫眼胶后用磁铁斜吸 45 度，看到光再照灯。",
            media_asset_path: null,
            timer_seconds: 60,
            voice_shortcut: "下一步",
          },
        ],
        completion_tip: "最后封层后观察高光是否连贯，不满意可局部补吸。",
      };

    const { error: bomError } = await supabase.from("bom_lists").upsert({
      session_id: body.session_id,
      bom_json: bom,
    });

    if (bomError) throw bomError;

    const { error: sopError } = await supabase.from("sop_guides").upsert({
      session_id: body.session_id,
      version: "v1",
      sop_json: sop,
    });

    if (sopError) throw sopError;

    await supabase
      .from("sessions")
      .update({ status: "sop_ready" })
      .eq("id", body.session_id);

    const response: GenerateExecutionPackageResponse = {
      session_id: body.session_id,
      status: "sop_ready",
    };

    return jsonResponse(response);
  } catch (error) {
    return jsonResponse(
      { error: error instanceof Error ? error.message : "Unknown error" },
      { status: 500 },
    );
  }
});
