// supabase/functions/main/index.ts
console.log("Main worker booted successfully");

Deno.serve(async (req: Request) => {
  const url = new URL(req.url);
  const pathname = url.pathname;
  const pathParts = pathname.split('/');
  const functionName = pathParts[1];

  if (!functionName) {
    return new Response('No function name specified in path', { status: 400 });
  }

  // Use absolute container path to bypass Deno's temporary compilation directory sandbox
  const servicePath = `file:///home/deno/functions/${functionName}/index.ts`;
  console.log(`[main] Routing to service absolute path: ${servicePath}`);

  try {
    const module = await import(servicePath);
    const handler = module.default;
    if (typeof handler === 'function') {
      return await handler(req);
    }
    
    return new Response(`Function ${functionName} loaded successfully`, { status: 200 });
  } catch (err) {
    console.error(`[main] Error routing to ${functionName}:`, err);
    return new Response(`Function not found or failed to load: ${err.message}`, { status: 404 });
  }
});
