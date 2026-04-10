import { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";

export interface AuthRequest extends Request {
  userId?: number;
  userEmail?: string;
}

export function authenticateToken(
  req: AuthRequest,
  res: Response,
  next: NextFunction
): void {
  const authHeader = req.headers["authorization"];
  const token = authHeader && authHeader.split(" ")[1];

  if (!token) {
    res.status(401).json({ message: "Token tidak ditemukan" });
    return;
  }

  try {
    const secret = process.env.JWT_SECRET || "nexpos_secret";
    const decoded = jwt.verify(token, secret) as { userId: number; email: string };
    req.userId = decoded.userId;
    req.userEmail = decoded.email;
    next();
  } catch {
    res.status(403).json({ message: "Token tidak valid atau sudah kadaluarsa" });
  }
}
