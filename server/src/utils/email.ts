import nodemailer from "nodemailer";

function createTransporter() {
  const user = process.env.GMAIL_USER;
  const pass = process.env.GMAIL_APP_PASSWORD;
  if (!user || !pass) {
    return null;
  }
  // Gunakan host eksplisit port 587 (STARTTLS) karena Railway sering blokir port 465
  return nodemailer.createTransport({
    host: "smtp.gmail.com",
    port: 587,
    secure: false, // STARTTLS (bukan SSL langsung)
    auth: { user, pass },
    tls: { rejectUnauthorized: false },
    connectionTimeout: 15_000,
    greetingTimeout: 15_000,
    socketTimeout: 20_000,
    logger: process.env.NODE_ENV !== "production",
    debug: process.env.NODE_ENV !== "production",
  });
}

export async function sendOtpEmail(to: string, otp: string, name: string): Promise<void> {
  const transporter = createTransporter();

  if (!transporter) {
    console.warn("[Email] GMAIL_USER atau GMAIL_APP_PASSWORD belum dikonfigurasi.");
    return;
  }

  const timeoutPromise = new Promise<never>((_, reject) =>
    setTimeout(() => reject(new Error("Email timeout setelah 25 detik")), 25_000)
  );

  const info = await Promise.race([
    transporter.sendMail({
      from: `"NexPos Admin" <${process.env.GMAIL_USER}>`,
      to,
      subject: "Kode OTP Reset Password NexPos",
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px; border: 1px solid #e0e0e0; border-radius: 12px;">
          <h2 style="color: #1565C0; margin-bottom: 8px;">NexPos Admin</h2>
          <p style="color: #555; margin-bottom: 24px;">Halo <strong>${name}</strong>,</p>
          <p style="color: #333;">Anda meminta reset password untuk akun NexPos Anda. Gunakan kode OTP berikut:</p>
          <div style="background: #E3F2FD; border-radius: 8px; padding: 20px; text-align: center; margin: 24px 0;">
            <span style="font-size: 36px; font-weight: bold; letter-spacing: 12px; color: #1565C0;">${otp}</span>
          </div>
          <p style="color: #555; font-size: 14px;">Kode ini berlaku selama <strong>10 menit</strong>. Jangan bagikan kode ini kepada siapapun.</p>
          <p style="color: #555; font-size: 14px;">Jika Anda tidak meminta reset password, abaikan email ini.</p>
          <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;" />
          <p style="color: #aaa; font-size: 12px;">NexPos &mdash; Manajemen Outlet Laundry Digital</p>
        </div>
      `,
    }),
    timeoutPromise,
  ]);

  console.info("[Email] OTP terkirim:", {
    to,
    messageId: info.messageId,
    accepted: info.accepted,
    rejected: info.rejected,
    response: info.response,
  });
}
