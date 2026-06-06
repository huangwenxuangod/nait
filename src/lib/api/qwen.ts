const API_KEY = () => process.env.QWEN_API_KEY ?? process.env.DASHSCOPE_API_KEY ?? "";
const BASE_URL = () => process.env.QWEN_BASE_URL ?? "https://dashscope.aliyuncs.com/compatible-mode/v1";
const MODEL = () => process.env.QWEN_TEXT_MODEL ?? "qwen-plus";

interface ChatMessage {
  role: "system" | "user" | "assistant";
  content: string;
}

interface ChatCompletionRequest {
  model: string;
  messages: ChatMessage[];
  temperature?: number;
  max_tokens?: number;
  response_format?: { type: "json_object" };
}

interface ChatCompletionResponse {
  choices: { message: { content: string } }[];
}

export async function chatCompletion(
  messages: ChatMessage[],
  opts?: { temperature?: number; maxTokens?: number; jsonMode?: boolean },
): Promise<string> {
  const key = API_KEY();
  if (!key) {
    throw new Error("QWEN_API_KEY 未配置，请在 .env 中设置 QWEN_API_KEY");
  }

  const body: ChatCompletionRequest = {
    model: MODEL(),
    messages,
    temperature: opts?.temperature ?? 0.7,
    max_tokens: opts?.maxTokens ?? 4096,
  };

  if (opts?.jsonMode) {
    body.response_format = { type: "json_object" };
  }

  const res = await fetch(`${BASE_URL()}/chat/completions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${key}`,
    },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const err = await res.text();
    throw new Error(`Qwen API 调用失败 (${res.status}): ${err.slice(0, 200)}`);
  }

  const data = (await res.json()) as ChatCompletionResponse;
  return data.choices?.[0]?.message?.content ?? "";
}
