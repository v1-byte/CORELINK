# CORELINK Workers (tanpa Termux)

Mode cloud: Android app → **Cloudflare Workers** → LLM API (OpenAI / Groq / OpenRouter).

## Deploy

```bash
npm i -g wrangler
cd workers
wrangler login
wrangler secret put LLM_API_KEY
# optional:
# wrangler secret put LLM_BASE_URL   # default https://api.openai.com/v1
wrangler deploy
```

Salin URL hasil deploy, contoh:
`https://corelink-api.<akun>.workers.dev`

Di APK → **LINK** → mode **WORKERS** → tempel URL → **CONNECT**.

## Tidak perlu

- Termux  
- Ollama di HP  
- Port 8787 lokal  
