import { useState, useEffect } from "react";
import { useAuth } from "../lib/auth-context";
import { api, ReportSummary, formatRupiah } from "../lib/api";

export default function LaporanPage() {
  const { outletId } = useAuth();
  const [summary, setSummary] = useState<ReportSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!outletId) return;
    api.getReport(outletId)
      .then(setSummary)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [outletId]);

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-foreground">Laporan</h1>
        <p className="text-muted-foreground text-sm mt-1">Ringkasan performa outlet</p>
      </div>

      {loading ? (
        <div className="space-y-4">{[...Array(3)].map((_, i) => <div key={i} className="bg-card rounded-xl p-4 border border-border animate-pulse h-20" />)}</div>
      ) : summary ? (
        <div className="space-y-4">
          {/* Income Card */}
          <div className="bg-primary rounded-2xl p-6 text-primary-foreground">
            <p className="text-primary-foreground/80 text-sm">Total Pendapatan</p>
            <p className="text-4xl font-bold mt-1">{formatRupiah(summary.total_income)}</p>
            <p className="text-primary-foreground/70 text-sm mt-2">dari {summary.total_transactions} transaksi</p>
          </div>

          {/* Status Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[
              { label: "Menunggu", value: summary.total_pending, color: "text-yellow-600 bg-yellow-50 border-yellow-200" },
              { label: "Diproses", value: summary.total_process, color: "text-blue-600 bg-blue-50 border-blue-200" },
              { label: "Selesai", value: summary.total_done, color: "text-green-600 bg-green-50 border-green-200" },
              { label: "Diambil", value: summary.total_picked, color: "text-purple-600 bg-purple-50 border-purple-200" },
            ].map(item => (
              <div key={item.label} className={`rounded-xl p-4 border ${item.color}`}>
                <p className="text-2xl font-bold">{item.value}</p>
                <p className="text-xs font-medium mt-1 opacity-80">{item.label}</p>
              </div>
            ))}
          </div>

          {/* Progress bars */}
          <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
            <h3 className="text-sm font-semibold text-foreground mb-4">Distribusi Status</h3>
            <div className="space-y-3">
              {[
                { label: "Menunggu", value: summary.total_pending, color: "bg-yellow-400" },
                { label: "Diproses", value: summary.total_process, color: "bg-blue-400" },
                { label: "Selesai", value: summary.total_done, color: "bg-green-400" },
                { label: "Diambil", value: summary.total_picked, color: "bg-purple-400" },
              ].map(item => {
                const pct = summary.total_transactions > 0 ? (item.value / summary.total_transactions) * 100 : 0;
                return (
                  <div key={item.label}>
                    <div className="flex items-center justify-between text-xs mb-1">
                      <span className="text-muted-foreground">{item.label}</span>
                      <span className="text-foreground font-medium">{item.value} ({pct.toFixed(0)}%)</span>
                    </div>
                    <div className="h-2 bg-secondary rounded-full overflow-hidden">
                      <div className={`h-full ${item.color} rounded-full transition-all`} style={{ width: `${pct}%` }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-card rounded-xl p-10 border border-border text-center">
          <p className="text-muted-foreground text-sm">Gagal memuat laporan</p>
        </div>
      )}
    </div>
  );
}
