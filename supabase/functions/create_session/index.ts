import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { getAdminClient } from "../_shared/client.ts";
import type {
  CreateSessionRequest,
  CreateSessionResponse,
} from "../_shared/types.ts";

Deno.serve(async (req) => {
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  try {
    const body = (await req.json()) as CreateSessionRequest;
    if (!body.install_id || !body.source_type) {
      return jsonResponse({ error: "install_id and source_type are required" }, { status: 400 });
    }

    const supabase = getAdminClient();
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

    const response: CreateSessionResponse = {
      session_id: data.id,
      status: data.status,
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
