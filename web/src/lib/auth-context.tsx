import { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { getToken, clearToken, getOutletId, getUser, setUser, setToken, setOutletId } from "./api";

interface AuthContextType {
  token: string | null;
  outletId: string | null;
  user: { name: string; email: string } | null;
  login: (token: string, outletId: string, user: { name: string; email: string }) => void;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(getToken);
  const [outletId, setOutletIdState] = useState<string | null>(getOutletId);
  const [user, setUserState] = useState<{ name: string; email: string } | null>(getUser);

  useEffect(() => {
    setTokenState(getToken());
    setOutletIdState(getOutletId());
    setUserState(getUser());
  }, []);

  function login(t: string, oid: string, u: { name: string; email: string }) {
    setToken(t);
    setOutletId(oid);
    setUser(u);
    setTokenState(t);
    setOutletIdState(oid);
    setUserState(u);
  }

  function logout() {
    clearToken();
    setTokenState(null);
    setOutletIdState(null);
    setUserState(null);
  }

  return (
    <AuthContext.Provider value={{ token, outletId, user, login, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
