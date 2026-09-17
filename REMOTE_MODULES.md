# CORELINK Remote Support (izin 2 pihak)

## Yang sudah full di Bridge + App (v1.7+)

| Fitur | Endpoint / UI |
|--------|----------------|
| User buat token | `POST /api/remote/create` · tombol GENERATE TOKEN |
| Admin minta sesi | `POST /api/remote/request` · HUBUNGKAN KE USER |
| User izinkan | `POST /api/remote/accept` · popup + IZINKAN SESI |
| Stop sesi | `POST /api/remote/stop` |
| Status poll | `GET /api/remote/status/:token` |
| Chat support | `POST /api/remote/message` |
| Bagikan info HP | `POST /api/remote/device` |

## Alur
1. Admin **SHARE LINK APK** ke user  
2. User install / buka CORELINK → REMOTE → **USER** → Generate Token (Bridge harus jalan)  
3. User kirim token ke admin  
4. Admin **ADMIN** → tempel token → Hubungkan  
5. User tekan **IZINKAN SESI** (popup)  
6. Sesi **active** → chat support + bagikan info HP  
7. STOP kapan saja  

## Syarat teknis
- **Bridge yang sama** menyimpan sesi (memori). Admin & user harus mengarah ke Bridge yang sama (`http://IP:8787` jika beda HP).  
- Bukan remote layar/kontrol input (belum). Bukan akses FB/IG diam-diam.  

## Bukan RAT
Tidak ada kontrol sistem penuh atau overlay app pihak ketiga tanpa sepengetahuan user.
