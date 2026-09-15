import http from "node:http";
import { execFile } from "node:child_process";
import { promisify } from "node:util";
import { URL } from "node:url";

const exec = promisify(execFile);
const PORT = Number(process.env.CORELINK_BRIDGE_PORT || 8787);
const HOST = process.env.CORELINK_BRIDGE_HOST || "127.0.0.1";
const OLLAMA_URL = (process.env.OLLAMA_URL || "http://127.0.0.1:11434").replace(/\/$/, "");
const ALLOW_ORIGIN = process.env.CORELINK_ALLOW_ORIGIN || "*";

const json = (res, status, data) => { res.writeHead(status, { "Content-Type": "application/json", "Access-Control-Allow-Origin": ALLOW_ORIGIN, "Access-Control-Allow-Headers": "Content-Type, Authorization", "Access-Control-Allow-Methods": "GET,POST,OPTIONS" }); res.end(JSON.stringify(data)); };
const readBody = async (req) => { let body = ""; for await (const chunk of req) body += chunk; return body ? JSON.parse(body) : {}; };
const token = (name) => process.env[name] ? { configured: true } : { configured: false };

async function fetchJson(url, options = {}, timeout = 8000) {
  const controller = new AbortController(); const timer = setTimeout(() => controller.abort(), timeout);
  try { const response = await fetch(url, { ...options, signal: controller.signal }); const text = await response.text(); let data; try { data = JSON.parse(text); } catch { data = { raw: text }; } return { ok: response.ok, status: response.status, data }; }
  finally { clearTimeout(timer); }
}
async function command(name, args = [], timeout = 8000) { try { const result = await exec(name, args, { timeout }); return { configured: true, online: true, output: result.stdout.slice(0, 20000) }; } catch (error) { return { configured: true, online: false, error: error.shortMessage || error.message }; } }
async function connectorStatus() {
  const [ollama, docker, cloudflared] = await Promise.all([
    fetchJson(`${OLLAMA_URL}/api/tags`).then((r) => ({ configured: true, online: r.ok, latency: r.ok ? "ok" : "error", models: r.data?.models?.map((m) => m.name) || [] })).catch(() => ({ configured: true, online: false })),
    command("docker", ["info", "--format", "{{.ServerVersion}}"]),
    command("cloudflared", ["--version"]),
  ]);
  return { Ollama: ollama, Docker: docker, Cloudflared: cloudflared, GitHub: { ...token("GITHUB_TOKEN"), provider: "github" }, GitLab: { ...token("GITLAB_TOKEN"), provider: "gitlab" }, Vercel: { ...token("VERCEL_TOKEN"), provider: "vercel" }, Supabase: { ...token("SUPABASE_ACCESS_TOKEN"), provider: "supabase" }, HuggingFace: { ...token("HF_TOKEN"), provider: "huggingface" } };
}
async function cloudRequest(provider, path, options = {}) {
  const config = { github: ["https://api.github.com", "GITHUB_TOKEN"], gitlab: ["https://gitlab.com/api/v4", "GITLAB_TOKEN"], vercel: ["https://api.vercel.com", "VERCEL_TOKEN"], supabase: ["https://api.supabase.com", "SUPABASE_ACCESS_TOKEN"], huggingface: ["https://huggingface.co/api", "HF_TOKEN"] }[provider];
  if (!config || !process.env[config[1]]) return { configured: false, error: `${provider} token is not configured` };
  const headers = { Accept: "application/json", Authorization: `Bearer ${process.env[config[1]]}`, ...(options.body ? { "Content-Type": "application/json" } : {}) };
  const result = await fetchJson(`${config[0]}${path}`, { ...options, headers: { ...headers, ...(options.headers || {}) } }); return { configured: true, ...result };
}
async function proxyOllama(path, body, res) {
  const response = await fetch(`${OLLAMA_URL}${path}`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
  res.writeHead(response.status, { "Content-Type": response.headers.get("content-type") || "application/json", "Access-Control-Allow-Origin": ALLOW_ORIGIN });
  if (response.body) for await (const chunk of response.body) res.write(chunk); res.end();
}

const server = http.createServer(async (req, res) => {
  if (req.method === "OPTIONS") return json(res, 204, {});
  const url = new URL(req.url, `http://${req.headers.host}`);
  try {
    if (req.method === "GET" && url.pathname === "/health") return json(res, 200, { name: "CORELINK Bridge", version: "1.0.0", online: true, port: PORT });
    if (req.method === "GET" && url.pathname === "/api/connectors") return json(res, 200, await connectorStatus());
    if (req.method === "GET" && url.pathname === "/api/ollama/tags") { const r = await fetchJson(`${OLLAMA_URL}/api/tags`); return json(res, r.status, r.data); }
    if (req.method === "POST" && url.pathname === "/api/ollama/generate") return proxyOllama("/api/generate", await readBody(req), res);
    if (req.method === "GET" && url.pathname === "/api/github/repos") return json(res, 200, await cloudRequest("github", "/user/repos?per_page=100"));
    if (req.method === "GET" && url.pathname === "/api/gitlab/projects") return json(res, 200, await cloudRequest("gitlab", "/projects?membership=true&per_page=100"));
    if (req.method === "GET" && url.pathname === "/api/vercel/projects") return json(res, 200, await cloudRequest("vercel", "/v9/projects"));
    if (req.method === "GET" && url.pathname === "/api/supabase/projects") return json(res, 200, await cloudRequest("supabase", "/v1/projects"));
    if (req.method === "GET" && url.pathname === "/api/huggingface/models") return json(res, 200, await cloudRequest("huggingface", "/models?limit=20"));
    if (req.method === "POST" && url.pathname === "/api/docker") { const body = await readBody(req); return json(res, 200, await command("docker", body.args || ["ps"])); }
    if (req.method === "POST" && url.pathname === "/api/cloudflared") { const body = await readBody(req); return json(res, 200, await command("cloudflared", body.args || ["tunnel", "list"])); }
    return json(res, 404, { error: "Not found" });
  } catch (error) { return json(res, 500, { error: error.message }); }
});
server.listen(PORT, HOST, () => console.log(`CORELINK Bridge listening on http://${HOST}:${PORT}`));
