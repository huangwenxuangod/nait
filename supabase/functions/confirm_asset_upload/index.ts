import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import { createRequestLogger, stringifyError } from "../_shared/logger.ts";
import type {
  ConfirmAssetUploadRequest,
  ConfirmAssetUploadResponse,
} from "../_shared/types.ts";

Deno.serve(async (req) => {
  const logger = createRequestLogger("confirm_asset_upload");
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  try {
    const body = (await req.json()) as ConfirmAssetUploadRequest;
    logger.log("request_start", {
      session_id: body.session_id,
      asset_id: body.asset_id,
      asset_type: body.asset_type,
      storage_path: body.storage_path,
    });
    if (!body.session_id || !body.asset_id || !body.asset_type || !body.storage_path) {
      logger.warn("validation_failed", { reason: "missing required field" });
      return jsonResponse(
        { error: "session_id, asset_id, asset_type, and storage_path are required" },
        { status: 400 },
      );
    }

    const supabase = getAdminClient();
    logger.log("db_insert_asset_start");
    const { error } = await supabase.from("session_assets").insert({
      id: body.asset_id,
      session_id: body.session_id,
      asset_type: body.asset_type,
      storage_path: body.storage_path,
    });

    if (error) throw error;
    logger.log("db_insert_asset_done");

    if (body.asset_type === "hand_photo") {
      logger.log("db_update_session_status_start", { status: "hand_uploaded" });
      await supabase
        .from("sessions")
        .update({ status: "hand_uploaded" })
        .eq("id", body.session_id);
      logger.log("db_update_session_status_done", { status: "hand_uploaded" });
    }

    const response: ConfirmAssetUploadResponse = { ok: true };
    logger.done("ok", { ok: true });
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
