import { Router, Response } from "express";
import pool from "../db/client";
import { authenticateToken, AuthRequest } from "../middleware/auth";

const router = Router();

router.get("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { outletId } = req.query;

  try {
    const result = await pool.query(
      "SELECT * FROM customers WHERE outlet_id::text = $1::text ORDER BY created_at DESC",
      [String(outletId)]
    );

    res.json({ customers: result.rows });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

router.post("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { outletId, name, phone, address } = req.body;

  try {
    const result = await pool.query(
      `INSERT INTO customers (outlet_id, owner_id, name, phone, address)
       VALUES ($1::text, $2::text, $3::text, $4::text, $5::text)
       RETURNING *`,
      [outletId, req.userId, name, phone, address]
    );

    res.status(201).json({ customer: result.rows[0] });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

export default router;