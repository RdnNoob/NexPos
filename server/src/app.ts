import express from "express";
import cors from "cors";
import authRouter from "./routes/auth";
import outletsRouter from "./routes/outlets";
import devicesRouter from "./routes/devices";
import transactionsRouter from "./routes/transactions";
import passwordResetRouter from "./routes/password-reset";

const app = express();

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.get("/api/healthz", (_req, res) => {
  res.json({ status: "ok", timestamp: new Date().toISOString() });
});

app.use("/api/auth", authRouter);
app.use("/api/auth", passwordResetRouter);
app.use("/api/outlets", outletsRouter);
app.use("/api/devices", devicesRouter);
app.use("/api/transactions", transactionsRouter);

app.use((_req, res) => {
  res.status(404).json({ message: "Endpoint tidak ditemukan" });
});

export default app;
