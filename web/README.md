# NexPos Web Kasir

Antarmuka web kasir untuk NexPos Laundry.

## Fitur
- Login dengan JWT (token dari NexPos API)
- Dashboard ringkasan outlet
- Kasir: pilih customer → pilih layanan → harga otomatis → quantity → total otomatis
- Transaksi: daftar + ubah status (pending → process → done → picked)
- Customer: CRUD + pencarian
- Layanan: CRUD — harga & satuan bisa diubah kapan saja
- Laporan: ringkasan pendapatan & status

## Setup

```bash
# Install dependencies
npm install

# Jalankan dev server
npm run dev

# Build production
npm run build
```

## Konfigurasi

Buat file `.env` di folder `web/`:
```
VITE_API_URL=https://your-nexpos-server.com
```

Jika `VITE_API_URL` kosong, akan menggunakan URL relatif (cocok jika web di-serve dari server yang sama).

## Teknologi
- React 18
- Vite
- TypeScript
- Tailwind CSS v4
- React Query
- Wouter (routing)
