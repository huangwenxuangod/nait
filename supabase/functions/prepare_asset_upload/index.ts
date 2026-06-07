import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import { createRequestLogger, stringifyError } from "../_shared/logger.ts";
import type {
  PrepareAssetUploadRequest,
  PrepareAssetUploadResponse,
} from "../_shared/types.ts";

const BUCKET = "nail-it-assets";

function extensionForMimeType(mimeType: string) {
  if (mimeType.includes("png")) return "png";
  if (mimeType.includes("webp")) return "webp";
  return "jpg";
}

Deno.serve(async (req) => {
  const logger = createRequestLogger("prepare_asset_upload");
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  try {
    const body = (await req.json()) as PrepareAssetUploadRequest;
    logger.log("request_start", {
      session_id: body.session_id,
      asset_type: body.asset_type,
      mime_type: body.mime_type,
    });
    if (!body.session_id || !body.asset_type || !body.mime_type) {
      logger.warn("validation_failed", { reason: "missing required field" });
      return jsonResponse(
        { error: "session_id, asset_type, and mime_type are required" },
        { status: 400 },
      );
    }

    const supabase = getAdminClient();
    logger.log("db_read_session_start");
    const { data: session, error: sessionError } = await supabase
      .from("sessions")
      .select("id, install_id")
      .eq("id", body.session_id)
      .single();

    if (sessionError || !session) {
      throw sessionError ?? new Error("Session not found");
    }
    logger.log("db_read_session_done", {
      install_id: session.install_id,
    });

    const assetId = crypto.randomUUID();
    const ext = extensionForMimeType(body.mime_type);
    const folder = body.asset_type === "hand_photo"
      ? "hands"
      : body.asset_type === "try_on_result"
      ? "try-on"
      : body.asset_type === "sop_media"
      ? "sop"
      : "tutorials";

    const storagePath = `${folder}/${session.install_id}/${body.session_id}/${assetId}.${ext}`;
    const response: PrepareAssetUploadResponse = {
      asset_id: assetId,
      storage_path: storagePath,
      bucket: BUCKET,
    };

    logger.done("ok", {
      asset_id: assetId,
      storage_path: storagePath,
      bucket: BUCKET,
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
