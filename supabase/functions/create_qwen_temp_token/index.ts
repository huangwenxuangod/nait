import { handleOptions, jsonResponse } from "../_shared/cors.ts";

const REGION = Deno.env.get("DASHSCOPE_REGION") ?? "beijing";
const DASHSCOPE_API_KEY = Deno.env.get("DASHSCOPE_API_KEY") ?? "";
const MODEL = Deno.env.get("DASHSCOPE_REALTIME_MODEL") ?? "qwen3.5-omni-plus-realtime";

const websocketUrl = REGION === "beijing"
  ? "wss://dashscope.aliyuncs.com/api-ws/v1/realtime"
  : "wss://dashscope-intl.aliyuncs.com/api-ws/v1/realtime";

const tokenUrl = REGION === "beijing"
  ? "https://dashscope.aliyuncs.com/api/v1/apps/auth/token"
  : "https://dashscope-intl.aliyuncs.com/api/v1/apps/auth/token";

Deno.serve(async (req) => {
  const preflight = handleOptions(req);
  if (preflight) return preflight;

  if (!DASHSCOPE_API_KEY) {
    return jsonResponse(
      {
        token: "demo-qwen-token",
        websocket_url: websocketUrl,
        model: MODEL,
        expires_at: null,
      },
      { status: 200 },
    );
  }

  try {
    const response = await fetch(tokenUrl, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${DASHSCOPE_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        expire_seconds: 600,
      }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`DashScope token error ${response.status}: ${errorText}`);
    }

    const data = await response.json();
    const token = data?.data?.token ?? data?.token;
    const expiresAt = data?.data?.expired_at ?? data?.expired_at ?? null;

    if (typeof token !== "string" || token.length === 0) {
      throw new Error("DashScope returned empty token");
    }

    return jsonResponse({
      token,
      websocket_url: websocketUrl,
      model: MODEL,
      expires_at: expiresAt,
    });
  } catch (error) {
    console.error("create_qwen_temp_token error:", error);
    const message = error instanceof Error ? error.message : String(error);
    return jsonResponse({ error: message }, { status: 500 });
  }
});
