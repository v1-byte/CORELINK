#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")"
[ -f .env ] && set -a && . ./.env && set +a
mkdir -p "$HOME/.cache/corelink"
OLLAMA_URL="${OLLAMA_URL:-http://127.0.0.1:11434}"
termux-wake-lock 2>/dev/null || true
if command -v ollama >/dev/null 2>&1 && ! curl -fsS "$OLLAMA_URL/api/tags" >/dev/null 2>&1; then
  ollama serve > "$HOME/.cache/corelink/ollama.log" 2>&1 &
  sleep 2
fi
exec node server.mjs
