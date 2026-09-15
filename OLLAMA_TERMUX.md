# Menghubungkan CORELINK ke Ollama di Termux

CORELINK sekarang dapat memanggil Ollama melalui `POST /api/generate`. Klik **Connect Ollama** di header aplikasi, lalu masukkan endpoint Ollama.

## Jika CORELINK dan browser berjalan di HP yang sama

Gunakan:

```text
http://127.0.0.1:11434
```

Di Termux/Ubuntu:

```bash
ollama serve
```

## Jika CORELINK dibuka dari laptop atau perangkat lain

Gunakan IP lokal HP, misalnya:

```text
http://192.168.1.25:11434
```

Ollama harus listen pada semua interface dan mengizinkan request browser:

```bash
export OLLAMA_HOST=0.0.0.0:11434
export OLLAMA_ORIGINS=*
ollama serve
```

Pastikan HP dan perangkat browser berada di jaringan Wi-Fi yang sama dan firewall/router mengizinkan port `11434`. Jangan membuka port Ollama langsung ke internet tanpa autentikasi atau VPN.

## Model yang digunakan

CORELINK mengirim request ke model berikut:

```text
qwen2.5-coder:1.5b
```

Siapkan modelnya sekali:

```bash
ollama pull qwen2.5-coder:1.5b
```

CORELINK menggunakan `stream: false` dan `num_ctx: 2048`, sesuai target perangkat Termux dengan RAM terbatas.

## Status saat ini

- UI connector dan connection flow: aktif.
- Endpoint Ollama: dapat dikonfigurasi dari tombol **Connect Ollama**.
- Health check: CORELINK memanggil `/api/tags` sebelum menandai Ollama online.
- Chat: ketika online, prompt dikirim ke `/api/generate`; jika gagal, aplikasi kembali ke mode preview.
- Keamanan: endpoint Ollama tidak diberi autentikasi oleh CORELINK. Gunakan jaringan lokal tepercaya, VPN, atau reverse proxy berautentikasi.
