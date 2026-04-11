import { Router, Response } from "express";
import { nanoid } from "../utils/nanoid";
import pool, { ensureRuntimeSchema } from "../db/client";
import { authenticateToken, AuthRequest } from "../middleware/auth";

const router = Router();

function toClientId(value: unknown): unknown {
  const numberValue = Number(value);
  return Number.isSafeInteger(numberValue) && String(value) === String(numberValue)
    ? numberValue
    : value;
}

router.get("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    await ensureRuntimeSchema();
    const result = await pool.query(
      "SELECT id, owner_id, name, activation_code, created_at FROM outlets WHERE owner_id::text = $1::text ORDER BY created_at DESC",
      [req.userId]
    );
    res.json({ outlets: result.rows.map(r => ({
      id: r.id,
      ownerId: toClientId(r.owner_id),
      name: r.name,
      activationCode: r.activation_code,
      createdAt: r.created_at,
    })) });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

router.post("/", authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  const { name } = req.body;
  if (!name) {
    res.status(400).json({ message: "Nama outlet wajib diisi" });
    return;
  }

  try {
    await ensureRuntimeSchema();
    const countResult = await pool.query(
      "SELECT COUNT(*) FROM outlets WHERE owner_id::text = $1::text",
      [req.userId]
    );
    const count = parseInt(countResult.rows[0].count);
    if (count >= 5) {
      res.status(403).json({ message: "Batas maksimal 5 outlet per owner tercapai" });
      return;
    }

    const activationCode = nanoid(8).toUpperCase();
    const result = await pool.query(
      "INSERT INTO outlets (owner_id, name, activation_code) VALUES ($1, $2, $3) RETURNING *",
      [String(req.userId), name, activationCode]
    );
    const outlet = result.rows[0];

    res.status(201).json({
      outlet: {
        id: outlet.id,
        ownerId: toClientId(outlet.owner_id),
        name: outlet.name,
        activationCode: outlet.activation_code,
        createdAt: outlet.created_at,
      },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

export default router;
