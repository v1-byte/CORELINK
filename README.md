# CORELINK — AI Connector Command Center

Aplikasi + Bridge untuk menghubungkan **chat AI lokal (Ollama)** dengan connector (GitHub, GitLab, Vercel, dll.) lewat **CORELINK Bridge** di Termux.

## Cepat mulai

1. **Termux — Bridge**
   ```bash
   # lihat CORELINK_BRIDGE.md dan OLLAMA_TERMUX.md
   cd CORELINK/bridge
   bash start-termux.sh
   ```
2. **Ollama**
   ```bash
   ollama serve
   ```
3. **APK Android** — unduh rilis: [Releases](https://github.com/v1-byte/CORELINK/releases)  
   Tab **LINK** (Bridge) → Connect ke `http://127.0.0.1:8787`

## Navbar Android

| Tab | Fungsi |
|-----|--------|
| **CHAT** | Chat dengan Ollama / agent |
| **LINK** | Endpoint Bridge & model |
| **TOOLS** | Daftar connector |
| **SETUP** | Perintah instalasi Termux |
| **AI** | System prompt + temperature (kepandaian) |
| **REMOTE** | MyBase + A-Connect (lihat di bawah) |

## Modul REMOTE (pengingat)

Detail lengkap: **[REMOTE_MODULES.md](./REMOTE_MODULES.md)**

- **MyBase** — analisis / proteksi **perangkat user sendiri** (UI dulu, engine bertahap).
- **A-Connect** — remote **support** Admin ↔ User lewat **token + izin**. Bukan remote diam-diam.

> Engine remote penuh dan scan MyBase **belum selesai**; yang ada sekarang daftar UI + alur token lokal.

## Dokumen lain

- [ANDROID_RELEASE.md](./ANDROID_RELEASE.md) — catatan rilis APK
- [CORELINK_BRIDGE.md](./CORELINK_BRIDGE.md) — Bridge
- [OLLAMA_TERMUX.md](./OLLAMA_TERMUX.md) — Ollama di Termux
- [REMOTE_MODULES.md](./REMOTE_MODULES.md) — roadmap MyBase & A-Connect

## Struktur repo

- `android/` — APK native
- `bridge/` — server Bridge (Node)
- `client/` — UI web (Vite)
- `server/` — server terkait template

## Lisensi

MIT (lihat package.json).
