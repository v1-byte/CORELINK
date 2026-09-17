# CORELINK Bridge untuk Termux

CORELINK memakai bridge lokal untuk menghubungkan APK dengan Ollama dan connector:

```text
APK CORELINK → http://127.0.0.1:8787 → CORELINK Bridge
                                     ├─ Ollama
                                     ├─ Docker
                                     ├─ Cloudflared
                                     ├─ GitHub API
                                     ├─ GitLab API
                                     ├─ Vercel API
                                     ├─ Supabase API
                                     └─ HuggingFace API
```

Bridge menyimpan token di environment Termux, bukan di APK. Endpoint layanan cloud hanya aktif jika tokennya diisi di `.env`.

## Instalasi sekali

Jalankan dari folder `bridge` pada repository:

```bash
bash install-termux.sh
```

Installer akan memasang Node.js, menyalin bridge ke `~/corelink-bridge`, membuat `.env`, dan menambahkan auto-start ke `~/.bashrc`. Jadi bridge otomatis berjalan setiap kali membuka sesi shell Termux biasa. Termux:Boot tidak diperlukan.

## Menjalankan dan menghentikan

```bash
cd ~/corelink-bridge
nano .env
./start-background.sh
./stop-background.sh
```

`start-background.sh` menjalankan bridge dengan `nohup`, jadi bridge tidak bergantung pada jendela Termux yang sedang terbuka. Log tersedia di `~/.cache/corelink/bridge.log`.

Bridge berjalan di `http://127.0.0.1:8787`. Pada APK, buka tab **LINK**, masukkan alamat tersebut, lalu tekan **Connect**.

## Ollama

Jika Ollama terpasang, `start-termux.sh` akan mencoba menjalankan `ollama serve` bila Ollama belum online. Untuk model default:

```bash
ollama pull qwen2.5-coder:1.5b
```

Jika tidak membutuhkan Ollama, bridge tetap dapat dipakai untuk status connector dan API cloud.

## Endpoint utama

| Endpoint | Fungsi |
|---|---|
| `GET /health` | Status bridge |
| `GET /api/connectors` | Status Ollama, Docker, Cloudflared, dan konfigurasi cloud |
| `GET /api/ollama/tags` | Daftar model Ollama |
| `POST /api/ollama/generate` | Proxy chat ke Ollama |
| `GET /api/github/repos` | Repository GitHub milik token |
| `GET /api/gitlab/projects` | Project GitLab token |
| `GET /api/vercel/projects` | Project Vercel token |
| `GET /api/supabase/projects` | Project Supabase token |
| `GET /api/huggingface/models` | Model publik HuggingFace |
| `GET /api/tools` | Registry tool agent |
| `POST /api/agent` | Chat agent dan pemilihan tool |

## Environment token

Isi hanya token dengan scope minimum di `.env`: `GITHUB_TOKEN`, `GITLAB_TOKEN`, `VERCEL_TOKEN`, `SUPABASE_ACCESS_TOKEN`, dan `HF_TOKEN`. Jangan commit `.env` atau membagikan port bridge ke internet. Token GitHub yang pernah dikirim di chat harus segera dicabut dan dibuat ulang dari GitHub Settings.

## Troubleshooting

Cek apakah bridge aktif:

```bash
curl http://127.0.0.1:8787/health
```

Lihat log startup:

```bash
tail -f ~/.cache/corelink/bridge.log
```

Jika belum otomatis, tutup dan buka kembali Termux agar `~/.bashrc` dibaca. Pastikan optimasi baterai untuk Termux dinonaktifkan agar proses background tidak dihentikan Android.
