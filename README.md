# CORELINK — AI Connector Command Center

Aplikasi + Bridge untuk menghubungkan **chat AI lokal (Ollama)** dengan connector (GitHub, GitLab, Vercel, dll.) lewat **CORELINK Bridge** di Termux.

## Cepat mulai

1. Install **Termux** biasa, sebaiknya dari F-Droid.
2. Di Termux, clone repository dan pasang bridge:

   ```bash
   git clone https://github.com/v1-byte/CORELINK.git
   cd CORELINK/bridge
   bash install-termux.sh
   ```

3. Edit `~/corelink-bridge/.env` jika ingin memakai connector cloud.
4. Tutup lalu buka kembali Termux.
5. APK CORELINK → tab **LINK** → Connect ke `http://127.0.0.1:8787`.

Installer menambahkan auto-start ke `~/.bashrc`, sehingga bridge berjalan otomatis ketika Termux biasa dibuka. Ini bukan auto-start saat Android boot; untuk itu memang diperlukan Termux:Boot.

## Ollama

```bash
ollama serve
ollama pull qwen2.5-coder:1.5b
```

Lihat [CORELINK_BRIDGE.md](./CORELINK_BRIDGE.md) dan [OLLAMA_TERMUX.md](./OLLAMA_TERMUX.md) untuk konfigurasi lengkap.

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
