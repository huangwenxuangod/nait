import { serve } from "https://deno.land/std@0.131.0/http/server.ts";
import * as jose from "https://deno.land/x/jose@v4.14.1/index.ts";

console.log("main function started");

const JWT_SECRET = Deno.env.get("JWT_SECRET") ?? "";
const VERIFY_JWT = Deno.env.get("VERIFY_JWT") === "true";
const DEFAULT_WORKER_TIMEOUT_MS = 300 * 1000;
const DEFAULT_WORKER_MEMORY_MB = 256;

function parsePositiveIntEnv(name: string, fallback: number) {
  const raw = Deno.env.get(name);
  if (!raw) return fallback;
  const parsed = Number.parseInt(raw, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function getAuthToken(req: Request) {
  const authHeader = req.headers.get("authorization");
  if (!authHeader) {
    throw new Error("Missing authorization header");
  }
  const [bearer, token] = authHeader.split(" ");
  if (bearer !== "Bearer" || !token) {
    throw new Error("Auth header is not 'Bearer {token}'");
  }
  return token;
}

async function verifyJWT(jwt: string): Promise<boolean> {
  const encoder = new TextEncoder();
  const secretKey = encoder.encode(JWT_SECRET);
  try {
    await jose.jwtVerify(jwt, secretKey);
  } catch (err) {
    console.error("[main] jwt verify failed", err);
    return false;
  }
  return true;
}

serve(async (req: Request) => {
  if (req.method !== "OPTIONS" && VERIFY_JWT) {
    try {
      const token = getAuthToken(req);
      const isValidJWT = await verifyJWT(token);

      if (!isValidJWT) {
        return new Response(
          JSON.stringify({ msg: "Invalid JWT" }),
          { status: 401, headers: { "Content-Type": "application/json" } },
        );
      }
    } catch (e) {
      console.error("[main] auth failed", e);
      return new Response(
        JSON.stringify({ msg: e instanceof Error ? e.message : String(e) }),
        { status: 401, headers: { "Content-Type": "application/json" } },
      );
    }
  }

  const url = new URL(req.url);
  const { pathname } = url;
  const pathParts = pathname.split("/");
  const serviceName = pathParts[1];

  if (!serviceName) {
    return new Response(
      JSON.stringify({ msg: "missing function name in request" }),
      { status: 400, headers: { "Content-Type": "application/json" } },
    );
  }

  const servicePath = `/home/deno/functions/${serviceName}`;
  const workerTimeoutMs = parsePositiveIntEnv("EDGE_WORKER_TIMEOUT_MS", DEFAULT_WORKER_TIMEOUT_MS);
  const memoryLimitMb = parsePositiveIntEnv("EDGE_WORKER_MEMORY_MB", DEFAULT_WORKER_MEMORY_MB);

  console.log(
    `[main] serving request with ${servicePath} | timeout_ms=${workerTimeoutMs} | memory_mb=${memoryLimitMb}`,
  );

  const noModuleCache = false;
  const importMapPath = null;
  const envVarsObj = Deno.env.toObject();
  const envVars = Object.keys(envVarsObj).map((key) => [key, envVarsObj[key]]);

  try {
    const worker = await EdgeRuntime.userWorkers.create({
      servicePath,
      memoryLimitMb,
      workerTimeoutMs,
      noModuleCache,
      importMapPath,
      envVars,
    });
    return await worker.fetch(req);
  } catch (e) {
    console.error("[main] worker create/fetch failed", e);
    return new Response(
      JSON.stringify({ msg: e instanceof Error ? e.message : String(e) }),
      { status: 500, headers: { "Content-Type": "application/json" } },
    );
  }
});
