# CORELINK Remote Modules — Izin 2 pihak

Remote di CORELINK **hanya** untuk support dengan **izin Admin + User**.  
Bukan remote diam-diam, bukan akses ke FB/IG/TikTok tanpa sepengetahuan user.

---

## Nama modul

| Peran | Nama | Keterangan |
|--------|------|------------|
| **Admin** | **CoreLink Desk** (Connector Android / PC) | Aplikasi/admin utama yang meminta sesi |
| **User** | **CoreLink Assist** | APK yang di-share ke pihak yang dibantu |

CORELINK (app utama) menampung tab **REMOTE** untuk kedua alur.

---

## Alur wajib (2 pihak)

1. **USER** install CoreLink Assist (atau pakai tab USER di app) → setuju syarat support  
2. **USER** tekan **Generate Token** → kirim token ke Admin  
3. **ADMIN** (CoreLink Desk) tempel token → kirim permintaan sesi  
4. **USER** dapat **popup konfirmasi** “Izinkan support remote?” → harus **YA**  
5. Sesi aktif hanya setelah langkah 4  
6. **USER atau ADMIN** bisa **STOP** sesi kapan saja  

Tanpa token **atau** tanpa konfirmasi user → **tidak ada sesi**.

---

## Yang boleh (roadmap bertahap)

- [x] UI token USER / ADMIN  
- [x] Bagikan link APK + salin syarat izin  
- [ ] Relay sesi lewat Bridge (setelah kedua pihak setuju)  
- [ ] Notifikasi “sesi aktif” di sisi user  
- [ ] Berbagi layar / file **terbatas** hanya dalam sesi disetujui  
- [ ] Log audit: siapa setuju, kapan mulai/stop  

## Yang tidak dikerjakan

- Remote tanpa izin user  
- Overlay / popup saat buka setting FB, IG, TikTok untuk mengambil alih  
- Akses penuh diam-diam ke file & pengaturan sistem  
- Spyware / RAT tersembunyi  

---

## MyBase

Analisis **perangkat sendiri** (bukan remote orang lain).  
Lihat tab REMOTE → MyBase.

---

*Prinsip: support transparan, izin 2 pihak, stop kapan saja.*
