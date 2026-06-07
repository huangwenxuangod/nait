import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import type {
  CreateTryOnRequest,
  CreateTryOnResponse,
} from "../_shared/types.ts";

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

    const response: CreateTryOnResponse = {
      session_id: body.session_id,
      status: "try_on_pending",
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
