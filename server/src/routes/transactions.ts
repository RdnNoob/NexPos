import { Router, Response } from "express";
import pool from "../db/client";
import { authenticateToken, AuthRequest } from "../middleware/auth";

const router = Router();

const VALID_STATUSES = ["diterima", "dicuci", "disetrika", "selesai"];

router.get("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { outletId } = req.query;

  try {
    let result;
    if (outletId) {
      result = await pool.query(
        `SELECT t.*, o.name as outlet_name
         FROM transactions t
         LEFT JOIN outlets o ON t.outlet_id = o.id
         WHERE t.outlet_id = $1
         ORDER BY t.created_at DESC`,
        [outletId]
      );
    } else if (req.outletId) {
      // Kasir: hanya lihat transaksi outlet sendiri
      result = await pool.query(
        `SELECT t.*, o.name as outlet_name
         FROM transactions t
         LEFT JOIN outlets o ON t.outlet_id = o.id
         WHERE t.outlet_id = $1
         ORDER BY t.created_at DESC`,
        [req.outletId]
      );
    } else {
      // Admin: lihat semua transaksi miliknya
      result = await pool.query(
        `SELECT t.*, o.name as outlet_name
         FROM transactions t
         LEFT JOIN outlets o ON t.outlet_id = o.id
         WHERE o.owner_id = $1
         ORDER BY t.created_at DESC`,
        [req.userId]
      );
    }

    res.json({
      transactions: result.rows.map((t) => ({
        id: t.id,
        outletId: t.outlet_id,
        customer: t.customer,
        service: t.service,
        amount: parseFloat(t.amount),
        status: t.status,
        createdAt: t.created_at,
        updatedAt: t.updated_at,
        outletName: t.outlet_name ?? null,
      })),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

router.post("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { customer, service, amount } = req.body;

  if (!customer || !service || amount === undefined || amount === null) {
    res.status(400).json({ message: "Customer, layanan, dan harga wajib diisi" });
    return;
  }

  const amountNum = parseFloat(amount);
  if (isNaN(amountNum) || amountNum <= 0) {
    res.status(400).json({ message: "Harga tidak valid" });
    return;
  }

  const outletId = req.outletId;
  if (!outletId) {
    res.status(403).json({ message: "Hanya kasir yang bisa membuat transaksi" });
    return;
  }

  try {
    const result = await pool.query(
      `INSERT INTO transactions (outlet_id, customer, service, amount, status)
       VALUES ($1, $2, $3, $4, 'diterima')
       RETURNING *`,
      [outletId, customer.trim(), service.trim(), amountNum]
    );
    const t = result.rows[0];

    const outletResult = await pool.query(
      "SELECT name FROM outlets WHERE id = $1",
      [outletId]
    );
    const outletName = outletResult.rows[0]?.name ?? null;

    res.status(201).json({
      id: t.id,
      outletId: t.outlet_id,
      customer: t.customer,
      service: t.service,
      amount: parseFloat(t.amount),
      status: t.status,
      createdAt: t.created_at,
      updatedAt: t.updated_at,
      outletName,
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
       RETURNING t.*, o.name as outlet_name`,
      [status.toLowerCase(), transactionId, req.userId, req.outletId ?? null]
    );

    if (result.rows.length === 0) {
      res.status(404).json({ message: "Transaksi tidak ditemukan atau akses ditolak" });
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
      updatedAt: t.updated_at,
      outletName: t.outlet_name ?? null,
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

export default router;
