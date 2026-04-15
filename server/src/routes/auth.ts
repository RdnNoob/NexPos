import { Router, Request, Response } from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import crypto from "crypto";
import pool from "../db/client";
import { sendOtpEmail } from "../utils/email";

const router = Router();

function generateToken(payload: object): string {
  const secret = process.env.JWT_SECRET || "nexpos_secret";
  const expiresIn = process.env.JWT_EXPIRES_IN || "7d";
  return jwt.sign(payload, secret, { expiresIn } as jwt.SignOptions);
}

function toClientId(value: unknown): unknown {
  const numberValue = Number(value);
  return Number.isSafeInteger(numberValue) && String(value) === String(numberValue)
    ? numberValue
    : value;
}

function toRequiredClientId(value: unknown): number {
  const numberValue = Number(value);
  return Number.isSafeInteger(numberValue) ? numberValue : 0;
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

  const step = { current: "init" };
  try {
    const normalizedActivationCode = String(activationCode).trim().toUpperCase();
    const normalizedDeviceId = String(deviceId).trim();
    const normalizedDeviceName = String(deviceName).trim();

    step.current = "query-outlet";
    const outletResult = await pool.query(
      "SELECT * FROM outlets WHERE UPPER(TRIM(activation_code)) = $1",
      [normalizedActivationCode]
    );
    if (outletResult.rows.length === 0) {
      res.status(401).json({ message: "Kode aktivasi tidak valid" });
      return;
    }
    const outlet = outletResult.rows[0];
    const outletDbId = String(outlet.id);
    const outletClientId = toRequiredClientId(outlet.client_id ?? outlet.id);
    const outletOwnerId = String(outlet.owner_id);

    step.current = "query-existing-device";
    const existingDevice = await pool.query(
      "SELECT * FROM devices WHERE device_id = $1",
      [normalizedDeviceId]
    );

    if (existingDevice.rows.length === 0) {
      step.current = "count-devices";
      const deviceCountResult = await pool.query(
        "SELECT COUNT(*) FROM devices WHERE outlet_id::text = $1::text OR outlet_id::text = $2::text",
        [outletDbId, String(outletClientId)]
      );
      const deviceCount = parseInt(deviceCountResult.rows[0].count);
      if (deviceCount >= 5) {
        res.status(403).json({ message: "Batas maksimal 5 device per owner tercapai" });
        return;
      }
    }

    let device;
    if (existingDevice.rows.length > 0) {
      step.current = "update-device";
      const updated = await pool.query(
        "UPDATE devices SET status = 'online', last_seen = NOW(), device_name = $1, outlet_id = $2, owner_id = $3 WHERE device_id = $4 RETURNING *",
        [normalizedDeviceName, outletDbId, outletOwnerId, normalizedDeviceId]
      );
      device = updated.rows[0];
      if (!device) {
        step.current = "update-device-fallback";
        const fallback = await pool.query("SELECT * FROM devices WHERE device_id = $1", [normalizedDeviceId]);
        device = fallback.rows[0];
      }
    } else {
      step.current = "insert-device";
      const inserted = await pool.query(
        "INSERT INTO devices (owner_id, outlet_id, device_name, device_id, status, last_seen) VALUES ($1, $2, $3, $4, 'online', NOW()) RETURNING *",
        [outletOwnerId, outletDbId, normalizedDeviceName, normalizedDeviceId]
      );
      device = inserted.rows[0];
    }

    if (!device) {
      console.error("[login-device] device undefined setelah step:", step.current);
      res.status(500).json({ message: "Terjadi kesalahan server, coba lagi nanti" });
      return;
    }

    step.current = "query-owner";
    const ownerResult = await pool.query("SELECT * FROM users WHERE id::text = $1::text", [outletOwnerId]);
    const owner = ownerResult.rows[0];

    step.current = "generate-token";
    const tokenPayload = {
      userId: String(owner?.id ?? outletOwnerId),
      email: owner?.email ?? "",
      deviceId: device.id,
      outletId: outletClientId,
    };

    const token = generateToken(tokenPayload);

    res.json({
      token,
      outlet: {
        id: outletClientId,
        name: outlet.name,
        ownerId: outletOwnerId,
        activationCode: outlet.activation_code,
      },
      device: {
        id: device.id,
        deviceName: device.device_name,
        deviceId: device.device_id,
        status: device.status,
        outletId: outletClientId,
      },
    });
  } catch (err: any) {
    console.error(`[login-device:${step.current}] ERROR:`, err?.message ?? err);
    res.status(500).json({ message: "Terjadi kesalahan server, coba lagi nanti" });
  }
});

router.delete("/account", async (req: Request, res: Response): Promise<void> => {
  const authHeader = req.headers["authorization"];
  const token = authHeader && authHeader.split(" ")[1];

  if (!token) {
    res.status(401).json({ message: "Token tidak ditemukan" });
    return;
  }

  try {
    const secret = process.env.JWT_SECRET || "nexpos_secret";
    const decoded = jwt.verify(token, secret) as { userId: string | number; email: string };
    const userId = decoded.userId;

    const client = await pool.connect();
    try {
      await client.query("BEGIN");
      await client.query("DELETE FROM transactions WHERE outlet_id IN (SELECT id FROM outlets WHERE owner_id::text = $1::text)", [userId]);
      await client.query("DELETE FROM devices WHERE owner_id::text = $1::text", [userId]);
      await client.query("DELETE FROM outlets WHERE owner_id::text = $1::text", [userId]);
      await client.query("DELETE FROM users WHERE id::text = $1::text", [userId]);
      await client.query("COMMIT");
    } catch (err) {
      await client.query("ROLLBACK");
      throw err;
    } finally {
      client.release();
    }

    res.json({ message: "Akun berhasil dihapus" });
  } catch (err: any) {
    if (err.name === "JsonWebTokenError" || err.name === "TokenExpiredError") {
      res.status(401).json({ message: "Token tidak valid atau sudah kadaluarsa" });
      return;
    }
    console.error(err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

// ── Forgot Password: Step 1 – kirim OTP ──────────────────────────────────────
router.post("/forgot-password", async (req: Request, res: Response): Promise<void> => {
  const { email } = req.body;
  if (!email) {
    res.status(400).json({ message: "Email wajib diisi" });
    return;
  }

  try {
    const result = await pool.query("SELECT id, name, email FROM users WHERE email = $1", [email.trim().toLowerCase()]);
    if (result.rows.length === 0) {
      // Jangan bocorkan info "email tidak terdaftar" — respons sama seperti sukses
      res.json({ message: "Jika email terdaftar, kode OTP telah dikirim" });
      return;
    }

    const user = result.rows[0];
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 menit

    await pool.query(
      "UPDATE users SET otp_code = $1, otp_expires_at = $2, reset_token = NULL, reset_token_expires_at = NULL WHERE id = $3",
      [otp, expiresAt, user.id]
    );

    // Selalu log OTP ke console (Railway logs) agar bisa dicek jika email tidak masuk
    console.info("[forgot-password] ========================================");
    console.info(`[forgot-password] OTP untuk: ${user.email} (${user.name})`);
    console.info(`[forgot-password] Kode OTP: ${otp}`);
    console.info(`[forgot-password] Berlaku sampai: ${expiresAt.toISOString()}`);
    console.info("[forgot-password] ========================================");

    // Kirim email di background — jangan block response ke client
    sendOtpEmail(user.email, otp, user.name).catch((emailErr: any) => {
      console.error("[forgot-password] Gagal kirim email:", emailErr?.message || emailErr);
      console.error("[forgot-password] GMAIL_USER set?", !!process.env.GMAIL_USER);
      console.error("[forgot-password] GMAIL_APP_PASSWORD set?", !!process.env.GMAIL_APP_PASSWORD);
    });

    res.json({ message: "Jika email terdaftar, kode OTP telah dikirim" });
  } catch (err: any) {
    console.error("[forgot-password]", err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

// ── Forgot Password: Step 2 – verifikasi OTP ─────────────────────────────────
router.post("/verify-otp", async (req: Request, res: Response): Promise<void> => {
  const { email, otp } = req.body;
  if (!email || !otp) {
    res.status(400).json({ message: "Email dan kode OTP wajib diisi" });
    return;
  }

  try {
    const result = await pool.query(
      "SELECT id, otp_code, otp_expires_at FROM users WHERE email = $1",
      [email.trim().toLowerCase()]
    );

    if (result.rows.length === 0) {
      res.status(400).json({ message: "Kode OTP tidak valid atau sudah kadaluarsa" });
      return;
    }

    const user = result.rows[0];

    if (!user.otp_code || user.otp_code !== otp.trim()) {
      res.status(400).json({ message: "Kode OTP salah" });
      return;
    }

    if (!user.otp_expires_at || new Date(user.otp_expires_at) < new Date()) {
      res.status(400).json({ message: "Kode OTP sudah kadaluarsa. Minta kode baru." });
      return;
    }

    const resetToken = crypto.randomBytes(48).toString("hex");
    const resetExpiresAt = new Date(Date.now() + 15 * 60 * 1000); // 15 menit

    await pool.query(
      "UPDATE users SET otp_code = NULL, otp_expires_at = NULL, reset_token = $1, reset_token_expires_at = $2 WHERE id = $3",
      [resetToken, resetExpiresAt, user.id]
    );

    res.json({ resetToken });
  } catch (err) {
    console.error("[verify-otp]", err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

// ── Forgot Password: Step 3 – reset password ─────────────────────────────────
router.post("/reset-password", async (req: Request, res: Response): Promise<void> => {
  const { email, resetToken, newPassword } = req.body;
  if (!email || !resetToken || !newPassword) {
    res.status(400).json({ message: "Email, token reset, dan password baru wajib diisi" });
    return;
  }

  if (newPassword.length < 6) {
    res.status(400).json({ message: "Password baru minimal 6 karakter" });
    return;
  }

  try {
    const result = await pool.query(
      "SELECT id, reset_token, reset_token_expires_at FROM users WHERE email = $1",
      [email.trim().toLowerCase()]
    );

    if (result.rows.length === 0) {
      res.status(400).json({ message: "Permintaan reset password tidak valid" });
      return;
    }

    const user = result.rows[0];

    if (!user.reset_token || user.reset_token !== resetToken) {
      res.status(400).json({ message: "Token reset tidak valid" });
      return;
    }

    if (!user.reset_token_expires_at || new Date(user.reset_token_expires_at) < new Date()) {
      res.status(400).json({ message: "Token reset sudah kadaluarsa. Ulangi proses dari awal." });
      return;
    }

    const hashedPassword = await bcrypt.hash(newPassword, 10);
    await pool.query(
      "UPDATE users SET password = $1, reset_token = NULL, reset_token_expires_at = NULL WHERE id = $2",
      [hashedPassword, user.id]
    );

    res.json({ message: "Password berhasil diperbarui. Silakan login dengan password baru." });
  } catch (err) {
    console.error("[reset-password]", err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

export default router;
