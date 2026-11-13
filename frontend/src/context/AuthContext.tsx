import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import {
  apiGetMe,
  apiLogin,
  clearStoredToken,
  getStoredToken,
  setOnUnauthorizedListener,
  setStoredToken,
  type User,
} from "@/lib/api";

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, motDePasse: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getStoredToken());
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const logout = useCallback(() => {
    clearStoredToken();
    setToken(null);
    setUser(null);
  }, []);

  // Écouteur global 401 du client API centralisé
  useEffect(() => {
    setOnUnauthorizedListener(() => {
      logout();
    });
    return () => setOnUnauthorizedListener(null);
  }, [logout]);

  useEffect(() => {
    async function loadUser() {
      if (!token) {
        setIsLoading(false);
        return;
      }

      try {
        const userData = await apiGetMe(token);
        setUser(userData);
      } catch (err) {
        console.error("Impossible de récupérer l'utilisateur courant :", err);
        clearStoredToken();
        setToken(null);
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    }

    loadUser();
  }, [token]);

  const login = async (email: string, motDePasse: string) => {
    setIsLoading(true);
    try {
      const response = await apiLogin(email, motDePasse);
      setStoredToken(response.token);
      setToken(response.token);
      const currentUser = await apiGetMe(response.token);
      setUser(currentUser);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth doit être utilisé dans un AuthProvider");
  }
  return context;
}
