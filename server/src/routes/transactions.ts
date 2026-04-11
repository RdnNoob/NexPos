import { Router, Response } from "express";
import pool from "../db/client";
import { authenticateToken, AuthRequest } from "../middleware/auth";

const router = Router();

const VALID_STATUSES = ["diterima", "dicuci", "disetrika", "selesai"];

router.get("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { outletId } = req.query;

  try {
    let query: string;
    let params: (number | undefined)[];

    if (outletId) {
      query = `
        SELECT t.*, o.name as outlet_name 
        FROM transactions t 
        LEFT JOIN outlets o ON t.outlet_id = o.id 
        WHERE t.outlet_id = $1 AND o.owner_id = $2
        ORDER BY t.created_at DESC
      `;
      params = [parseInt(outletId as string), req.userId];
    } else if (req.outletId) {
      // FIX: Kasir device — hanya tampilkan transaksi outlet yang sedang aktif
      query = `
        SELECT t.*, o.name as outlet_name 
        FROM transactions t 
        LEFT JOIN outlets o ON t.outlet_id = o.id 
        WHERE t.outlet_id = $1
        ORDER BY t.created_at DESC
      `;
      params = [req.outletId];
    } else {
      query = `
        SELECT t.*, o.name as outlet_name 
        FROM transactions t 
        LEFT JOIN outlets o ON t.outlet_id = o.id 
        WHERE o.owner_id = $1
        ORDER BY t.created_at DESC
      `;
      params = [req.userId];
    }

    const result = await pool.query(query, params);
    res.json({
      transactions: result.rows.map(t => ({
        id: t.id,
        outletId: t.outlet_id,
        outletName: t.outlet_name,
        customer: t.customer,
        service: t.service,
        amount: parseFloat(t.amount),
        status: t.status,
        createdAt: t.created_at,
      })),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

router.post("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { customer, service, amount, outletId } = req.body;

  if (!customer || !service || !amount) {
    res.status(400).json({ message: "Customer, layanan, dan harga wajib diisi" });
    return;
  }

  try {
    // FIX: Prioritaskan outlet dari JWT token (kasir device), lalu dari body, lalu fallback
    let targetOutletId = outletId || req.outletId;
    if (!targetOutletId) {
      const outletResult = await pool.query(
        "SELECT id FROM outlets WHERE owner_id = $1 LIMIT 1",
        [req.userId]
      );
      if (outletResult.rows.length === 0) {
        res.status(400).json({ message: "Outlet tidak ditemukan" });
        return;
      }
      targetOutletId = outletResult.rows[0].id;
    }

    const result = await pool.query(
      `INSERT INTO transactions (outlet_id, customer, service, amount, status) 
       VALUES ($1, $2, $3, $4, 'diterima') RETURNING *`,
      [targetOutletId, customer, service, parseFloat(amount)]
    );
    const t = result.rows[0];

    res.status(201).json({
      id: t.id,
      outletId: t.outlet_id,
      customer: t.customer,
      service: t.service,
      amount: parseFloat(t.amount),
      status: t.status,
      createdAt: t.created_at,
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

router.put("/status", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { transactionId, status } = req.body;

  if (!transactionId || !status) {
    res.status(400).json({ message: "ID transaksi dan status wajib diisi" });
    return;
  }

  if (!VALID_STATUSES.includes(status.toLowerCase())) {
    res.status(400).json({
      message: `Status tidak valid. Gunakan: ${VALID_STATUSES.join(", ")}`,
    });
    return;
  }

  try {
    const result = await pool.query(
      `UPDATE transactions t SET status = $1, updated_at = NOW()
       FROM outlets o
       WHERE t.id = $2
       AND t.outlet_id = o.id
       AND (o.owner_id = $3 OR t.outlet_id = $4)
       RETURNING t.*`,
      [status.toLowerCase(), transactionId, req.userId, req.outletId ?? null]
    );

    if (result.rows.length === 0) {
      res.status(404).json({ message: "Transaksi tidak ditemukan" });
      return;
    }

    const t = result.rows[0];
    res.json({
      id: t.id,
      outletId: t.outlet_id,
      customer: t.customer,
      service: t.service,
      amount: parseFloat(t.amount),
      status: t.status,
      createdAt: t.created_at,
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

export default router;
