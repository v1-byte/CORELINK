const MODEL = "@cf/meta/llama-3.1-8b-instruct";

function corsHeaders(origin = "*") {
  return {
    "Access-Control-Allow-Origin": origin,
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Content-Type": "application/json; charset=utf-8",
  };
}

function json(data, status = 200, origin = "*") {
  return new Response(JSON.stringify(data), { status, headers: corsHeaders(origin) });
}

function promptText(body) {
  const prompt = String(body.prompt || "").trim();
  const system = String(body.system || "").trim();
  return system ? `${system}\n\n---\nUser request:\n${prompt}` : prompt;
}

async function runAI(env, body) {
  const prompt = promptText(body);
  if (!prompt) return { error: "Prompt is required" };
  const result = await env.AI.run(body.model || MODEL, {
    messages: [
      { role: "system", content: String(body.system || "You are CORELINK AI. Answer clearly and concisely.") },
      { role: "user", content: prompt },
    ],
    temperature: Number(body.temperature ?? 0.2),
    max_tokens: Math.min(Number(body.max_tokens ?? 512), 1024),
  });
  const text = result?.response || result?.result?.response || result?.choices?.[0]?.message?.content || JSON.stringify(result);
  return { text };
}

export default {
  async fetch(request, env) {
    const origin = env.ALLOW_ORIGIN || "*";
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: corsHeaders(origin) });
    const url = new URL(request.url);
    try {
      if (request.method === "GET" && url.pathname === "/health") {
        return json({ name: "CORELINK Cloud Worker", version: "1.0.0-workers-ai", online: true, provider: "Cloudflare Workers AI" }, 200, origin);
      }
      if (request.method === "GET" && url.pathname === "/api/ollama/tags") {
        return json({ models: [{ name: MODEL, size: 0, provider: "workers-ai" }] }, 200, origin);
      }
      if (request.method === "POST" && (url.pathname === "/api/agent" || url.pathname === "/api/ollama/generate")) {
        const body = await request.json();
        const result = await runAI(env, body);
        if (result.error) return json(result, 400, origin);
        if (url.pathname === "/api/agent") return json({ ok: true, tool: "workers-ai", duration_ms: 0, result: { configured: true, tool: "workers-ai", output: result.text } }, 200, origin);
        return json({ response: result.text, done: true, model: body.model || MODEL }, 200, origin);
      }
      if (request.method === "GET" && url.pathname === "/api/tools") {
        return json({ tools: [{ name: "workers-ai", description: "Cloud AI through Cloudflare Workers AI" }] }, 200, origin);
      }
      return json({ error: "Not found" }, 404, origin);
    } catch (error) {
      return json({ error: error?.message || "Worker AI error" }, 500, origin);
    }
  },
};
