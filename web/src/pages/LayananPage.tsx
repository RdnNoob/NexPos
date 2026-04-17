import { useState, useEffect } from "react";
import { useAuth } from "../lib/auth-context";
import { api, Service, formatRupiah } from "../lib/api";

interface Form { name: string; price: string; unit: string; }
const EMPTY: Form = { name: "", price: "", unit: "kg" };

export default function LayananPage() {
  const { outletId } = useAuth();
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState<Form>(EMPTY);
  const [editing, setEditing] = useState<Service | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function load() {
    if (!outletId) return;
    setLoading(true);
    const res = await api.getServices(outletId).catch(() => ({ services: [] }));
    setServices(res.services);
    setLoading(false);
  }

  useEffect(() => { load(); }, [outletId]);

  function openAdd() { setEditing(null); setForm(EMPTY); setError(""); setShowForm(true); }
  function openEdit(s: Service) {
    setEditing(s);
    setForm({ name: s.name, price: String(s.price), unit: s.unit });
    setError("");
    setShowForm(true);
  }
  function closeForm() { setShowForm(false); setEditing(null); setForm(EMPTY); }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    const priceNum = parseFloat(form.price);
    if (!form.name.trim()) { setError("Nama layanan wajib diisi"); return; }
    if (isNaN(priceNum) || priceNum <= 0) { setError("Harga harus lebih dari 0"); return; }
    if (!form.unit.trim()) { setError("Satuan wajib diisi"); return; }
    if (!outletId) return;
    setSaving(true);
    try {
      if (editing) {
        const res = await api.updateService(editing.id, { name: form.name, price: priceNum, unit: form.unit });
        setServices(prev => prev.map(s => s.id === editing.id ? res.service : s));
      } else {
        const res = await api.createService({ outletId, name: form.name, price: priceNum, unit: form.unit });
        setServices(prev => [res.service, ...prev]);
      }
      closeForm();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Gagal menyimpan");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string) {
    if (!confirm("Hapus layanan ini?")) return;
    await api.deleteService(id).catch(console.error);
    setServices(prev => prev.filter(s => s.id !== id));
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Layanan</h1>
          <p className="text-muted-foreground text-sm mt-1">Kelola daftar layanan & harga (bisa diubah kapan saja)</p>
        </div>
        <button onClick={openAdd} className="flex items-center gap-2 bg-primary text-primary-foreground px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90 transition">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Tambah Layanan
        </button>
      </div>

      {/* Form Modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <div className="bg-card rounded-2xl border border-border shadow-xl w-full max-w-sm p-6">
            <h2 className="text-lg font-bold text-foreground mb-4">{editing ? "Edit Layanan" : "Tambah Layanan"}</h2>
            <form onSubmit={handleSave} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-foreground mb-1">Nama Layanan</label>
                <input
                  value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="Contoh: Cuci + Setrika"
                  className="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-foreground mb-1">Harga (Rp)</label>
                <input
                  type="number"
                  min="1"
                  value={form.price}
                  onChange={e => setForm(f => ({ ...f, price: e.target.value }))}
                  placeholder="Contoh: 6000"
                  className="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                  required
                />
                {form.price && parseFloat(form.price) > 0 && (
                  <p className="text-xs text-muted-foreground mt-1">{formatRupiah(parseFloat(form.price))} per {form.unit || "satuan"}</p>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-foreground mb-1">Satuan</label>
                <select
                  value={form.unit}
                  onChange={e => setForm(f => ({ ...f, unit: e.target.value }))}
                  className="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                >
                  <option value="kg">kg</option>
                  <option value="pcs">pcs</option>
                  <option value="item">item</option>
                  <option value="pasang">pasang</option>
                  <option value="lusin">lusin</option>
                </select>
              </div>
              {error && <p className="text-destructive text-sm">{error}</p>}
              <div className="flex gap-2 pt-1">
                <button type="button" onClick={closeForm} className="flex-1 py-2 rounded-lg border border-border text-sm font-medium hover:bg-secondary transition">Batal</button>
                <button type="submit" disabled={saving} className="flex-1 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-medium hover:opacity-90 transition disabled:opacity-60">
                  {saving ? "Menyimpan..." : "Simpan"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Daftar Layanan */}
      {loading ? (
        <div className="space-y-3">
          {[...Array(3)].map((_, i) => <div key={i} className="bg-card rounded-xl p-4 border border-border animate-pulse h-16" />)}
        </div>
      ) : services.length === 0 ? (
        <div className="bg-card rounded-xl p-10 border border-border text-center">
          <svg className="w-10 h-10 text-muted-foreground mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
          </svg>
          <p className="text-muted-foreground text-sm">Belum ada layanan. Tambah layanan pertama!</p>
        </div>
      ) : (
        <div className="bg-card rounded-xl border border-border overflow-hidden shadow-sm">
          <table className="w-full text-sm">
            <thead className="bg-secondary border-b border-border">
              <tr>
                <th className="text-left px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">Layanan</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">Harga</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">Satuan</th>
                <th className="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {services.map(s => (
                <tr key={s.id} className="hover:bg-secondary/50 transition-colors">
                  <td className="px-4 py-3 font-medium text-foreground">{s.name}</td>
                  <td className="px-4 py-3 text-primary font-semibold">{formatRupiah(s.price)}</td>
                  <td className="px-4 py-3 text-muted-foreground">/{s.unit}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2 justify-end">
                      <button onClick={() => openEdit(s)} className="text-xs px-2 py-1 rounded-lg border border-border hover:bg-secondary transition text-foreground">
                        Edit
                      </button>
                      <button onClick={() => handleDelete(s.id)} className="text-xs px-2 py-1 rounded-lg border border-destructive/30 text-destructive hover:bg-destructive/10 transition">
                        Hapus
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
