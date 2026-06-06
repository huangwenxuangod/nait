import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import { createQwenJsonResponse, hasQwenConfig } from "../_shared/qwen.ts";
import type {
  GenerateExecutionPackageRequest,
  GenerateExecutionPackageResponse,
} from "../_shared/types.ts";

interface ExecutionPackagePayload {
  session_id: string;
  style_name: string;
  target_image_path: string | null;
  estimated_total_minutes: number;
  steps: Array<{
    id: string;
    title: string;
    instruction: string;
    duration_sec: number;
    needs_timer: boolean;
    needs_visual_check: boolean;
    voice_goal: string;
    voice_shortcut: string;
  }>;
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

    const { data: session } = await supabase
      .from("sessions")
      .select("style_name")
      .eq("id", body.session_id)
      .maybeSingle();

    const { data: parseRecord } = await supabase
      .from("source_parses")
      .select("parse_json")
      .eq("session_id", body.session_id)
      .maybeSingle();

    const { data: tryOnRecord } = await supabase
      .from("try_on_results")
      .select("result_image_path")
      .eq("session_id", body.session_id)
      .maybeSingle();

    const parseJson = parseRecord?.parse_json ?? {};
    const styleName = session?.style_name ?? "暖粉猫眼";
    const input = JSON.stringify({
      style_name: styleName,
      target_image_path: tryOnRecord?.result_image_path ?? null,
      parse_json: parseJson,
    });

    const executionPackage = hasQwenConfig()
      ? await createQwenJsonResponse<ExecutionPackagePayload>({
        system:
          "你是一个视频带做美甲流程规划器。请根据当前款式和目标图，输出极简 execution package。每一步都必须适合手机单卡片引导，只返回 JSON，不要解释。",
        user: input,
        requiredKeys: [
          "session_id",
          "style_name",
          "target_image_path",
          "estimated_total_minutes",
          "steps",
        ],
      })
      : {
        session_id: body.session_id,
        style_name: styleName,
        target_image_path: tryOnRecord?.result_image_path ?? null,
        estimated_total_minutes: 28,
        steps: [
          {
            id: "prep_clean",
            title: "清洁甲面",
            instruction: "先擦净油脂，保持甲面干爽。",
            duration_sec: 45,
            needs_timer: false,
            needs_visual_check: true,
            voice_goal: "确认甲面已经清洁干净",
            voice_shortcut: "我好了",
          },
          {
            id: "base_apply",
            title: "薄涂底胶",
            instruction: "薄薄一层，边缘不要碰皮。",
            duration_sec: 60,
            needs_timer: true,
            needs_visual_check: true,
            voice_goal: "确认底胶没有堆厚",
            voice_shortcut: "看一下",
          },
          {
            id: "cat_eye",
            title: "做猫眼高光",
            instruction: "磁铁斜吸两秒，看到一条柔光就停。",
            duration_sec: 75,
            needs_timer: true,
            needs_visual_check: true,
            voice_goal: "确认高光已经形成",
            voice_shortcut: "下一步",
          },
        ],
      };

    const { error: sopError } = await supabase.from("sop_guides").upsert({
      session_id: body.session_id,
      version: "live_guide_v1",
      sop_json: executionPackage,
    });

    if (sopError) throw sopError;

    await supabase
      .from("sessions")
      .update({ status: "in_progress" })
      .eq("id", body.session_id);

    const response: GenerateExecutionPackageResponse = {
      session_id: body.session_id,
      status: "in_progress",
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
