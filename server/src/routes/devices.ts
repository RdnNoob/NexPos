import { Router, Response } from "express";
import pool from "../db/client";
import { authenticateToken, AuthRequest } from "../middleware/auth";

const router = Router();

router.get("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const result = await pool.query(
      `SELECT d.*, o.name as outlet_name 
       FROM devices d 
       LEFT JOIN outlets o ON d.outlet_id = o.id 
       WHERE d.owner_id = $1 
       ORDER BY d.last_seen DESC NULLS LAST`,
      [req.userId]
    );
    res.json({
      devices: result.rows.map(d => ({
        id: d.id,
        deviceName: d.device_name,
        deviceId: d.device_id,
        status: d.status,
        outletId: d.outlet_id,
        outletName: d.outlet_name,
        lastSeen: d.last_seen,
      })),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

router.post("/heartbeat", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { deviceId } = req.body;
  if (!deviceId) {
    res.status(400).json({ message: "Device ID wajib diisi" });
    return;
  }

  try {
    await pool.query(
      "UPDATE devices SET last_seen = NOW(), status = 'online' WHERE device_id = $1",
      [deviceId]
    );

    await pool.query(
      `UPDATE devices SET status = 'offline' 
       WHERE owner_id = $1 
       AND device_id != $2 
       AND (last_seen IS NULL OR last_seen < NOW() - INTERVAL '2 minutes')`,
      [req.userId, deviceId]
    );

    res.json({ message: "Heartbeat berhasil dikirim" });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

router.post("/force-logout", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { deviceId } = req.body;
  if (!deviceId) {
    res.status(400).json({ message: "Device ID wajib diisi" });
    return;
  }

  try {
    const result = await pool.query(
      "UPDATE devices SET status = 'offline', refresh_token = NULL WHERE id = $1 AND owner_id = $2 RETURNING id",
      [deviceId, req.userId]
    );

    if (result.rows.length === 0) {
      res.status(404).json({ message: "Device tidak ditemukan" });
      return;
    }

    res.json({ message: "Device berhasil di-force logout" });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

export default router;
