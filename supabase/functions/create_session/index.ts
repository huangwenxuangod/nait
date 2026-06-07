import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import { createRequestLogger, stringifyError } from "../_shared/logger.ts";
import type {
  CreateSessionRequest,
  CreateSessionResponse,
} from "../_shared/types.ts";

Deno.serve(async (req) => {
  const logger = createRequestLogger("create_session");
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  try {
    const body = (await req.json()) as CreateSessionRequest;
    logger.log("request_start", {
      install_id: body.install_id,
      source_type: body.source_type,
    });
    if (!body.install_id || !body.source_type) {
      logger.warn("validation_failed", { reason: "install_id or source_type missing" });
      return jsonResponse({ error: "install_id and source_type are required" }, { status: 400 });
    }

    const supabase = getAdminClient();
    logger.log("db_insert_session_start");
    const { data, error } = await supabase
      .from("sessions")
      .insert({
        install_id: body.install_id,
        source_type: body.source_type,
        status: "draft",
      })
      .select("id, status")
      .single();

    if (error || !data) {
      throw error ?? new Error("Failed to create session");
    }
    logger.log("db_insert_session_done", {
      session_id: data.id,
      status: data.status,
    });

    const response: CreateSessionResponse = {
      session_id: data.id,
      status: data.status,
    };

    logger.done("ok", {
      session_id: data.id,
      status: data.status,
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
