import { useState, useEffect } from "react";
import { useAuth } from "../lib/auth-context";
import { api, Customer, Service, formatRupiah } from "../lib/api";

export default function KasirPage() {
  const { outletId } = useAuth();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);

  const [selectedCustomer, setSelectedCustomer] = useState("");
  const [selectedService, setSelectedService] = useState("");
  const [quantity, setQuantity] = useState<number | "">("");
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState("");
  const [searchCustomer, setSearchCustomer] = useState("");

  useEffect(() => {
    if (!outletId) return;
    Promise.all([api.getCustomers(outletId), api.getServices(outletId)])
      .then(([c, s]) => {
        setCustomers(c.customers);
        setServices(s.services);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [outletId]);

  const selectedSvc = services.find(s => s.id === selectedService);
  const hargaSatuan = selectedSvc?.price ?? 0;
  const unit = selectedSvc?.unit ?? "kg";
  const total = hargaSatuan * (Number(quantity) || 0);

  const filteredCustomers = searchCustomer.trim()
    ? customers.filter(c =>
        c.name.toLowerCase().includes(searchCustomer.toLowerCase()) ||
        c.phone.includes(searchCustomer)
      )
    : customers;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    if (!selectedCustomer) { setError("Pilih customer terlebih dahulu"); return; }
    if (!selectedService) { setError("Pilih layanan terlebih dahulu"); return; }
    if (!quantity || Number(quantity) <= 0) { setError("Quantity harus lebih dari 0"); return; }
    if (!outletId) return;

    setSaving(true);
    try {
      await api.createTransaction({
        outletId,
        customerId: selectedCustomer,
        serviceId: selectedService,
        quantity: Number(quantity),
      });
      setSuccess(true);
      setSelectedCustomer("");
      setSelectedService("");
      setQuantity("");
      setSearchCustomer("");
      setTimeout(() => setSuccess(false), 3000);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Gagal menyimpan transaksi");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="max-w-lg mx-auto space-y-4">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="bg-card rounded-xl p-4 border border-border animate-pulse h-16" />
        ))}
      </div>
    );
  }

  return (
    <div className="max-w-lg mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-foreground">Kasir</h1>
        <p className="text-muted-foreground text-sm mt-1">Buat transaksi laundry baru</p>
      </div>

      {success && (
        <div className="mb-4 bg-green-50 border border-green-200 text-green-800 px-4 py-3 rounded-xl text-sm font-medium flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
          Transaksi berhasil disimpan!
        </div>
      )}

      <form onSubmit={handleSubmit} className="bg-card rounded-2xl border border-border shadow-sm p-6 space-y-5">

        {/* Step 1: Pilih Customer */}
        <div>
          <label className="block text-sm font-semibold text-foreground mb-2">
            <span className="inline-flex items-center justify-center w-5 h-5 bg-primary text-primary-foreground rounded-full text-xs mr-2">1</span>
            Pilih Customer
          </label>
          <input
            type="text"
            placeholder="Cari nama atau nomor HP..."
            value={searchCustomer}
            onChange={e => { setSearchCustomer(e.target.value); setSelectedCustomer(""); }}
            className="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm mb-2 focus:outline-none focus:ring-2 focus:ring-primary"
          />
          <select
            value={selectedCustomer}
            onChange={e => setSelectedCustomer(e.target.value)}
            className="w-full px-3 py-2 rounded-lg border border-input bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            required
          >
            <option value="">-- Pilih Customer --</option>
            {filteredCustomers.map(c => (
              <option key={c.id} value={c.id}>{c.name} {c.phone ? `(${c.phone})` : ""}</option>
            ))}
          </select>
          {customers.length === 0 && (
            <p className="text-xs text-muted-foreground mt-1">Belum ada customer. Tambah di menu Customer.</p>
          )}
        </div>

        {/* Step 2: Pilih Layanan */}
        <div>
          <label className="block text-sm font-semibold text-foreground mb-2">
            <span className="inline-flex items-center justify-center w-5 h-5 bg-primary text-primary-foreground rounded-full text-xs mr-2">2</span>
            Pilih Layanan
          </label>
          <select
            value={selectedService}
            onChange={e => { setSelectedService(e.target.value); setQuantity(""); }}
            className="w-full px-3 py-2 rounded-lg border border-input bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            required
          >
            <option value="">-- Pilih Layanan --</option>
            {services.map(s => (
              <option key={s.id} value={s.id}>{s.name} — {formatRupiah(s.price)}/{s.unit}</option>
            ))}
          </select>

          {/* Harga otomatis muncul setelah pilih layanan */}
          {selectedSvc && (
            <div className="mt-2 flex items-center gap-2 bg-blue-50 border border-blue-200 rounded-lg px-3 py-2">
              <svg className="w-4 h-4 text-blue-500 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span className="text-sm text-blue-700">
                Harga: <strong>{formatRupiah(hargaSatuan)}</strong> per {unit}
              </span>
            </div>
          )}

          {services.length === 0 && (
            <p className="text-xs text-muted-foreground mt-1">Belum ada layanan. Tambah di menu Layanan.</p>
          )}
        </div>

        {/* Step 3: Input Quantity */}
        <div>
          <label className="block text-sm font-semibold text-foreground mb-2">
            <span className="inline-flex items-center justify-center w-5 h-5 bg-primary text-primary-foreground rounded-full text-xs mr-2">3</span>
            Input Quantity {selectedSvc ? `(${unit})` : ""}
          </label>
          <input
            type="number"
            step="0.1"
            min="0.1"
            value={quantity}
            onChange={e => setQuantity(e.target.value === "" ? "" : Number(e.target.value))}
            placeholder={`Contoh: 1 (${unit})`}
            disabled={!selectedService}
            className="w-full px-3 py-2 rounded-lg border border-input bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary disabled:opacity-50"
          />
        </div>

        {/* Step 4: Total Otomatis */}
        <div className="bg-secondary rounded-xl p-4 border border-border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs text-muted-foreground">Total Pembayaran</p>
              <p className="text-2xl font-bold text-foreground mt-0.5">
                {total > 0 ? formatRupiah(total) : "—"}
              </p>
              {quantity && selectedSvc && (
                <p className="text-xs text-muted-foreground mt-1">
                  {formatRupiah(hargaSatuan)} × {quantity} {unit}
                </p>
              )}
            </div>
            <div className="w-12 h-12 bg-primary/10 rounded-xl flex items-center justify-center">
              <svg className="w-6 h-6 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
            </div>
          </div>
        </div>

        {error && (
          <div className="bg-destructive/10 text-destructive text-sm px-3 py-2 rounded-lg">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={saving || !selectedCustomer || !selectedService || !quantity}
          className="w-full bg-primary text-primary-foreground font-semibold py-3 rounded-xl text-sm transition hover:opacity-90 disabled:opacity-50 flex items-center justify-center gap-2"
        >
          {saving ? (
            <>
              <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              Menyimpan...
            </>
          ) : (
            <>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              Simpan Transaksi
            </>
          )}
        </button>
      </form>
    </div>
  );
}
