import { createContext, useCallback, useContext, useMemo, useSyncExternalStore } from "react";
import { api, authStore } from "../api/client";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const auth = useSyncExternalStore(authStore.subscribe, authStore.get);

  const login = useCallback(async (email, password) => {
    const data = await api.login(email, password);
    authStore.set(data);
    return data;
  }, []);

  const logout = useCallback(async () => {
    try {
      if (authStore.get()) await api.logout();
    } catch {
      // best effort: clear the local session regardless
    }
    authStore.clear();
  }, []);

  const value = useMemo(() => {
    const roles = auth?.roles ?? [];
    return {
      auth,
      login,
      logout,
      userId: auth?.userId ?? null,
      isAdmin: roles.includes("ADMIN"),
      isManager: roles.includes("ADMIN") || roles.includes("SUPERINTENDENT"),
    };
  }, [auth, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
