const API_BASE = import.meta.env.VITE_API_URL ?? "https://nexpos-production-3747.up.railway.app";

export function getToken(): string | null {
  return localStorage.getItem("nexpos_token");
}

export function setToken(token: string) {
  localStorage.setItem("nexpos_token", token);
}

export function clearToken() {
  localStorage.removeItem("nexpos_token");
  localStorage.removeItem("nexpos_outlet_id");
  localStorage.removeItem("nexpos_user");
}

export function getOutletId(): string | null {
  return localStorage.getItem("nexpos_outlet_id");
}

export function setOutletId(id: string) {
  localStorage.setItem("nexpos_outlet_id", id);
}

export function getUser(): { name: string; email: string } | null {
  const raw = localStorage.getItem("nexpos_user");
  return raw ? JSON.parse(raw) : null;
}

export function setUser(user: { name: string; email: string }) {
  localStorage.setItem("nexpos_user", JSON.stringify(user));
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: "Terjadi kesalahan" }));
    throw new Error(err.message ?? "Terjadi kesalahan");
  }
  return res.json();
}

export const api = {
  // Auth
  login: (email: string, password: string) =>
    request<{ token: string; user: { name: string; email: string } }>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),

  // Customers
  getCustomers: (outletId: string) =>
    request<{ customers: Customer[] }>(`/api/customers?outletId=${outletId}`),
  searchCustomers: (outletId: string, q: string) =>
    request<{ customers: Customer[] }>(`/api/customers/search?outletId=${outletId}&q=${encodeURIComponent(q)}`),
  createCustomer: (data: { outletId: string; name: string; phone: string; address: string }) =>
    request<{ customer: Customer }>("/api/customers", { method: "POST", body: JSON.stringify(data) }),
  updateCustomer: (id: string, data: { name: string; phone: string; address: string }) =>
    request<{ customer: Customer }>(`/api/customers/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteCustomer: (id: string) =>
    request<{ message: string }>(`/api/customers/${id}`, { method: "DELETE" }),

  // Services
  getServices: (outletId: string) =>
    request<{ services: Service[] }>(`/api/services?outletId=${outletId}`),
  createService: (data: { outletId: string; name: string; price: number; unit: string }) =>
    request<{ service: Service }>("/api/services", { method: "POST", body: JSON.stringify(data) }),
  updateService: (id: string, data: { name: string; price: number; unit: string }) =>
    request<{ service: Service }>(`/api/services/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteService: (id: string) =>
    request<{ message: string }>(`/api/services/${id}`, { method: "DELETE" }),

  // Transactions
  getTransactions: (outletId: string) =>
    request<{ transactions: Transaction[] }>(`/api/transactions?outletId=${outletId}`),
  createTransaction: (data: { outletId: string; customerId: string; serviceId: string; quantity: number }) =>
    request<{ transaction: Transaction }>("/api/transactions", { method: "POST", body: JSON.stringify(data) }),
  updateStatus: (id: string | number, status: string) =>
    request<{ transaction: Transaction }>(`/api/transactions/${id}/status`, {
      method: "PUT",
      body: JSON.stringify({ status }),
    }),

  // Reports
  getReport: (outletId: string) =>
    request<ReportSummary>(`/api/reports/summary?outletId=${outletId}`),
};

export interface Customer {
  id: string;
  name: string;
  phone: string;
  address: string;
  outlet_id: string;
  created_at: string;
}

export interface Service {
  id: string;
  name: string;
  price: number;
  unit: string;
  outlet_id: string;
  created_at: string;
}

export interface Transaction {
  id: string | number;
  customerId: string;
  customerName: string;
  serviceId: string;
  serviceName: string;
  servicePrice: number;
  serviceUnit: string;
  quantity: number;
  totalAmount: number;
  status: string;
  outletId: string | number;
  outletName: string;
  createdAt: string;
  updatedAt: string;
}

export interface ReportSummary {
  total_transactions: number;
  total_income: number;
  total_pending: number;
  total_process: number;
  total_done: number;
  total_picked: number;
}

export const STATUS_LABELS: Record<string, string> = {
  pending: "Menunggu",
  process: "Diproses",
  done: "Selesai",
  picked: "Diambil",
  diterima: "Diterima",
  dicuci: "Dicuci",
  disetrika: "Disetrika",
  selesai: "Selesai",
};

export const STATUS_COLORS: Record<string, string> = {
  pending: "bg-yellow-100 text-yellow-800",
  process: "bg-blue-100 text-blue-800",
  done: "bg-green-100 text-green-800",
  picked: "bg-purple-100 text-purple-800",
  diterima: "bg-yellow-100 text-yellow-800",
  dicuci: "bg-blue-100 text-blue-800",
  disetrika: "bg-orange-100 text-orange-800",
  selesai: "bg-green-100 text-green-800",
};

export function formatRupiah(amount: number): string {
  return new Intl.NumberFormat("id-ID", {
    style: "currency",
    currency: "IDR",
    minimumFractionDigits: 0,
  }).format(amount);
}
