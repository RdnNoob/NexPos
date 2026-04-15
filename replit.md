# NexPos

## Project Overview
NexPos is a laundry POS project with two Android apps and one Node.js/Express PostgreSQL backend API.

## Structure
- `app-admin/`: Android owner/admin app
- `app-laundry/`: Android cashier/outlet app
- `core/`: shared Android module
- `server/`: Express + TypeScript backend API

## Replit Setup
- Primary runnable service in Replit is the backend API in `server/`.
- The backend runs on port `5000` for the Replit web preview.
- PostgreSQL uses Replit's built-in database through `DATABASE_URL`.
- Development workflow command: `cd server && npm run dev`
- Health endpoint: `/api/healthz`

## Notes
- The web preview shows the API response because this repository does not include a web frontend.
- Android API base URL is configured in `core/build.gradle.kts`.
- Runtime database migrations normalize legacy Railway UUID `outlet_id` columns on `devices` and `transactions` back to integer IDs expected by the Android apps/API.