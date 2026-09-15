# CORELINK Android release

Build terakhir: **1.2.0 / versionCode 6**.

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
- Chat native menjadi agent entry point: Bridge memilih tool Ollama, GitHub, GitLab, Vercel, Supabase, HuggingFace/Meta, Docker, atau Cloudflared berdasarkan permintaan chat. Connector berada di drawer terlipat, bukan memenuhi layar.
- Tombol koneksi dipadatkan menjadi `CONNECT` agar tidak terpotong pada layar HP sempit; tombol `+` pada composer dapat memilih gambar, HTML, TXT, JSON, PDF, atau file lain untuk dikirim ke agent.

## Ollama di Termux

```bash
ollama pull qwen2.5-coder:1.5b
ollama serve
```

Untuk CORELINK dan Ollama pada HP yang sama gunakan `http://127.0.0.1:11434`. Untuk perangkat lain gunakan IP LAN HP dan jalankan Ollama dengan `OLLAMA_HOST=0.0.0.0:11434`.

## Pengujian

TypeScript check, Vite production build, Gradle release build, APK metadata, dan Android APK signature sudah diverifikasi. Pengujian pada perangkat fisik tetap diperlukan karena kompatibilitas Android System WebView dan ROM berbeda-beda. Jika aplikasi masih gagal dibuka, ambil log dengan `adb logcat -s CORELINK:V`.
- Layout revisi: panel Bridge + model berada dalam satu panel navbar yang bisa dibuka/tutup lewat tombol BRIDGE; tombol TOOLS membuka connector drawer; workspace chat diperbesar menjadi area utama.
- Logo AI Connector dan label AI BRAIN memakai animasi pulse terus-menerus; tombol `+` di composer tetap menjadi pemilih attachment.
- Navbar final kini berupa empat tab nyata: `CHAT`, `BRIDGE`, `TOOLS`, dan `SETUP`.
- Kolom pesan final memakai input multi-baris 96dp dengan tombol `+` attachment 46dp dan tombol `SEND` 82dp.
- Pemeriksaan tombol Chat: navbar kini benar-benar menampilkan tombol `CHAT` sebagai tab aktif, bukan label `CHAT / OLLAMA`.
- Composer diperbesar menjadi input multi-baris 96dp agar penulisan pesan nyaman di HP.
- Composer final: tombol `+` berada di dalam kotak input sebelah kiri; input multi-baris tetap besar; tombol `SEND` dipadatkan menjadi 68x52dp di sebelah kanan.
- Composer setelah audit lebar: input memakai seluruh ruang horizontal yang tersisa; tombol SEND dipadatkan menjadi 62x52dp sehingga kolom pesan lebih lebar di layar HP.
- Professional UI pass: safe horizontal spacing reduced for mobile, logo enlarged, elevated navigation/cards/chat surfaces, clearer empty chat state, 14sp composer typography, and improved attachment status text.
- Input visibility fix: composer menjadi 110dp, teks input putih 15sp dengan hint kontras, maksimal 5 baris, dan Activity memakai `adjustResize` supaya keyboard tidak menutupi input.

---

## UI Professional Update (2026-09-16)

Tampilan native Android telah di-upgrade menjadi lebih profesional:

- **Pesan user** di sisi kanan dengan bubble hijau gelap + radius khusus
- **Respons AI** di sisi kiri dengan bubble biru gelap + radius khusus
- Area chat **scrollable** + **auto-scroll** ke bawah saat pesan baru
- **Composer fixed** di bawah layar
- Tombol **+** berada di dalam composer (upload file/gambar)
- Tombol kirim berupa **ikon panah kecil** (➤)
- **Keyboard** tidak menutupi composer (WindowInsets + SOFT_INPUT_ADJUST_RESIZE)
- Status **Thinking...** dengan **animasi titik**
- Navbar **CHAT / BRIDGE / TOOLS / SETUP** sebagai panel terpisah
- Navbar tidak mengganggu area chat
- Warna & spacing diseragamkan agar terlihat clean dan profesional

