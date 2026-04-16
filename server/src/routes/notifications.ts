import { Router, Response } from "express";
import pool from "../db/client";
import { authenticateToken, AuthRequest } from "../middleware/auth";

const router = Router();

router.get("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const result = await pool.query(
      "SELECT id, title, message, type, is_read, created_at FROM notifications WHERE owner_id::text = $1::text ORDER BY created_at DESC LIMIT 100",
      [String(req.userId)]
    );
    res.json({
      notifications: result.rows.map((n) => ({
        id: n.id,
        title: n.title,
        message: n.message,
        type: n.type,
        isRead: n.is_read,
        createdAt: n.created_at,
      })),
    });
  } catch (err) {
    console.error("[notifications list]", err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

router.post("/:id/read", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const result = await pool.query(
      "UPDATE notifications SET is_read = TRUE WHERE id = $1 AND owner_id::text = $2::text RETURNING id",
      [req.params.id, String(req.userId)]
    );
    if (result.rows.length === 0) {
      res.status(404).json({ message: "Notifikasi tidak ditemukan" });
      return;
    }
    res.json({ message: "Notifikasi ditandai dibaca" });
  } catch (err) {
    console.error("[notifications read]", err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

export default router;