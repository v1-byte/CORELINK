#!/data/data/com.termux/files/usr/bin/bash
set -eu

# Beri waktu Android dan jaringan untuk siap setelah boot.
sleep 20

BRIDGE_DIR="${HOME}/corelink-bridge"
if [ -x "$BRIDGE_DIR/start-background.sh" ]; then
  "$BRIDGE_DIR/start-background.sh"
else
  printf 'CORELINK Bridge belum terpasang di %s\n' "$BRIDGE_DIR" >&2
  exit 1
fi
