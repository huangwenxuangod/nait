const OPENAI_BASE_URL = Deno.env.get("OPENAI_BASE_URL") ?? "https://api.openai.com/v1";
const OPENAI_API_KEY = Deno.env.get("OPENAI_API_KEY") ?? "";
const DEFAULT_MODEL = Deno.env.get("OPENAI_MODEL") ?? "gpt-4.1-mini";

export function hasOpenAiConfig() {
  return OPENAI_API_KEY.trim().length > 0;
}

export async function createJsonResponse<T>({
  system,
  user,
  jsonSchema,
  model = DEFAULT_MODEL,
}: {
  system: string;
  user: string;
  jsonSchema: Record<string, unknown>;
  model?: string;
}): Promise<T> {
  if (!hasOpenAiConfig()) {
    throw new Error("Missing OPENAI_API_KEY");
  }

  const response = await fetch(`${OPENAI_BASE_URL}/responses`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${OPENAI_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model,
      input: [
        {
          role: "system",
          content: [{ type: "input_text", text: system }],
        },
        {
          role: "user",
          content: [{ type: "input_text", text: user }],
        },
      ],
      text: {
        format: {
          type: "json_schema",
          name: "nail_it_schema",
          strict: true,
          schema: jsonSchema,
        },
      },
    }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`OpenAI error ${response.status}: ${errorText}`);
  }

  const data = await response.json();
  const outputText = extractOutputText(data);
  if (!outputText) {
    throw new Error("OpenAI returned no output text");
  }

  return JSON.parse(outputText) as T;
}

function extractOutputText(payload: Record<string, unknown>): string {
  const directText = payload.output_text;
  if (typeof directText === "string" && directText.trim().length > 0) {
    return directText;
  }

  const output = payload.output;
  if (!Array.isArray(output)) {
    return "";
  }

  for (const item of output) {
    if (!item || typeof item !== "object") continue;
    const content = (item as { content?: unknown }).content;
    if (!Array.isArray(content)) continue;
    for (const part of content) {
      if (!part || typeof part !== "object") continue;
      const text = (part as { text?: unknown }).text;
      if (typeof text === "string" && text.trim().length > 0) {
        return text;
      }
    }
  }

  return "";
}
