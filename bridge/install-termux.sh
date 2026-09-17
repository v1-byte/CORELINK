#!/data/data/com.termux/files/usr/bin/bash
set -eu

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${HOME}/corelink-bridge"

pkg update -y
pkg install -y nodejs git curl

mkdir -p "$TARGET"
cp -R "$SCRIPT_DIR"/. "$TARGET"/
chmod +x "$TARGET"/*.sh "$TARGET"/termux-boot/*.sh

cd "$TARGET"
[ -f .env ] || cp config.template .env

# Termux biasa tidak menerima event boot Android. Jalankan bridge otomatis
# ketika shell interaktif Termux dibuka, tanpa membuat proses ganda.
BASHRC="$HOME/.bashrc"
touch "$BASHRC"
if ! grep -q "CORELINK_AUTO_START_BEGIN" "$BASHRC"; then
  cat >> "$BASHRC" <<EOF

# CORELINK_AUTO_START_BEGIN
if [[ "\$-" == *i* ]] && [ -x "$TARGET/start-background.sh" ]; then
  "$TARGET/start-background.sh" >/dev/null 2>&1 || true
fi
# CORELINK_AUTO_START_END
EOF
fi

printf '\nCORELINK Bridge terpasang di %s\n' "$TARGET"
printf 'Edit token: nano %s/.env\n' "$TARGET"
printf 'Jalankan manual: %s/start-background.sh\n' "$TARGET"
printf 'Hentikan: %s/stop-background.sh\n' "$TARGET"
printf '\nAuto-start Termux biasa sudah ditambahkan ke %s\n' "$BASHRC"
printf 'Bridge akan mulai saat membuka sesi Termux baru. Termux:Boot tidak diperlukan.\n'
printf 'Cek log: tail -f %s/.cache/corelink/bridge.log\n' "$HOME"
