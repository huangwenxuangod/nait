import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { createRequestLogger } from "../_shared/logger.ts";
import type { CreateRealtimeTokenResponse } from "../_shared/types.ts";

const REGION = Deno.env.get("DASHSCOPE_REGION") ?? "beijing";
const DASHSCOPE_API_KEY = Deno.env.get("DASHSCOPE_API_KEY") ?? "";
const MODEL = Deno.env.get("DASHSCOPE_REALTIME_MODEL") ?? "qwen3.5-omni-plus-realtime";

const websocketBaseUrl = REGION === "beijing"
  ? "wss://dashscope.aliyuncs.com/api-ws/v1/realtime"
  : "wss://dashscope-intl.aliyuncs.com/api-ws/v1/realtime";
const websocketUrl = `${websocketBaseUrl}?model=${encodeURIComponent(MODEL)}`;

Deno.serve(async (req) => {
  const logger = createRequestLogger("create_qwen_temp_token");
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  logger.log("request_start", {
    region: REGION,
    model: MODEL,
    has_dashscope_key: DASHSCOPE_API_KEY.trim().length > 0,
  });

  if (!DASHSCOPE_API_KEY) {
    logger.warn("missing_dashscope_api_key");
    logger.done("error", { error: "DASHSCOPE_API_KEY missing" });
    return jsonResponse({ error: "DASHSCOPE_API_KEY missing" }, { status: 500 });
  }

  const response: CreateRealtimeTokenResponse = {
    token: DASHSCOPE_API_KEY,
    expires_at: null,
    websocket_url: websocketUrl,
    model: MODEL,
  };

  logger.done("ok", {
    region: REGION,
    model: MODEL,
    websocket_url: websocketUrl,
  });
  return jsonResponse(response);
});
