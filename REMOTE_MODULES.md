# CORELINK Remote Modules — Catatan Pengingat

Dokumen ini adalah **pengingat roadmap** untuk modul remote di aplikasi Android.  
**UI daftar sudah ada** di tab **REMOTE**. Engine penuh ditambahkan **bertahap**.

---

## 1. MyBase (Device shield & analysis)

**Tujuan:** analisis dan proteksi **pada perangkat pengguna aplikasi ini sendiri**.

### Yang sudah ada (UI)
- Kartu MyBase di tab REMOTE
- Tombol placeholder: SCAN, LAPORAN

### Yang belum / menyusul
- [ ] Engine scan permission mencurigakan
- [ ] Deteksi APK sumber tidak dikenal
- [ ] Ringkasan kesehatan perangkat
- [ ] Deteksi konfigurasi berisiko
- [ ] Laporan yang bisa dibagikan ke admin support (opsional, dengan izin user)

### Batasan (penting)
- MyBase **bukan** spyware dan **bukan** overlay diam-diam di atas app/situs orang lain.
- Fokus: **perangkat user yang memasang CORELINK**, dengan transparansi.

---

## 2. A-Connect (Remote support Desktop / Android)

**Tujuan:** bantuan jarak jauh **dengan persetujuan eksplisit** (token).

### Peran
| Peran | Fungsi |
|--------|--------|
| **USER** | Pemilik perangkat yang dibantu. Generate token, bagikan ke admin, bisa **STOP SESI**. |
| **ADMIN** | Pemberi bantuan. Menerima token dari user, lalu terhubung (engine menyusul). |

### Alur yang diingat
1. USER buka tab **REMOTE** → **A-Connect** → **SAYA USER**
2. USER tekan **GENERATE TOKEN**
3. USER **SALIN** token → kirim ke ADMIN (chat / nanti QR)
4. ADMIN pilih **SAYA ADMIN** → tempel token → **HUBUNGKAN KE USER**
5. USER bisa **STOP SESI** kapan saja

### Yang sudah ada (UI)
- Mode USER / ADMIN
- Generate / salin / stop token (lokal di HP)
- Input token di sisi admin
- Status teks sesi

### Yang belum / menyusul
- [ ] Server relay sesi (Bridge atau layanan terpisah)
- [ ] Mirror layar / kontrol input (hanya setelah token valid + izin)
- [ ] Share file terbatas untuk perbaikan
- [ ] QR code untuk token
- [ ] Log audit sesi (mulai / selesai)
- [ ] Dukungan Desktop (client terpisah) selaras dengan Android

### Batasan keamanan (wajib diingat)
- **Tidak ada remote diam-diam.**
- Sesi hanya aktif dengan **token yang dibuat user**.
- User selalu bisa menghentikan sesi.
- Jangan menyimpan kontrol permanen tanpa consent berulang.

---

## 3. Navbar terkait

| Tab | Isi |
|-----|-----|
| CHAT | Percakapan AI |
| LINK | Bridge / Ollama endpoint |
| TOOLS | Connector (GitHub, dll.) |
| SETUP | Perintah Termux |
| AI | System prompt + temperature |
| **REMOTE** | **MyBase + A-Connect** |

---

## 4. Versi & file terkait

- UI entry: `android/app/src/main/java/com/v1byte/corelink/MainActivity.java` → `buildRemotePanel()`
- Catatan rilis: `ANDROID_RELEASE.md`
- Bridge (chat/agent): `bridge/server.mjs` — **belum** menangani relay A-Connect; itu fase berikutnya

---

## 5. Prioritas pengembangan (usulan)

1. Token exchange lewat Bridge (validasi admin ↔ user)
2. Heartbeat sesi + timeout otomatis
3. MyBase scan dasar (permission list / installer source)
4. A-Connect channel pesan support (teks dulu, kontrol layar belakangan)
5. Client desktop minimal untuk admin

---

*Terakhir diingat: modul remote = support & proteksi transparan, bukan akses tersembunyi.*
