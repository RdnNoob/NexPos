import { Router, Response } from "express";
import pool from "../db/client";
import { authenticateToken, AuthRequest } from "../middleware/auth";

const router = Router();

const VALID_STATUSES = ["diterima", "dicuci", "disetrika", "selesai"];

function safeInt(value: unknown): number {
  const n = Number(value);
  return Number.isFinite(n) ? Math.floor(n) : 0;
}

router.get("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { outletId } = req.query;

  try {
    let result;
    if (outletId) {
      result = await pool.query(
        `SELECT t.*, o.client_id as outlet_client_id, o.name as outlet_name
         FROM transactions t
         LEFT JOIN outlets o ON t.outlet_id::text = o.id::text OR t.outlet_id::text = o.client_id::text
         WHERE t.outlet_id::text = $1::text OR o.client_id::text = $1::text
         ORDER BY t.created_at DESC`,
        [outletId]
      );
    } else if (req.outletId) {
      // Kasir: hanya lihat transaksi outlet sendiri
      result = await pool.query(
        `SELECT t.*, o.client_id as outlet_client_id, o.name as outlet_name
         FROM transactions t
         LEFT JOIN outlets o ON t.outlet_id::text = o.id::text OR t.outlet_id::text = o.client_id::text
         WHERE t.outlet_id::text = $1::text OR o.client_id::text = $1::text
         ORDER BY t.created_at DESC`,
        [req.outletId]
      );
    } else {
      // Admin: lihat semua transaksi miliknya
      result = await pool.query(
        `SELECT t.*, o.client_id as outlet_client_id, o.name as outlet_name
         FROM transactions t
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
  const { customer, service, amount, outletId, customerId, serviceId, quantity } = req.body;

  if (outletId && customerId && serviceId && quantity !== undefined && quantity !== null) {
    try {
      const serviceResult = await pool.query(
        "SELECT name, price FROM services WHERE id::text = $1::text",
        [serviceId]
      );
      const customerResult = await pool.query(
        "SELECT name FROM customers WHERE id::text = $1::text",
        [customerId]
      );
      const quantityNum = Number(quantity);
      const price = Number(serviceResult.rows[0]?.price || 0);
      const totalPrice = Math.round(price * quantityNum);
      const customerName = customerResult.rows[0]?.name ?? String(customerId);
      const serviceName = serviceResult.rows[0]?.name ?? String(serviceId);

      const result = await pool.query(
        `INSERT INTO transactions
         (outlet_id, owner_id, customer_id, service_id, quantity, total_price, customer, service, amount, status)
         VALUES ($1::text, $2::text, $3::uuid, $4::uuid, $5::float, $6::integer, $7::text, $8::text, $9::numeric, 'pending')
         RETURNING *`,
        [outletId, req.userId, customerId, serviceId, quantityNum, totalPrice, customerName, serviceName, totalPrice]
      );

      res.status(201).json({ transaction: result.rows[0] });
      return;
    } catch (err) {
      console.error(err);
      res.status(500).json({ message: "Terjadi kesalahan server" });
      return;
    }
  }

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
    const outletResult = await pool.query(
      "SELECT id, client_id, owner_id, name FROM outlets WHERE client_id::text = $1::text OR id::text = $1::text",
      [outletId]
    );
    if (outletResult.rows.length === 0) {
      res.status(404).json({ message: "Outlet tidak ditemukan" });
      return;
    }
    const outlet = outletResult.rows[0];
    const outletDbId = String(outlet.id);
    const outletClientId = safeInt(outlet.client_id ?? outlet.id);

    const result = await pool.query(
      `INSERT INTO transactions (outlet_id, customer, service, amount, status)
       VALUES ($1, $2, $3, $4, 'diterima')
       RETURNING *`,
      [outletDbId, customer.trim(), service.trim(), amountNum]
    );
    const t = result.rows[0];

    await pool.query(
      "INSERT INTO notifications (owner_id, title, message, type) VALUES ($1, $2, $3, 'transaction')",
      [String(outlet.owner_id), "Transaksi baru", `Transaksi ${t.customer} di ${outlet.name} berhasil dibuat dengan status diterima.`]
    );

    res.status(201).json({
      id: t.id,
      outletId: outletClientId,
      customer: t.customer,
      service: t.service,
      amount: parseFloat(t.amount),
      status: t.status,
      createdAt: t.created_at,
      updatedAt: t.updated_at,
      outletName: outlet.name ?? null,
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

router.put("/:id/status", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { id } = req.params;
  const { status } = req.body;

  try {
    const result = await pool.query(
      "UPDATE transactions SET status = $1::text, updated_at = NOW() WHERE id::text = $2::text RETURNING *",
      [status, id]
    );

    res.json({ transaction: result.rows[0] });
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
         AND (t.outlet_id::text = o.id::text OR t.outlet_id::text = o.client_id::text)
         AND (o.owner_id::text = $3::text OR o.client_id::text = $4::text OR t.outlet_id::text = $4::text)
       RETURNING t.*, o.client_id as outlet_client_id, o.name as outlet_name`,
      [status.toLowerCase(), transactionId, req.userId, req.outletId ?? null]
    );

    if (result.rows.length === 0) {
      res.status(404).json({ message: "Transaksi tidak ditemukan atau akses ditolak" });
      return;
    }

    const t = result.rows[0];
    const ownerIdResult = await pool.query("SELECT owner_id FROM outlets WHERE client_id::text = $1::text OR id::text = $1::text LIMIT 1", [String(t.outlet_client_id ?? t.outlet_id)]);
    if (ownerIdResult.rows[0]?.owner_id) {
      await pool.query(
        "INSERT INTO notifications (owner_id, title, message, type) VALUES ($1, $2, $3, 'transaction')",
        [String(ownerIdResult.rows[0].owner_id), "Status transaksi diperbarui", `Transaksi ${t.customer} sekarang berstatus ${t.status}.`]
      );
    }
    res.json({
      id: t.id,
      outletId: safeInt(t.outlet_client_id ?? t.outlet_id),
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
