import nodemailer from "nodemailer";

function createTransporter() {
  const user = process.env.GMAIL_USER;
  const pass = process.env.GMAIL_APP_PASSWORD;
  if (!user || !pass) {
    throw new Error("GMAIL_USER atau GMAIL_APP_PASSWORD belum dikonfigurasi");
  }
  return nodemailer.createTransport({
    service: "gmail",
    auth: { user, pass },
  });
}

export async function sendOtpEmail(to: string, otp: string, name: string): Promise<void> {
  const transporter = createTransporter();
  await transporter.sendMail({
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
  });
}
