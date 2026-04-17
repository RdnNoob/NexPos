import { Router, Response } from "express";
import pool from "../db/client";
import { authenticateToken, AuthRequest } from "../middleware/auth";

const router = Router();

// GET /customers - list semua customer
router.get("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { outletId } = req.query;

  try {
    const result = await pool.query(
      "SELECT * FROM customers WHERE outlet_id::text = $1::text ORDER BY name ASC",
      [String(outletId)]
    );
    res.json({ customers: result.rows });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

// GET /customers/search?q=&outletId=
router.get("/search", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { q, outletId } = req.query;

  if (!q) {
    res.status(400).json({ message: "Parameter pencarian wajib diisi" });
    return;
  }

  try {
    const result = await pool.query(
      `SELECT * FROM customers
       WHERE outlet_id::text = $1::text
         AND (name ILIKE $2 OR phone ILIKE $2)
       ORDER BY name ASC`,
      [String(outletId), `%${q}%`]
    );
    res.json({ customers: result.rows });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

// POST /customers - tambah customer
router.post("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { outletId, name, phone, address } = req.body;

  if (!name || !name.trim()) {
    res.status(400).json({ message: "Nama customer wajib diisi" });
    return;
  }

  if (phone && !/^[0-9+\-\s]{7,20}$/.test(phone.trim())) {
    res.status(400).json({ message: "Nomor telepon tidak valid" });
    return;
  }

  try {
    const result = await pool.query(
      `INSERT INTO customers (outlet_id, owner_id, name, phone, address)
       VALUES ($1::text, $2::text, $3::text, $4::text, $5::text)
       RETURNING *`,
      [outletId, req.userId, name.trim(), phone?.trim() ?? "", address?.trim() ?? ""]
    );
    res.status(201).json({ customer: result.rows[0] });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

// PUT /customers/:id - edit customer
router.put("/:id", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { id } = req.params;
  const { name, phone, address } = req.body;

  if (!name || !name.trim()) {
    res.status(400).json({ message: "Nama customer wajib diisi" });
    return;
  }

  if (phone && !/^[0-9+\-\s]{7,20}$/.test(phone.trim())) {
    res.status(400).json({ message: "Nomor telepon tidak valid" });
    return;
  }

  try {
    const result = await pool.query(
      `UPDATE customers SET name = $1, phone = $2, address = $3
       WHERE id::text = $4::text
       RETURNING *`,
      [name.trim(), phone?.trim() ?? "", address?.trim() ?? "", id]
    );

    if (result.rows.length === 0) {
      res.status(404).json({ message: "Customer tidak ditemukan" });
      return;
    }

    res.json({ customer: result.rows[0] });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

// DELETE /customers/:id - hapus customer
router.delete("/:id", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { id } = req.params;

  try {
    const result = await pool.query(
      "DELETE FROM customers WHERE id::text = $1::text RETURNING id",
      [id]
    );

    if (result.rows.length === 0) {
      res.status(404).json({ message: "Customer tidak ditemukan" });
      return;
    }

    res.json({ message: "Customer berhasil dihapus" });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

export default router;
