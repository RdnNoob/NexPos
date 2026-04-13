import { Router, Request, Response } from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import nodemailer from "nodemailer";
import pool from "../db/client";

const router = Router();

function generateOtp(): string {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

function generateResetToken(email: string): string {
  const secret = process.env.JWT_SECRET || "nexpos_secret";
  return jwt.sign({ email, type: "password_reset" }, secret, { expiresIn: "15m" });
}

function createTransporter() {
  const user = process.env.GMAIL_USER;
  const pass = process.env.GMAIL_APP_PASSWORD;
  if (!user || !pass) {
    throw new Error("Konfigurasi email belum diatur di server");
  }
  return nodemailer.createTransport({
    service: "gmail",
    auth: { user, pass },
  });
}

// POST /api/auth/forgot-password
router.post("/forgot-password", async (req: Request, res: Response): Promise<void> => {
  const { email } = req.body;
  if (!email) {
    res.status(400).json({ message: "Email wajib diisi" });
    return;
  }

  try {
    const userResult = await pool.query("SELECT id, name FROM users WHERE email = $1", [email.trim().toLowerCase()]);
    if (userResult.rows.length === 0) {
      // Jangan beri tahu apakah email terdaftar (keamanan)
      res.json({ message: "Jika email terdaftar, kode OTP telah dikirim" });
      return;
    }

    const user = userResult.rows[0];
    const otp = generateOtp();
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 menit

    // Hapus OTP lama untuk email ini
    await pool.query("DELETE FROM password_reset_otps WHERE email = $1", [email.trim().toLowerCase()]);

    // Simpan OTP baru
    await pool.query(
      "INSERT INTO password_reset_otps (email, otp, expires_at) VALUES ($1, $2, $3)",
      [email.trim().toLowerCase(), otp, expiresAt]
    );

    // Kirim email
    const transporter = createTransporter();
    await transporter.sendMail({
      from: `"NexPos Admin" <${process.env.GMAIL_USER}>`,
      to: email.trim(),
      subject: "Kode OTP Reset Password NexPos",
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 12px;">
          <h2 style="color: #1565C0; margin-bottom: 8px;">Reset Password NexPos</h2>
          <p>Halo, <strong>${user.name}</strong>!</p>
          <p>Kamu meminta reset password untuk akun NexPos Admin kamu.</p>
          <p>Gunakan kode OTP berikut untuk melanjutkan:</p>
          <div style="text-align: center; margin: 24px 0;">
            <span style="font-size: 40px; font-weight: bold; letter-spacing: 12px; color: #1565C0; background: #E3F2FD; padding: 12px 24px; border-radius: 8px;">${otp}</span>
          </div>
          <p style="color: #757575; font-size: 13px;">Kode ini berlaku selama <strong>10 menit</strong>. Jangan bagikan kode ini kepada siapapun.</p>
          <p style="color: #757575; font-size: 13px;">Jika kamu tidak meminta reset password, abaikan email ini.</p>
          <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 16px 0;"/>
          <p style="color: #bdbdbd; font-size: 11px;">NexPos Laundry &copy; 2024</p>
        </div>
      `,
    });

    res.json({ message: "Jika email terdaftar, kode OTP telah dikirim" });
  } catch (err: any) {
    console.error("[ForgotPassword]", err);
    if (err.message?.includes("Konfigurasi email")) {
      res.status(503).json({ message: err.message });
    } else {
      res.status(500).json({ message: "Gagal mengirim email. Coba beberapa saat lagi." });
    }
  }
});

// POST /api/auth/verify-otp
router.post("/verify-otp", async (req: Request, res: Response): Promise<void> => {
  const { email, otp } = req.body;
  if (!email || !otp) {
    res.status(400).json({ message: "Email dan kode OTP wajib diisi" });
    return;
  }

  try {
    const result = await pool.query(
      "SELECT * FROM password_reset_otps WHERE email = $1 AND used = FALSE ORDER BY created_at DESC LIMIT 1",
      [email.trim().toLowerCase()]
    );

    if (result.rows.length === 0) {
      res.status(400).json({ message: "Kode OTP tidak valid atau sudah digunakan" });
      return;
    }

    const otpRecord = result.rows[0];
    if (new Date() > new Date(otpRecord.expires_at)) {
      res.status(400).json({ message: "Kode OTP sudah kadaluarsa. Minta kode baru." });
      return;
    }

    if (otpRecord.otp !== otp.trim()) {
      res.status(400).json({ message: "Kode OTP salah" });
      return;
    }

    // Tandai OTP sebagai terverifikasi (belum dipakai — akan dipakai saat reset)
    const resetToken = generateResetToken(email.trim().toLowerCase());

    res.json({
      message: "OTP berhasil diverifikasi",
      resetToken,
    });
  } catch (err) {
    console.error("[VerifyOTP]", err);
    res.status(500).json({ message: "Terjadi kesalahan server" });
  }
});

// POST /api/auth/reset-password
router.post("/reset-password", async (req: Request, res: Response): Promise<void> => {
  const { resetToken, newPassword } = req.body;
  if (!resetToken || !newPassword) {
    res.status(400).json({ message: "Token reset dan password baru wajib diisi" });
    return;
  }

  if (newPassword.length < 6) {
    res.status(400).json({ message: "Password minimal 6 karakter" });
    return;
  }

  try {
    const secret = process.env.JWT_SECRET || "nexpos_secret";
    const decoded = jwt.verify(resetToken, secret) as { email: string; type: string };
    if (decoded.type !== "password_reset") {
      res.status(400).json({ message: "Token tidak valid" });
      return;
    }

    const email = decoded.email;

    // Hapus semua OTP lama untuk email ini
    await pool.query("DELETE FROM password_reset_otps WHERE email = $1", [email]);

    // Update password
    const hashedPassword = await bcrypt.hash(newPassword, 10);
    const result = await pool.query(
      "UPDATE users SET password = $1 WHERE email = $2 RETURNING id",
      [hashedPassword, email]
    );

    if (result.rows.length === 0) {
      res.status(404).json({ message: "Akun tidak ditemukan" });
      return;
    }

    res.json({ message: "Password berhasil diubah. Silakan login dengan password baru." });
  } catch (err: any) {
    console.error("[ResetPassword]", err);
    if (err.name === "JsonWebTokenError" || err.name === "TokenExpiredError") {
      res.status(400).json({ message: "Token tidak valid atau sudah kadaluarsa. Ulangi proses dari awal." });
    } else {
      res.status(500).json({ message: "Terjadi kesalahan server" });
    }
  }
});

export default router;
