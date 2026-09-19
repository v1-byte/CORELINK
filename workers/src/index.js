/**
 * CORELINK Workers API — no Termux required
 * Same routes as local bridge so the Android app can connect.
 *
 * Secrets (wrangler secret put):
 *   LLM_API_KEY   — OpenAI / Groq / OpenRouter key
 *   LLM_BASE_URL  — e.g. https://api.openai.com/v1  or https://api.groq.com/openai/v1
 */

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json", ...cors },
  });
}

async function chatLLM(env, { model, prompt, system, temperature }) {
  const base = (env.LLM_BASE_URL || "https://api.openai.com/v1").replace(/\/$/, "");
  const key = env.LLM_API_KEY || "";
  if (!key) {
    return {
      ok: false,
      error: "LLM_API_KEY belum di-set di Workers. Jalankan: wrangler secret put LLM_API_KEY",
    };
  }
  const body = {
    model: model || env.OLLAMA_MODEL || "gpt-4o-mini",
    messages: [
      ...(system ? [{ role: "system", content: system }] : []),
      { role: "user", content: prompt || "" },
    ],
    temperature: temperature ?? 0.85,
  };
  const res = await fetch(`${base}/chat/completions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${key}`,
    },
    body: JSON.stringify(body),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    return { ok: false, error: data.error?.message || data.error || `LLM HTTP ${res.status}` };
  }
  const text = data.choices?.[0]?.message?.content || "";
  return { ok: true, tool: "workers-llm", output: text, response: text };
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: cors });
    }
    const url = new URL(request.url);
    const path = url.pathname;

    try {
      if (request.method === "GET" && path === "/health") {
        return json({
          name: "CORELINK Workers",
          version: "2.0.0-workers",
          online: true,
          mode: "workers",
          llm_configured: Boolean(env.LLM_API_KEY),
        });
      }

      if (request.method === "GET" && path === "/api/tools") {
        return json({ tools: [{ name: "workers-llm", description: "Cloud LLM via Workers" }] });
      }

      if (request.method === "GET" && path === "/api/ollama/tags") {
        const model = env.OLLAMA_MODEL || "gpt-4o-mini";
        return json({ models: [{ name: model }] });
      }

      if (request.method === "GET" && path === "/api/connectors") {
        return json({
          Workers: { configured: true, online: true, mode: "cloud" },
          Ollama: { configured: false, online: false, note: "Local Ollama not used in Workers mode" },
        });
      }

      if (request.method === "POST" && path === "/api/agent") {
        const body = await request.json().catch(() => ({}));
        const started = Date.now();
        const result = await chatLLM(env, {
          model: body.model,
          prompt: body.prompt || body.message || "",
          system: body.system || body.system_prompt || "",
          temperature: body.temperature,
        });
        return json({
          ok: result.ok !== false,
          tool: "workers-llm",
          duration_ms: Date.now() - started,
          result,
        });
      }

      // Remote session stubs (in-memory not durable on Workers — use KV later)
      if (path.startsWith("/api/remote/")) {
        return json({
          ok: false,
          error: "Remote sesi di Workers butuh KV binding — menyusul. Chat AI sudah aktif.",
        });
      }

      return json({ error: "Not found", path }, 404);
    } catch (e) {
      return json({ error: e.message || String(e) }, 500);
    }
  },
};
