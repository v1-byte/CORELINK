# CORELINK Bridge untuk Termux

CORELINK sekarang memakai arsitektur bridge lokal:

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

Bridge menyimpan token di environment Termux, bukan di APK. Endpoint layanan cloud hanya aktif jika tokennya diisi di `.env`. Docker dan Cloudflared dipanggil sebagai proses lokal; GitHub, GitLab, Vercel, Supabase, dan HuggingFace dipanggil melalui API resmi.

## Instalasi Termux

```bash
git clone https://github.com/v1-byte/CORELINK.git
cd CORELINK/bridge
cp config.template .env
nano .env
bash start-termux.sh
```

Bridge berjalan di `http://127.0.0.1:8787`.

## Endpoint utama

| Endpoint | Fungsi |
|---|---|
| `GET /health` | Status bridge |
| `GET /api/connectors` | Status Ollama, Docker, Cloudflared, dan konfigurasi cloud |
| `GET /api/ollama/tags` | Daftar model Ollama |
| `POST /api/ollama/generate` | Proxy chat streaming ke Ollama |
| `GET /api/github/repos` | Repository GitHub milik token |
| `GET /api/gitlab/projects` | Project GitLab token |
| `GET /api/vercel/projects` | Project Vercel token |
| `GET /api/supabase/projects` | Project Supabase token |
| `GET /api/huggingface/models` | Model publik HuggingFace |
| `POST /api/docker` | Command Docker terbatas dari bridge |
| `POST /api/cloudflared` | Command Cloudflared dari Bridge |
| `GET /api/tools` | Registry tool yang dapat dipilih agent |
| `POST /api/agent` | Chat agent: memilih tool berdasarkan permintaan dan mengembalikan hasil ke chat; menerima `attachment` base64 |

## Environment token

Isi hanya token dengan scope minimum di `.env`: `GITHUB_TOKEN`, `GITLAB_TOKEN`, `VERCEL_TOKEN`, `SUPABASE_ACCESS_TOKEN`, dan `HF_TOKEN`. Jangan commit `.env` atau membagikan port bridge ke internet. Untuk akses dari perangkat lain, gunakan jaringan tepercaya/VPN dan ubah `CORELINK_BRIDGE_HOST` secara sadar.

## Batasan release ini

Adapter read/status dan proxy chat tersedia. `/api/agent` menjadi router chat-first: permintaan GitHub, GitLab, Vercel, Supabase, HuggingFace/Meta, Docker, atau Cloudflared diarahkan ke tool yang sesuai; pertanyaan umum diarahkan ke Ollama. Operasi mutasi berisiko seperti push repository, delete project, deploy production, dan perubahan database memerlukan `confirmed: true` dan belum diekspos sebagai operasi mutasi default.

APK dapat memilih gambar, HTML, TXT, JSON, PDF, dan tipe file lain melalui tombol `+`. Gambar dikirim ke model Ollama sebagai `images`; file non-gambar dibaca sebagai UTF-8 dan dikirim sebagai konteks teks dengan batas 120.000 karakter.
