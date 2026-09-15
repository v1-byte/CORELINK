#!/data/data/com.termux/files/usr/bin/bash
set -e
pkg update -y
pkg install -y nodejs git curl
mkdir -p "$HOME/corelink-bridge"
cp -R ./* "$HOME/corelink-bridge/"
cd "$HOME/corelink-bridge"
[ -f .env ] || cp .env.example .env
printf '\nCORELINK Bridge installed at %s\n' "$HOME/corelink-bridge"
printf 'Edit .env, then run: cd ~/corelink-bridge && npm start\n'
