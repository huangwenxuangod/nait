import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import { createQwenJsonResponse, hasQwenConfig } from "../_shared/qwen.ts";
import type {
  SubmitSourceLinkRequest,
  SubmitSourceLinkResponse,
} from "../_shared/types.ts";

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

Deno.serve(async (req) => {
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  try {
    const body = (await req.json()) as SubmitSourceLinkRequest;
    if (!body.session_id || !body.source_url) {
      return jsonResponse({ error: "session_id and source_url are required" }, { status: 400 });
    }

    const supabase = getAdminClient();
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

    const mockParse = hasQwenConfig()
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
      })
      : {
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
        note: "Fallback parse generated without OPENAI_API_KEY.",
      };

    await supabase.from("source_parses").upsert({
      session_id: body.session_id,
      model: hasQwenConfig() ? "qwen3.6-plus" : "fallback",
      version: "v1",
      parse_json: mockParse,
    });

    await supabase
      .from("sessions")
      .update({
        status: "source_parsed",
        style_name: mockParse.style_name,
      })
      .eq("id", body.session_id);

    const response: SubmitSourceLinkResponse = {
      session_id: data.id,
      status: "source_parsing",
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
