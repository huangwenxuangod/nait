const QWEN_BASE_URL =
  Deno.env.get("QWEN_BASE_URL") ?? "https://dashscope.aliyuncs.com/compatible-mode/v1";
const QWEN_API_KEY =
  Deno.env.get("QWEN_API_KEY") ?? Deno.env.get("DASHSCOPE_API_KEY") ?? "";
const QWEN_TEXT_MODEL = Deno.env.get("QWEN_TEXT_MODEL") ?? "qwen2.5-vl-72b-instruct";

export function hasQwenConfig() {
  return QWEN_API_KEY.trim().length > 0;
}

export async function createQwenJsonResponse<T>({
  system,
  user,
  model = QWEN_TEXT_MODEL,
  requiredKeys = [],
  validate,
  imageInputs = [],
}: {
  system: string;
  user: string;
  model?: string;
  requiredKeys?: string[];
  validate?: (payload: unknown) => string | null;
  imageInputs?: Array<{
    imageBase64: string;
    mimeType?: string;
  }>;
}): Promise<T> {
  if (!hasQwenConfig()) {
    throw new Error("Missing QWEN_API_KEY/DASHSCOPE_API_KEY");
  }

  console.log(`[qwen] createQwenJsonResponse start | model=${model} | image_inputs=${imageInputs.length} | required_keys=${requiredKeys.join(",")}`);

  const parseAndValidate = (raw: string): T => {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      throw new Error("Qwen returned non-object JSON");
    }

    for (const key of requiredKeys) {
      if (!(key in parsed)) {
        throw new Error(`Qwen JSON missing required key: ${key}`);
      }
    }

    const validationError = validate?.(parsed) ?? null;
    if (validationError) {
      throw new Error(validationError);
    }

    return parsed as T;
  };

  const firstRaw = await requestQwenJsonText(system, user, model, imageInputs);
  try {
    console.log(`[qwen] first_response_received | length=${firstRaw.length}`);
    return parseAndValidate(firstRaw);
  } catch (firstError) {
    console.warn(`[qwen] first_validation_failed | error=${firstError instanceof Error ? firstError.message : String(firstError)}`);
    const secondRaw = await requestQwenJsonText(
      `${system}\n你上一次输出没有通过程序校验。这一次只能输出严格 JSON 对象，禁止附带任何解释、markdown、代码块或额外文本。`,
      `${user}\n请重新输出完整 JSON，确保字段齐全、类型正确。`,
      model,
      imageInputs,
    );

    try {
      console.log(`[qwen] retry_response_received | length=${secondRaw.length}`);
      return parseAndValidate(secondRaw);
    } catch (secondError) {
      const firstMessage = firstError instanceof Error ? firstError.message : String(firstError);
      const secondMessage = secondError instanceof Error ? secondError.message : String(secondError);
      throw new Error(`Qwen JSON validation failed. First attempt: ${firstMessage}; second attempt: ${secondMessage}`);
    }
  }
}

async function requestQwenJsonText(
  system: string,
  user: string,
  model: string,
  imageInputs?: Array<{ imageBase64: string; mimeType?: string }>,
): Promise<string> {
  let userContent: any = user;
  
  if (imageInputs && imageInputs.length > 0) {
    userContent = [
      { type: "text", text: user },
      ...imageInputs.map((img) => ({
        type: "image_url",
        image_url: {
          url: `data:${img.mimeType ?? "image/jpeg"};base64,${img.imageBase64}`,
        },
      })),
    ];
  } else {
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    const match = user.match(urlRegex);
    
    if (model.includes("vl") && match) {
      const url = match[0];
      const isVideo = url.endsWith(".mp4") || url.endsWith(".webm") || url.endsWith(".mov") || url.includes("video");
      
      if (isVideo) {
        userContent = [
          {
            type: "video",
            video: url
          },
          {
            type: "text",
            text: user.replace(url, "").trim()
          }
        ];
      } else if (url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") || url.endsWith(".webp")) {
        userContent = [
          {
            type: "image_url",
            image_url: {
              url: url
            }
          },
          {
            type: "text",
            text: user.replace(url, "").trim()
          }
        ];
      }
    }
  }

  const response = await fetch(`${QWEN_BASE_URL.replace(/\/+$/, "")}/chat/completions`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${QWEN_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model,
      messages: [
        { role: "system", content: system },
        { role: "user", content: userContent },
      ],
      response_format: {
        type: "json_object",
      },
    }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error(`[qwen] request_failed | model=${model} | status=${response.status} | body=${errorText}`);
    throw new Error(`Qwen error ${response.status}: ${errorText}`);
  }

  const data = await response.json();
  const outputText = data?.choices?.[0]?.message?.content;
  if (typeof outputText !== "string" || outputText.trim().length === 0) {
    console.error(`[qwen] empty_output | model=${model}`);
    throw new Error("Qwen returned no output text");
  }

  console.log(`[qwen] request_success | model=${model} | output_length=${outputText.length}`);

  return outputText;
}
