// supabase/functions/main/index.ts
import create_session from "../create_session/index.ts";
import create_try_on from "../create_try_on/index.ts";
import render_try_on from "../render_try_on/index.ts";
import submit_source_link from "../submit_source_link/index.ts";
import generate_execution_package from "../generate_execution_package/index.ts";
import prepare_asset_upload from "../prepare_asset_upload/index.ts";
import confirm_asset_upload from "../confirm_asset_upload/index.ts";
import create_qwen_temp_token from "../create_qwen_temp_token/index.ts";

console.log("Static Main Router booted successfully");

const ROUTES: Record<string, (req: Request) => Promise<Response>> = {
  "create_session": create_session,
  "create_try_on": create_try_on,
  "render_try_on": render_try_on,
  "submit_source_link": submit_source_link,
  "generate_execution_package": generate_execution_package,
  "prepare_asset_upload": prepare_asset_upload,
  "confirm_asset_upload": confirm_asset_upload,
  "create_qwen_temp_token": create_qwen_temp_token,
};

Deno.serve(async (req: Request) => {
  const url = new URL(req.url);
  const pathname = url.pathname;
  const pathParts = pathname.split('/');
  const functionName = pathParts[1];

  if (!functionName) {
    return new Response('No function name specified in path', { status: 400 });
  }

  console.log(`[main] Routing to statically bundled service: ${functionName}`);

  const handler = ROUTES[functionName];
  if (typeof handler === 'function') {
    try {
      return await handler(req);
    } catch (err) {
      console.error(`[main] Error execution in ${functionName}:`, err);
      return new Response(`Internal Server Error in ${functionName}: ${err.message}`, { status: 500 });
    }
  }

  return new Response(`Function ${functionName} not found in static router`, { status: 404 });
});
