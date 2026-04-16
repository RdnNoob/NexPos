# NexPos

## Project Overview
NexPos is a laundry POS project with two Android apps and one Node.js/Express PostgreSQL backend API.

## Structure
- `app-admin/`: Android owner/admin app
- `app-laundry/`: Android cashier/outlet app
- `core/`: shared Android module
- `server/`: Express + TypeScript backend API

## Replit Setup
- Primary deployment target is Railway, not Replit.
- Replit is used for code editing and validation only; no Replit server workflow is required.
- PostgreSQL uses Replit's built-in database through `DATABASE_URL`.
- Health endpoint: `/api/healthz`

## Notes
- This repository does not include a web frontend.
- Android API base URL is configured in `core/build.gradle.kts`.
- Railway may have UUID primary keys on `outlets.id`; the API exposes numeric `client_id` values to Android while storing internal relations as text to avoid UUID/integer operator errors.
- The owner app is now branded as "NexPos Owners".
- A hidden super-admin panel is available from the owner app by tapping the NexPos Owners title/logo five times. Default credentials are username `admin` and password `admin!@#`, with optional Railway env overrides `SUPER_ADMIN_USERNAME` and `SUPER_ADMIN_PASSWORD`.
- Password reset OTP requests are stored in `super_admin_events` for manual Customer Service handling. The app directs users to Gmail or WhatsApp Customer Service instead of relying on SMTP delivery.
- Owner notifications are stored in the `notifications` table and exposed through `/api/notifications`; transaction create/status changes insert notification records for the owner.