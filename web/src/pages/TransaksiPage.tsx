import { useState, useEffect, useCallback } from "react";
import { useAuth } from "../lib/auth-context";
import { api, Transaction, STATUS_LABELS, STATUS_COLORS, formatRupiah } from "../lib/api";

const STATUSES = ["pending", "process", "done", "picked"];

export default function TransaksiPage() {
  const { outletId } = useAuth();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState("all");

  const load = useCallback(async () => {
    if (!outletId) return;
    setLoading(true);
    const res = await api.getTransactions(outletId).catch(() => ({ transactions: [] }));
    setTransactions(res.transactions);
    setLoading(false);
  }, [outletId]);

  useEffect(() => { load(); }, [load]);

  async function handleStatus(id: string | number, status: string) {
    await api.updateStatus(id, status).catch(console.error);
    setTransactions(prev => prev.map(t => t.id === id ? { ...t, status } : t));
  }

  const filtered = filterStatus === "all" ? transactions : transactions.filter(t => t.status === filterStatus);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Transaksi</h1>
          <p className="text-muted-foreground text-sm mt-1">{transactions.length} total transaksi</p>
        </div>
        <button onClick={load} className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition px-3 py-2 rounded-lg border border-border hover:bg-secondary">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          Refresh
        </button>
      </div>

      {/* Filter Status */}
      <div className="flex gap-2 flex-wrap mb-4">
        <button onClick={() => setFilterStatus("all")} className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition ${filterStatus === "all" ? "bg-primary text-primary-foreground border-primary" : "border-border text-foreground hover:bg-secondary"}`}>
          Semua ({transactions.length})
        </button>
        {STATUSES.map(s => (
          <button key={s} onClick={() => setFilterStatus(s)} className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition ${filterStatus === s ? "bg-primary text-primary-foreground border-primary" : "border-border text-foreground hover:bg-secondary"}`}>
            {STATUS_LABELS[s] ?? s} ({transactions.filter(t => t.status === s).length})
          </button>
        ))}
      </div>

      {loading ? (
        <div className="space-y-3">{[...Array(4)].map((_, i) => <div key={i} className="bg-card rounded-xl p-4 border border-border animate-pulse h-20" />)}</div>
      ) : filtered.length === 0 ? (
        <div className="bg-card rounded-xl p-10 border border-border text-center">
          <p className="text-muted-foreground text-sm">Belum ada transaksi</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map(t => (
            <div key={t.id} className="bg-card rounded-xl border border-border shadow-sm p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <p className="font-semibold text-foreground">{t.customerName ?? "—"}</p>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLORS[t.status] ?? "bg-gray-100 text-gray-700"}`}>
                      {STATUS_LABELS[t.status] ?? t.status}
                    </span>
                  </div>
                  <p className="text-sm text-muted-foreground mt-0.5">{t.serviceName ?? "—"} — {t.quantity} {t.serviceUnit ?? ""}</p>
                  <p className="text-primary font-bold mt-1">{formatRupiah(t.totalAmount)}</p>
                  <p className="text-xs text-muted-foreground mt-1">{new Date(t.createdAt).toLocaleString("id-ID")}</p>
                </div>
                <div className="shrink-0">
                  <select
                    value={t.status}
                    onChange={e => handleStatus(t.id, e.target.value)}
                    className="text-xs px-2 py-1.5 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  >
                    {STATUSES.map(s => (
                      <option key={s} value={s}>{STATUS_LABELS[s] ?? s}</option>
                    ))}
                  </select>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
