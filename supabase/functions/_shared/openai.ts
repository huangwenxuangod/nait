const OPENAI_BASE_URL = Deno.env.get("OPENAI_BASE_URL") ?? "https://api.openai.com/v1";
const OPENAI_API_KEY = Deno.env.get("OPENAI_API_KEY") ?? "";
const DEFAULT_MODEL = Deno.env.get("OPENAI_MODEL") ?? "gpt-4o-mini";
const IMAGE_MODEL = Deno.env.get("OPENAI_IMAGE_MODEL") ?? "gpt-image-2";

export function hasOpenAiConfig() {
  return OPENAI_API_KEY.trim().length > 0;
}

export async function createJsonResponse<T>({
  system,
  user,
  jsonSchema,
  model = DEFAULT_MODEL,
  imageInputs = [],
}: {
  system: string;
  user: string;
  jsonSchema: Record<string, unknown>;
  model?: string;
  imageInputs?: Array<{
    imageBase64: string;
    mimeType?: string;
  }>;
}): Promise<T> {
  if (!hasOpenAiConfig()) {
    throw new Error("Missing OPENAI_API_KEY");
  }

  const isCustomResponses = OPENAI_BASE_URL.endsWith("/responses");
  const endpoint = isCustomResponses ? OPENAI_BASE_URL : `${OPENAI_BASE_URL.replace(/\/+$/, "")}/chat/completions`;

  const body = isCustomResponses 
    ? {
        model,
        input: [
          { role: "system", content: [{ type: "input_text", text: system }] },
          {
            role: "user",
            content: [
              { type: "input_text", text: user },
              ...imageInputs.map((image) => ({
                type: "input_image",
                image_url: toDataUrl(image.imageBase64, image.mimeType),
              })),
            ],
          },
        ],
        text: {
          format: {
            type: "json_schema",
            name: "nail_it_schema",
            strict: true,
            schema: jsonSchema,
          }
        }
      }
    : {
        model,
        messages: [
          { role: "system", content: system },
          {
            role: "user",
            content: [
              { type: "text", text: user },
              ...imageInputs.map((image) => ({
                type: "image_url",
                image_url: {
                  url: toDataUrl(image.imageBase64, image.mimeType),
                },
              })),
            ],
          }
        ],
        response_format: {
          type: "json_object"
        }
      };

  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${OPENAI_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`OpenAI error ${response.status}: ${errorText}`);
  }

  const data = await response.json();
  
  let outputText = "";
  if (data?.choices?.[0]?.message?.content) {
    outputText = data.choices[0].message.content;
  } else {
    outputText = extractOutputText(data);
  }

  if (!outputText) {
    throw new Error("OpenAI returned no output text");
  }

  return JSON.parse(outputText) as T;
}

export async function createImageEdit({
  prompt,
  imageBase64,
  mimeType = "image/jpeg",
  fileName = "hand-photo.jpg",
  model = IMAGE_MODEL,
  size = "1024x1024",
  imageInputs,
}: {
  prompt: string;
  imageBase64: string;
  mimeType?: string;
  fileName?: string;
  model?: string;
  size?: "1024x1024" | "1024x1536" | "1536x1024" | "auto";
  imageInputs?: Array<{
    imageBase64: string;
    mimeType?: string;
    fileName?: string;
  }>;
}): Promise<string> {
  if (!hasOpenAiConfig()) {
    throw new Error("Missing OPENAI_API_KEY");
  }

  const endpoint = `${OPENAI_BASE_URL.replace(/\/+$/, "")}/images/edits`;
  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${OPENAI_API_KEY}`,
    },
    body: createImageEditFormData({
      prompt,
      imageBase64,
      mimeType,
      fileName,
      model,
      size,
      imageInputs,
    }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`OpenAI image edit error ${response.status}: ${errorText}`);
  }

  const data = await response.json();
  const base64 = data?.data?.[0]?.b64_json;
  if (typeof base64 !== "string" || base64.length === 0) {
    throw new Error("OpenAI returned no edited image");
  }
  return base64;
}

function createImageEditFormData({
  prompt,
  imageBase64,
  mimeType,
  fileName,
  model,
  size,
  imageInputs,
}: {
  prompt: string;
  imageBase64: string;
  mimeType: string;
  fileName: string;
  model: string;
  size: string;
  imageInputs?: Array<{
    imageBase64: string;
    mimeType?: string;
    fileName?: string;
  }>;
}) {
  const form = new FormData();
  form.set("model", model);
  form.set("prompt", prompt);
  form.set("size", size);
  form.set("quality", "auto");
  form.set("format", "png");
  form.set("background", "auto");
  form.set("moderation", "auto");

  // 👈 严格单图：只发送手图（hand_photo）作为 image 参数，符合 OpenAI 官方 /images/edits 规范，防止多图导致中转代理挂起超时！
  form.append(
    "image",
    new Blob([Uint8Array.from(atob(imageBase64), (c) => c.charCodeAt(0))], {
      type: mimeType || "image/jpeg",
    }),
    fileName || "hand-photo.jpg",
  );
  return form;
}

function toDataUrl(imageBase64: string, mimeType = "image/jpeg") {
  return `data:${mimeType};base64,${imageBase64}`;
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
