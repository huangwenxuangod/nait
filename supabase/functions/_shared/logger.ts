export interface RequestLogger {
  requestId: string;
  log: (message: string, extra?: unknown) => void;
  warn: (message: string, extra?: unknown) => void;
  error: (message: string, extra?: unknown) => void;
  done: (status: "ok" | "error", extra?: unknown) => void;
}

export function createRequestLogger(functionName: string, requestId = crypto.randomUUID()): RequestLogger {
  const prefix = `[${functionName}][${requestId}]`;
  const startedAt = Date.now();

  return {
    requestId,
    log(message: string, extra?: unknown) {
      console.log(format(prefix, message, extra));
    },
    warn(message: string, extra?: unknown) {
      console.warn(format(prefix, message, extra));
    },
    error(message: string, extra?: unknown) {
      console.error(format(prefix, message, extra));
    },
    done(status: "ok" | "error", extra?: unknown) {
      const elapsedMs = Date.now() - startedAt;
      const payload = {
        elapsed_ms: elapsedMs,
        ...(isRecord(extra) ? extra : extra == null ? {} : { extra }),
      };
      const message = `request_end status=${status}`;
      if (status === "ok") {
        console.log(format(prefix, message, payload));
      } else {
        console.error(format(prefix, message, payload));
      }
    },
  };
}

export function stringifyError(error: unknown) {
  if (error instanceof Error) return error.message;
  if (typeof error === "string") return error;
  if (typeof error === "object" && error !== null && "message" in error) {
    return String((error as { message: unknown }).message);
  }
  return JSON.stringify(error);
}

function format(prefix: string, message: string, extra?: unknown) {
  if (extra == null) return `${prefix} ${message}`;
  if (typeof extra === "string") return `${prefix} ${message} | ${extra}`;
  try {
    return `${prefix} ${message} | ${JSON.stringify(extra)}`;
  } catch {
    return `${prefix} ${message} | ${String(extra)}`;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
