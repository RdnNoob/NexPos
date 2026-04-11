import { Router, Request, Response } from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import pool, { initDb } from "../db/client";

const router = Router();

initDb().catch(console.error);

function generateToken(payload: object): string {
  const secret = process.env.JWT_SECRET || "nexpos_secret";
  const expiresIn = process.env.JWT_EXPIRES_IN || "7d";
  return jwt.sign(payload, secret, { expiresIn } as jwt.SignOptions);
}

router.post("/register", async (req: Request, res: Response): Promise<void> => {
  const { email, password, name } = req.body;
  if (!email || !password || !name) {
    res.status(400).json({ message: "Email, password, dan nama wajib diisi" });
    return;
  }

  try {
    const existing = await pool.query("SELECT id FROM users WHERE email = $1", [email]);
    if (existing.rows.length > 0) {
      res.status(409).json({ message: "Email sudah terdaftar" });
      return;
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const result = await pool.query(
      "INSERT INTO users (email, password, name) VALUES ($1, $2, $3) RETURNING id, email, name",
      [email, hashedPassword, name]
    );

    const user = result.rows[0];
    const token = generateToken({ userId: user.id, email: user.email });

    res.status(201).json({
      token,
      user: { id: user.id, email: user.email, name: user.name },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

router.post("/login", async (req: Request, res: Response): Promise<void> => {
  const { email, password } = req.body;
  if (!email || !password) {
    res.status(400).json({ message: "Email dan password wajib diisi" });
    return;
  }

  try {
    const result = await pool.query("SELECT * FROM users WHERE email = $1", [email]);
    if (result.rows.length === 0) {
      res.status(401).json({ message: "Email atau password salah" });
      return;
    }

    const user = result.rows[0];
    const valid = await bcrypt.compare(password, user.password);
    if (!valid) {
      res.status(401).json({ message: "Email atau password salah" });
      return;
    }

    const token = generateToken({ userId: user.id, email: user.email });
    res.json({
      token,
      user: { id: user.id, email: user.email, name: user.name },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

router.post("/login-device", async (req: Request, res: Response): Promise<void> => {
  const { activationCode, deviceName, deviceId } = req.body;
  if (!activationCode || !deviceName || !deviceId) {
    res.status(400).json({ message: "Kode aktivasi, nama device, dan ID device wajib diisi" });
    return;
  }

  try {
    const outletResult = await pool.query(
      "SELECT * FROM outlets WHERE activation_code = $1",
      [activationCode]
    );
    if (outletResult.rows.length === 0) {
      res.status(401).json({ message: "Kode aktivasi tidak valid" });
      return;
    }
    const outlet = outletResult.rows[0];

    const existingDevice = await pool.query(
      "SELECT * FROM devices WHERE device_id = $1",
      [deviceId]
    );

    if (existingDevice.rows.length === 0) {
      const deviceCountResult = await pool.query(
        "SELECT COUNT(*) FROM devices WHERE owner_id = $1",
        [outlet.owner_id]
      );
      const deviceCount = parseInt(deviceCountResult.rows[0].count);
      if (deviceCount >= 5) {
        res.status(403).json({ message: "Batas maksimal 5 device per owner tercapai" });
        return;
      }
    }

    let device;
    if (existingDevice.rows.length > 0) {
      const updated = await pool.query(
        "UPDATE devices SET status = 'online', last_seen = NOW(), device_name = $1 WHERE device_id = $2 RETURNING *",
        [deviceName, deviceId]
      );
      device = updated.rows[0];
    } else {
      const inserted = await pool.query(
        "INSERT INTO devices (owner_id, outlet_id, device_name, device_id, status, last_seen) VALUES ($1, $2, $3, $4, 'online', NOW()) RETURNING *",
        [outlet.owner_id, outlet.id, deviceName, deviceId]
      );
      device = inserted.rows[0];
    }

    const ownerResult = await pool.query("SELECT * FROM users WHERE id = $1", [outlet.owner_id]);
    const owner = ownerResult.rows[0];

    const token = generateToken({ userId: owner.id, email: owner.email, deviceId: device.id, outletId: outlet.id });

    res.json({
      token,
      outlet: {
        id: outlet.id,
        name: outlet.name,
        ownerId: outlet.owner_id,
        activationCode: outlet.activation_code,
      },
      device: {
        id: device.id,
        deviceName: device.device_name,
        deviceId: device.device_id,
        status: device.status,
        outletId: device.outlet_id,
      },
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

export default router;
