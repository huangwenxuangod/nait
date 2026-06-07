import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import { createRequestLogger, stringifyError } from "../_shared/logger.ts";
import type {
  CreateTryOnRequest,
  CreateTryOnResponse,
} from "../_shared/types.ts";

function getInternalFunctionUrl(functionName: string) {
  const publicBase = Deno.env.get("SUPABASE_PUBLIC_URL")?.trim().replace(/\/+$/, "");
  const internalBase = Deno.env.get("SUPABASE_URL")?.trim().replace(/\/+$/, "");
  const base = publicBase || internalBase;
  if (!base) {
    throw new Error("Missing SUPABASE_PUBLIC_URL or SUPABASE_URL");
  }
  return `${base}/functions/v1/${functionName}`;
}

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

    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const renderTryOnUrl = getInternalFunctionUrl("render_try_on");
    const triggerPayload = {
      session_id: body.session_id,
      nail_position_hints: body.nail_position_hints ?? [],
    };

    logger.log("background_render_try_on_dispatch_start", {
      render_try_on_url: renderTryOnUrl,
      nail_hint_count: triggerPayload.nail_position_hints.length,
    });

    queueMicrotask(async () => {
      try {
        const response = await fetch(renderTryOnUrl, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${serviceRoleKey}`,
            "apikey": serviceRoleKey,
          },
          body: JSON.stringify(triggerPayload),
        });

        if (!response.ok) {
          const errorText = await response.text();
          console.error(
            `[create_try_on] background render_try_on failed | session_id=${body.session_id} | status=${response.status} | body=${errorText}`,
          );
          return;
        }

        const result = await response.text();
        console.log(
          `[create_try_on] background render_try_on completed | session_id=${body.session_id} | body=${result}`,
        );
      } catch (error) {
        console.error(
          `[create_try_on] background render_try_on exception | session_id=${body.session_id} | error=${stringifyError(error)}`,
        );
      }
    });
    logger.log("background_render_try_on_dispatch_done", {
      session_id: body.session_id,
    });

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
