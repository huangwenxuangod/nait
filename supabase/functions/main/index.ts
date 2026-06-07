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

  const servicePath = `../${functionName}`;
  console.log(`[main] Routing to service: ${servicePath}`);

  try {
    const module = await import(`${servicePath}/index.ts`);
    const handler = module.default;
    if (typeof handler === 'function') {
      return await handler(req);
    }
    
    // If the module does not export a default handler, we return a success status
    // because some Supabase Edge Runtime environments handle Deno.serve registration globally.
    return new Response(`Function ${functionName} loaded successfully`, { status: 200 });
  } catch (err) {
    console.error(`[main] Error routing to ${functionName}:`, err);
    return new Response(`Function not found or failed to load: ${err.message}`, { status: 404 });
  }
});
