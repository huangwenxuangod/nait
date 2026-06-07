import { handleOptions, jsonResponse } from "../_shared/cors.ts";
import { createRequestLogger } from "../_shared/logger.ts";

const REGION = Deno.env.get("DASHSCOPE_REGION") ?? "beijing";
const DASHSCOPE_API_KEY = Deno.env.get("DASHSCOPE_API_KEY") ?? "";
const MODEL = Deno.env.get("DASHSCOPE_REALTIME_MODEL") ?? "qwen3.5-omni-plus-realtime";

const websocketBaseUrl = REGION === "beijing"
  ? "wss://dashscope.aliyuncs.com/api-ws/v1/realtime"
  : "wss://dashscope-intl.aliyuncs.com/api-ws/v1/realtime";
const websocketUrl = `${websocketBaseUrl}?model=${encodeURIComponent(MODEL)}`;

const handler = async (req: Request) => {
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

};

export default handler;

Deno.serve(handler);
