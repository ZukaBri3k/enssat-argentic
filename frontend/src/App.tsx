import "./App.css";
import { BrowserRouter, Navigate, useLocation } from "@/lib/router";
import { ToastProvider } from "@/components/shared/ToastProvider";
import { AuthProvider } from "@/context/AuthContext";
import { ProtectedRoute } from "@/components/shared/ProtectedRoute";
import { AppLayout } from "@/components/layout/AppLayout";
import { LoginPage } from "@/pages/LoginPage";
import { PhotographesPage } from "@/pages/PhotographesPage";
import { ClientsPage } from "@/pages/ClientsPage";
import { SeancesPage } from "@/pages/SeancesPage";
import { FacturesPage } from "@/pages/FacturesPage";
import { StatistiquesPage } from "@/pages/StatistiquesPage";

function AppRoutes() {
  const { pathname } = useLocation();

  if (pathname === "/login") {
    return <LoginPage />;
  }

  if (pathname === "/") {
    return <Navigate to="/photographes" replace />;
  }

  if (pathname === "/photographes") {
    return (
      <ProtectedRoute>
        <AppLayout>
          <PhotographesPage />
        </AppLayout>
      </ProtectedRoute>
    );
  }

  if (pathname === "/clients") {
    return (
      <ProtectedRoute>
        <AppLayout>
          <ClientsPage />
        </AppLayout>
      </ProtectedRoute>
    );
  }

  if (pathname === "/seances") {
    return (
      <ProtectedRoute>
        <AppLayout>
          <SeancesPage />
        </AppLayout>
      </ProtectedRoute>
    );
  }

  if (pathname === "/factures") {
    return (
      <ProtectedRoute>
        <AppLayout>
          <FacturesPage />
        </AppLayout>
      </ProtectedRoute>
    );
  }

  if (pathname === "/statistiques" || pathname === "/stats") {
    return (
      <ProtectedRoute>
        <AppLayout>
          <ProtectedRoute requiredRole="GESTIONNAIRE">
            <StatistiquesPage />
          </ProtectedRoute>
        </AppLayout>
      </ProtectedRoute>
    );
  }

  return <Navigate to="/photographes" replace />;
}

function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
