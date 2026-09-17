#!/data/data/com.termux/files/usr/bin/bash
set -eu

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME_DIR="${HOME}/.cache/corelink"
PID_FILE="${RUNTIME_DIR}/bridge.pid"
LOG_FILE="${RUNTIME_DIR}/bridge.log"

mkdir -p "$RUNTIME_DIR"

if [ -f "$PID_FILE" ]; then
  pid="$(cat "$PID_FILE" 2>/dev/null || true)"
  if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
    printf 'CORELINK Bridge sudah berjalan (PID %s)\n' "$pid"
    exit 0
  fi
  rm -f "$PID_FILE"
fi

nohup "$SCRIPT_DIR/start-termux.sh" >>"$LOG_FILE" 2>&1 </dev/null &
pid=$!
printf '%s\n' "$pid" >"$PID_FILE"
printf 'CORELINK Bridge berjalan di background (PID %s)\n' "$pid"
printf 'Log: %s\n' "$LOG_FILE"
