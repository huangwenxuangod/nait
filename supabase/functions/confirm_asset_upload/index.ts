import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import type {
  ConfirmAssetUploadRequest,
  ConfirmAssetUploadResponse,
} from "../_shared/types.ts";

Deno.serve(async (req) => {
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  try {
    const body = (await req.json()) as ConfirmAssetUploadRequest;
    if (!body.session_id || !body.asset_id || !body.asset_type || !body.storage_path) {
      return jsonResponse(
        { error: "session_id, asset_id, asset_type, and storage_path are required" },
        { status: 400 },
      );
    }

    const supabase = getAdminClient();
    const { error } = await supabase.from("session_assets").insert({
      id: body.asset_id,
      session_id: body.session_id,
      asset_type: body.asset_type,
      storage_path: body.storage_path,
    });

    if (error) throw error;

    if (body.asset_type === "hand_photo") {
      await supabase
        .from("sessions")
        .update({ status: "hand_uploaded" })
        .eq("id", body.session_id);
    }

    const response: ConfirmAssetUploadResponse = { ok: true };
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
