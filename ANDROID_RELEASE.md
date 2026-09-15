# CORELINK Android release

Build terakhir: **1.1.1 / versionCode 5**.

Fitur yang tersedia:

- WebView asset loader berbasis `WebViewAssetLoader`, bukan pemuatan `file://` langsung.
- Fallback screen dan logging tag `CORELINK` untuk kegagalan startup/WebView.
- Launcher icon CORELINK.
- Koneksi Ollama melalui endpoint yang dapat dikonfigurasi.
- Auto-check endpoint ketika aplikasi dibuka.
- Health check `/api/tags` dengan timeout.
- Daftar model Ollama dan pemilihan model.
- Streaming respons `/api/generate`.
- Timeout 120 detik dan tombol Stop.
- Endpoint dan model terakhir disimpan secara lokal.
- Layout mobile dirapikan agar sidebar tidak menutupi chat, menu connector dapat dibuka/tutup, dan modal setup menyesuaikan layar HP.
- Panduan Termux menjelaskan `pkg install`, `ollama serve`, endpoint Bridge `8787`, dan perbedaan port Ollama `11434`.
- APK terbaru memakai dashboard Android native dengan header branding, kartu koneksi Bridge, pemilih model, chat workspace, dan input pesan; WebView tidak dipakai untuk layar utama native.

## Ollama di Termux

```bash
ollama pull qwen2.5-coder:1.5b
ollama serve
```

Untuk CORELINK dan Ollama pada HP yang sama gunakan `http://127.0.0.1:11434`. Untuk perangkat lain gunakan IP LAN HP dan jalankan Ollama dengan `OLLAMA_HOST=0.0.0.0:11434`.

## Pengujian

TypeScript check, Vite production build, Gradle release build, APK metadata, dan Android APK signature sudah diverifikasi. Pengujian pada perangkat fisik tetap diperlukan karena kompatibilitas Android System WebView dan ROM berbeda-beda. Jika aplikasi masih gagal dibuka, ambil log dengan `adb logcat -s CORELINK:V`.
