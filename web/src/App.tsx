import { Switch, Route, Router as WouterRouter } from "wouter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider, useAuth } from "./lib/auth-context";
import Layout from "./components/Layout";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import KasirPage from "./pages/KasirPage";
import TransaksiPage from "./pages/TransaksiPage";
import CustomerPage from "./pages/CustomerPage";
import LayananPage from "./pages/LayananPage";
import LaporanPage from "./pages/LaporanPage";

const queryClient = new QueryClient();

function AppRoutes() {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <LoginPage />;
  return (
    <Layout>
      <Switch>
        <Route path="/" component={DashboardPage} />
        <Route path="/kasir" component={KasirPage} />
        <Route path="/transaksi" component={TransaksiPage} />
        <Route path="/customer" component={CustomerPage} />
        <Route path="/layanan" component={LayananPage} />
        <Route path="/laporan" component={LaporanPage} />
        <Route>
          <div className="text-center py-20">
            <p className="text-muted-foreground">Halaman tidak ditemukan</p>
          </div>
        </Route>
      </Switch>
    </Layout>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <WouterRouter>
          <AppRoutes />
        </WouterRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
