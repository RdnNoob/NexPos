import { Router, Response } from "express";
import pool from "../db/client";
import { authenticateToken, AuthRequest } from "../middleware/auth";

const router = Router();

const VALID_STATUSES = ["pending", "process", "done", "picked", "diterima", "dicuci", "disetrika", "selesai"];

function safeInt(value: unknown): number {
  const n = Number(value);
  return Number.isFinite(n) ? Math.floor(n) : 0;
}

// GET /transactions - ambil semua transaksi dengan JOIN customer & service
router.get("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { outletId } = req.query;

  try {
    let result;
    if (outletId) {
      result = await pool.query(
        `SELECT t.*,
                c.name AS customer_name,
                s.name AS service_name,
                s.price AS service_price,
                s.unit AS service_unit,
                o.client_id AS outlet_client_id,
                o.name AS outlet_name
         FROM transactions t
         LEFT JOIN customers c ON t.customer_id = c.id
         LEFT JOIN services s ON t.service_id = s.id
         LEFT JOIN outlets o ON t.outlet_id::text = o.id::text OR t.outlet_id::text = o.client_id::text
         WHERE t.outlet_id::text = $1::text OR o.client_id::text = $1::text
         ORDER BY t.created_at DESC`,
        [outletId]
      );
    } else if (req.outletId) {
      result = await pool.query(
        `SELECT t.*,
                c.name AS customer_name,
                s.name AS service_name,
                s.price AS service_price,
                s.unit AS service_unit,
                o.client_id AS outlet_client_id,
                o.name AS outlet_name
         FROM transactions t
         LEFT JOIN customers c ON t.customer_id = c.id
         LEFT JOIN services s ON t.service_id = s.id
         LEFT JOIN outlets o ON t.outlet_id::text = o.id::text OR t.outlet_id::text = o.client_id::text
         WHERE t.outlet_id::text = $1::text OR o.client_id::text = $1::text
         ORDER BY t.created_at DESC`,
        [req.outletId]
      );
    } else {
      result = await pool.query(
        `SELECT t.*,
                c.name AS customer_name,
                s.name AS service_name,
                s.price AS service_price,
                s.unit AS service_unit,
                o.client_id AS outlet_client_id,
                o.name AS outlet_name
         FROM transactions t
         LEFT JOIN customers c ON t.customer_id = c.id
         LEFT JOIN services s ON t.service_id = s.id
         LEFT JOIN outlets o ON t.outlet_id::text = o.id::text OR t.outlet_id::text = o.client_id::text
         WHERE o.owner_id::text = $1::text
         ORDER BY t.created_at DESC`,
        [req.userId]
      );
    }

    res.json({
      transactions: result.rows.map((t) => ({
        id: t.id,
        outletId: safeInt(t.outlet_client_id ?? t.outlet_id),
        outletName: t.outlet_name ?? null,
        customerId: t.customer_id,
        customerName: t.customer_name ?? t.customer ?? null,
        serviceId: t.service_id,
        serviceName: t.service_name ?? t.service ?? null,
        servicePrice: t.service_price ?? null,
        serviceUnit: t.service_unit ?? null,
        quantity: t.quantity ?? 1,
        totalAmount: parseFloat(t.total_amount ?? t.amount ?? 0),
        status: t.status,
        createdAt: t.created_at,
        updatedAt: t.updated_at,
      })),
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

// POST /transactions - buat transaksi baru (harga otomatis dari service)
router.post("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { outletId, customerId, serviceId, quantity } = req.body;

  // Flow baru: wajib pakai customerId + serviceId
  if (!outletId || !customerId || !serviceId) {
    res.status(400).json({ message: "outletId, customerId, dan serviceId wajib diisi" });
    return;
  }

  const qty = Number(quantity);
  if (!quantity || isNaN(qty) || qty <= 0) {
    res.status(400).json({ message: "Quantity harus lebih dari 0" });
    return;
  }

  try {
    // Ambil harga dari tabel services (otomatis)
    const serviceResult = await pool.query(
      "SELECT id, name, price FROM services WHERE id::text = $1::text",
      [serviceId]
    );

    if (!serviceResult.rows.length) {
      res.status(400).json({ message: "Layanan tidak ditemukan" });
      return;
    }

    const service = serviceResult.rows[0];
    const price = service.price ?? 0;
    const total_amount = price * qty; // harga × quantity = total otomatis

    const result = await pool.query(
      `INSERT INTO transactions
         (outlet_id, owner_id, customer_id, service_id, quantity, total_amount, status)
       VALUES ($1::text, $2::text, $3::uuid, $4::uuid, $5::float, $6::integer, 'pending')
       RETURNING *`,
      [outletId, req.userId, customerId, serviceId, qty, total_amount]
    );

    res.status(201).json({ transaction: result.rows[0] });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

// PUT /transactions/:id/status - ubah status transaksi
router.put("/:id/status", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { id } = req.params;
  const { status } = req.body;

  if (!status) {
    res.status(400).json({ message: "Status wajib diisi" });
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
      "UPDATE transactions SET status = $1::text, updated_at = NOW() WHERE id::text = $2::text RETURNING *",
      [status.toLowerCase(), id]
    );

    if (result.rows.length === 0) {
      res.status(404).json({ message: "Transaksi tidak ditemukan" });
      return;
    }

    res.json({ transaction: result.rows[0] });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

// PUT /transactions/status (legacy support)
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
      `UPDATE transactions SET status = $1, updated_at = NOW()
       WHERE id = $2
       RETURNING *`,
      [status.toLowerCase(), transactionId]
    );

    if (result.rows.length === 0) {
      res.status(404).json({ message: "Transaksi tidak ditemukan" });
      return;
    }

    res.json({ transaction: result.rows[0] });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

export default router;
