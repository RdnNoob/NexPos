import { Router, Response } from "express";
import pool from "../db/client";
import { authenticateToken, AuthRequest } from "../middleware/auth";

const router = Router();

// GET /reports/summary - ringkasan laporan dengan status laundry yang benar
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
         COUNT(CASE WHEN t.status = 'diterima' THEN 1 END) AS total_diterima,
         COUNT(CASE WHEN t.status = 'dicuci' THEN 1 END) AS total_dicuci,
         COUNT(CASE WHEN t.status = 'disetrika' THEN 1 END) AS total_disetrika,
         COUNT(CASE WHEN t.status = 'selesai' THEN 1 END) AS total_selesai,
         COUNT(CASE WHEN t.status = 'dibatalkan' THEN 1 END) AS total_dibatalkan
       FROM transactions t
       LEFT JOIN outlets o ON t.outlet_id::text = o.id::text OR t.outlet_id::text = o.client_id::text
       ${whereClause}`,
      params
    );

    const row = result.rows[0];
    res.json({
      totalTransactions: parseInt(row.total_transactions, 10),
      totalIncome: parseFloat(row.total_income),
      totalDiterima: parseInt(row.total_diterima, 10),
      totalDicuci: parseInt(row.total_dicuci, 10),
      totalDisetrika: parseInt(row.total_disetrika, 10),
      totalSelesai: parseInt(row.total_selesai, 10),
      totalDibatalkan: parseInt(row.total_dibatalkan, 10),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

// GET /reports/daily - ringkasan harian 7 hari terakhir
router.get("/daily", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { outletId } = req.query;

  try {
    let filterClause = "";
    let ownerClause = "";
    let params: (string | number)[] = [];

    if (outletId) {
      filterClause = "AND (t.outlet_id::text = $1::text OR o.client_id::text = $1::text)";
      params = [String(outletId)];
    } else if (req.outletId) {
      filterClause = "AND (t.outlet_id::text = $1::text OR o.client_id::text = $1::text)";
      params = [String(req.outletId)];
    } else {
      ownerClause = "AND o.owner_id::text = $1::text";
      params = [String(req.userId)];
    }

    const result = await pool.query(
      `SELECT
         DATE(t.created_at) AS date,
         COUNT(t.id) AS count,
         COALESCE(SUM(t.total_amount), 0) AS income
       FROM transactions t
       LEFT JOIN outlets o ON t.outlet_id::text = o.id::text OR t.outlet_id::text = o.client_id::text
       WHERE t.created_at >= NOW() - INTERVAL '30 days'
         ${filterClause}${ownerClause}
       GROUP BY DATE(t.created_at)
       ORDER BY date DESC
       LIMIT 30`,
      params
    );

    res.json({
      days: result.rows.map((r) => ({
        date: r.date,
        count: parseInt(r.count, 10),
        income: parseFloat(r.income),
      })),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

export default router;
