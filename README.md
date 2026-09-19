# CORELINK — AI Connector (Workers mode)

Chat AI **tanpa Termux**. Android app → **Cloudflare Workers** → LLM cloud.

## Mode default: WORKERS

1. Deploy API cloud:
   ```bash
   cd workers
   npm i -g wrangler
   wrangler login
   wrangler secret put LLM_API_KEY
   wrangler deploy
   ```
2. Install APK dari [Releases](https://github.com/v1-byte/CORELINK/releases)
3. Tab **LINK** → tempel `https://….workers.dev` → **CONNECT** → **CHAT**

Tidak perlu Ollama / port 8787 di HP.

## Opsional: lokal (Termux)

Masih ada di folder `bridge/` jika ingin self-host di perangkat.

## Struktur

- `android/` — APK
- `workers/` — Cloudflare Workers API
- `bridge/` — legacy Termux bridge
- `client/` — web UI

Lihat `workers/README.md`.
