import { useState, useEffect } from "react";
import { useAuth } from "../lib/auth-context";
import { api, ReportSummary, formatRupiah } from "../lib/api";
import { Link } from "wouter";

export default function DashboardPage() {
  const { outletId, user } = useAuth();
  const [summary, setSummary] = useState<ReportSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!outletId) return;
    api.getReport(outletId)
      .then(setSummary)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [outletId]);

  const cards = summary
    ? [
        { label: "Total Transaksi", value: summary.total_transactions, color: "bg-blue-500", icon: "M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" },
        { label: "Total Pendapatan", value: formatRupiah(summary.total_income), color: "bg-emerald-500", icon: "M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" },
        { label: "Menunggu", value: summary.total_pending, color: "bg-yellow-500", icon: "M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" },
        { label: "Diproses", value: summary.total_process, color: "bg-orange-500", icon: "M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" },
        { label: "Selesai", value: summary.total_done, color: "bg-green-500", icon: "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" },
        { label: "Sudah Diambil", value: summary.total_picked, color: "bg-purple-500", icon: "M5 13l4 4L19 7" },
      ]
    : [];

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-foreground">Selamat datang, {user?.name ?? "Admin"} 👋</h1>
        <p className="text-muted-foreground text-sm mt-1">Ringkasan Outlet #{outletId}</p>
      </div>

      {loading ? (
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="bg-card rounded-xl p-4 border border-border animate-pulse h-24" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
          {cards.map((c) => (
            <div key={c.label} className="bg-card rounded-xl p-4 border border-border shadow-sm">
              <div className="flex items-center gap-3">
                <div className={`w-9 h-9 ${c.color} rounded-lg flex items-center justify-center shrink-0`}>
                  <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={c.icon} />
                  </svg>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">{c.label}</p>
                  <p className="text-lg font-bold text-foreground">{c.value}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="mt-8 grid grid-cols-1 md:grid-cols-2 gap-4">
        <Link href="/kasir">
          <a className="block bg-primary text-primary-foreground rounded-xl p-5 shadow-sm hover:opacity-90 transition">
            <div className="flex items-center gap-3">
              <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 4v16m8-8H4" />
              </svg>
              <div>
                <p className="font-bold text-lg">Buat Transaksi Baru</p>
                <p className="text-primary-foreground/80 text-sm">Kasir — pilih customer & layanan</p>
              </div>
            </div>
          </a>
        </Link>
        <Link href="/transaksi">
          <a className="block bg-card border border-border rounded-xl p-5 shadow-sm hover:bg-secondary transition">
            <div className="flex items-center gap-3">
              <svg className="w-8 h-8 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
              <div>
                <p className="font-bold text-lg text-foreground">Lihat Transaksi</p>
                <p className="text-muted-foreground text-sm">Kelola status pesanan</p>
              </div>
            </div>
          </a>
        </Link>
      </div>
    </div>
  );
}
