import { handleOptions, jsonResponse } from "../_shared/cors.ts";

const REGION = Deno.env.get("DASHSCOPE_REGION") ?? "beijing";
const DASHSCOPE_API_KEY = Deno.env.get("DASHSCOPE_API_KEY") ?? "";
const MODEL = Deno.env.get("DASHSCOPE_REALTIME_MODEL") ?? "qwen3.5-omni-plus-realtime";

const websocketBaseUrl = REGION === "beijing"
  ? "wss://dashscope.aliyuncs.com/api-ws/v1/realtime"
  : "wss://dashscope-intl.aliyuncs.com/api-ws/v1/realtime";
const websocketUrl = `${websocketBaseUrl}?model=${encodeURIComponent(MODEL)}`;

Deno.serve(async (req) => {
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  if (!DASHSCOPE_API_KEY) {
    return jsonResponse({ error: "DASHSCOPE_API_KEY missing" }, { status: 500 });
  }

  return jsonResponse({
    // Temporary token flow was causing handshake mismatch for realtime.
    // For this MVP path we return the actual API key so the Android client
    // can follow the official websocket auth shape directly.
    token: DASHSCOPE_API_KEY,
    websocket_url: websocketUrl,
    model: MODEL,
    expires_at: null,
  });
});
