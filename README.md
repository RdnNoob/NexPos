# NexPos

NexPos adalah aplikasi POS laundry dengan dua aplikasi Android dan satu backend API:

- `app-admin`: aplikasi owner/admin untuk login, registrasi, outlet, device, dashboard, dan monitoring transaksi.
- `app-laundry`: aplikasi kasir/outlet untuk login device, membuat transaksi, melihat daftar transaksi, dan memperbarui status laundry.
- `server`: backend Express + PostgreSQL untuk auth, outlet, device heartbeat, dan transaksi.

## Struktur Project

```text
app-admin/      Aplikasi Android untuk owner/admin
app-laundry/    Aplikasi Android untuk kasir/outlet
core/           Modul Android bersama: API, model, session, theme, komponen UI
gradle/         Konfigurasi Gradle wrapper
server/         Backend API Node.js/Express
```

## Backend Server

Backend menggunakan PostgreSQL melalui environment variable `DATABASE_URL`.

Environment yang umum digunakan:

```env
DATABASE_URL=postgresql://...
JWT_SECRET=isi_secret_yang_aman
JWT_EXPIRES_IN=7d
PORT=3000
NODE_ENV=production
```

Menjalankan server secara lokal:

```bash
cd server
npm install
npm run dev
```

Build dan start server:

```bash
cd server
npm install
npm run build
npm start
```

Health check:

```bash
curl https://nexpos-production-3747.up.railway.app/api/healthz
```

## Database

Saat server start, backend akan memastikan tabel utama tersedia:

- `users`
- `outlets`
- `devices`
- `transactions`

Server juga menjalankan migrasi aman untuk memastikan database lama di Railway memiliki kolom-kolom yang dibutuhkan oleh versi terbaru, termasuk `transactions.updated_at`. Jika database Railway sudah dibuat sebelum kolom ini ditambahkan, kolom akan dibuat otomatis dengan:

```sql
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();
```

## Android

Base URL API berada di:

```text
core/build.gradle.kts
```

Nilai default saat ini:

```text
https://nexpos-production-3747.up.railway.app
```

Build debug APK:

```bash
./gradlew :app-admin:assembleDebug
./gradlew :app-laundry:assembleDebug
```

Build release APK:

```bash
./gradlew :app-admin:assembleRelease
./gradlew :app-laundry:assembleRelease
```

## Alur Penggunaan

1. Owner membuat akun melalui `app-admin`.
2. Owner login dan membuat outlet.
3. Outlet menghasilkan kode aktivasi.
4. Kasir login di `app-laundry` memakai kode aktivasi.
5. Kasir membuat transaksi laundry.
6. Kasir/admin memperbarui status transaksi: `diterima`, `dicuci`, `disetrika`, `selesai`.
7. Device kasir mengirim heartbeat agar status online/offline tetap akurat.

## Catatan Perbaikan Terbaru

- `SessionManager` sudah menyediakan `getOutletId()` dan `getDeviceId()`.
- Backend memastikan kolom `transactions.updated_at` tersedia di database lama maupun baru.
- Response transaksi menyertakan `updatedAt`.
- Client Android membedakan error koneksi dan error parsing response server agar pesan di UI lebih akurat.
- Tampilan auth diberi padding untuk keyboard dan navigation bar.
- Warna navigation bar mengikuti theme agar tidak muncul putih mencolok pada dark mode.

## Endpoint Utama

```text
GET  /api/healthz
POST /api/auth/register
POST /api/auth/login
POST /api/auth/login-device
GET  /api/outlets
POST /api/outlets
GET  /api/devices
POST /api/devices/heartbeat
POST /api/devices/force-logout
GET  /api/transactions
POST /api/transactions
PUT  /api/transactions/status
```
