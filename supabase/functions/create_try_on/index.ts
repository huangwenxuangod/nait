import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import { createRequestLogger, stringifyError } from "../_shared/logger.ts";
import type {
  CreateTryOnRequest,
  CreateTryOnResponse,
} from "../_shared/types.ts";

Deno.serve(async (req) => {
  const logger = createRequestLogger("create_try_on");
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  try {
    const body = (await req.json()) as CreateTryOnRequest;
    logger.log("request_start", {
      session_id: body.session_id,
      nail_hint_count: body.nail_position_hints?.length ?? 0,
    });
    if (!body.session_id) {
      logger.warn("validation_failed", { reason: "session_id missing" });
      return jsonResponse({ error: "session_id is required" }, { status: 400 });
    }

    const supabase = getAdminClient();

    logger.log("db_update_session_status_start", { status: "try_on_pending" });
    const { error: pendingError } = await supabase
      .from("sessions")
      .update({ status: "try_on_pending" })
      .eq("id", body.session_id);

    if (pendingError) {
      throw new Error(`TRYON_SESSION_STATUS_UPDATE_FAILED: ${stringifyError(pendingError)}`);
    }
    logger.log("db_update_session_status_done", { status: "try_on_pending" });

    const response: CreateTryOnResponse = {
      session_id: body.session_id,
      status: "try_on_pending",
    };

    logger.done("ok", {
      session_id: body.session_id,
      status: "try_on_pending",
    });
    return jsonResponse(response);
  } catch (error) {
    logger.error("request_failed", { error: stringifyError(error) });
    logger.done("error", { error: stringifyError(error) });
    const message = stringifyError(error);
    return jsonResponse(
      { error: message },
      { status: 500 },
    );
  }
});
