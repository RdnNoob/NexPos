import dotenv from "dotenv";
dotenv.config();

import app from "./app";
import pool, { initDb } from "./db/client";

const PORT = process.env.PORT || 3000;

async function startServer() {
  await initDb().catch(console.error);

  app.listen(PORT, () => {
    console.log(`[NexPos Server] Berjalan di port ${PORT}`);
    console.log(`[NexPos Server] Environment: ${process.env.NODE_ENV || "development"}`);
  });

  // Background job: setiap 60 detik tandai device yang tidak heartbeat sebagai offline
  setInterval(async () => {
    try {
      const result = await pool.query(
        `UPDATE devices SET status = 'offline'
         WHERE status = 'online'
         AND (last_seen IS NULL OR last_seen < NOW() - INTERVAL '2 minutes')`
      );
      if (result.rowCount && result.rowCount > 0) {
        console.log(`[HeartbeatJob] ${result.rowCount} device ditandai offline`);
      }
    } catch (err) {
      console.error("[HeartbeatJob] Gagal update device offline:", err);
    }
  }, 60_000);
}

startServer();
