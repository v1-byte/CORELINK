#!/data/data/com.termux/files/usr/bin/bash
set -eu

PID_FILE="${HOME}/.cache/corelink/bridge.pid"

if [ ! -f "$PID_FILE" ]; then
  printf 'CORELINK Bridge tidak sedang berjalan.\n'
  exit 0
fi

pid="$(cat "$PID_FILE" 2>/dev/null || true)"
if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
  kill "$pid" 2>/dev/null || true
  printf 'CORELINK Bridge dihentikan (PID %s).\n' "$pid"
else
  printf 'Proses CORELINK Bridge sudah tidak aktif.\n'
fi
rm -f "$PID_FILE"
