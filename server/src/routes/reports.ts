import { Router, Response } from "express";
import pool from "../db/client";
import { authenticateToken, AuthRequest } from "../middleware/auth";

const router = Router();

// GET /reports/summary - ringkasan laporan
router.get("/summary", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { outletId } = req.query;

  try {
    let whereClause = "";
    let params: (string | number)[] = [];

    if (outletId) {
      whereClause = "WHERE t.outlet_id::text = $1::text OR o.client_id::text = $1::text";
      params = [String(outletId)];
    } else if (req.outletId) {
      whereClause = "WHERE t.outlet_id::text = $1::text OR o.client_id::text = $1::text";
      params = [String(req.outletId)];
    } else {
      whereClause = "WHERE o.owner_id::text = $1::text";
      params = [String(req.userId)];
    }

    const result = await pool.query(
      `SELECT
         COUNT(t.id) AS total_transactions,
         COALESCE(SUM(t.total_amount), 0) AS total_income,
         COUNT(CASE WHEN t.status = 'pending' THEN 1 END) AS total_pending,
         COUNT(CASE WHEN t.status = 'process' THEN 1 END) AS total_process,
         COUNT(CASE WHEN t.status = 'done' THEN 1 END) AS total_done,
         COUNT(CASE WHEN t.status = 'picked' THEN 1 END) AS total_picked
       FROM transactions t
       LEFT JOIN outlets o ON t.outlet_id::text = o.id::text OR t.outlet_id::text = o.client_id::text
       ${whereClause}`,
      params
    );

    const row = result.rows[0];
    res.json({
      total_transactions: parseInt(row.total_transactions, 10),
      total_income: parseFloat(row.total_income),
      total_pending: parseInt(row.total_pending, 10),
      total_process: parseInt(row.total_process, 10),
      total_done: parseInt(row.total_done, 10),
      total_picked: parseInt(row.total_picked, 10),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

export default router;
