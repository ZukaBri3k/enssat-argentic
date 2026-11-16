import React, { useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { NavLink, useNavigate } from "@/lib/router";
import { useToast } from "@/components/shared/ToastProvider";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { Button } from "@/components/ui/button";
import {
  Camera,
  Users,
  Calendar,
  FileText,
  BarChart3,
  LogOut,
  ShieldCheck,
  UserCheck,
  UserSquare2,
} from "lucide-react";

interface NavItem {
  to: string;
  label: string;
  icon: React.ReactNode;
  gestionnaireOnly?: boolean;
}

const NAV_ITEMS: NavItem[] = [
  {
    to: "/photographes",
    label: "Photographes",
    icon: <UserSquare2 className="w-4 h-4" />,
  },
  {
    to: "/clients",
    label: "Clients",
    icon: <Users className="w-4 h-4" />,
  },
  {
    to: "/seances",
    label: "Séances / Planning",
    icon: <Calendar className="w-4 h-4" />,
  },
  {
    to: "/factures",
    label: "Factures",
    icon: <FileText className="w-4 h-4" />,
  },
  {
    to: "/statistiques",
    label: "Statistiques",
    icon: <BarChart3 className="w-4 h-4" />,
    gestionnaireOnly: true,
  },
];

export function AppLayout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { info } = useToast();
  const [confirmLogoutOpen, setConfirmLogoutOpen] = useState(false);

  const isGestionnaire = user?.role === "GESTIONNAIRE";

  const visibleNavItems = NAV_ITEMS.filter(
    (item) => !item.gestionnaireOnly || isGestionnaire
  );

  const handleLogoutConfirm = () => {
    setConfirmLogoutOpen(false);
    logout();
    info("Déconnexion", "Vous avez été déconnecté avec succès.");
    navigate("/login", { replace: true });
  };

  return (
    <div className="min-h-screen w-full flex flex-col bg-muted/20 text-left">
      {/* Top Navbar */}
      <header className="sticky top-0 z-30 w-full border-b border-border bg-background/95 backdrop-blur-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
          {/* Logo & Navigation */}
          <div className="flex items-center gap-6 overflow-x-auto">
            <div className="flex items-center gap-2.5 shrink-0">
              <div className="p-2 rounded-xl bg-primary text-primary-foreground shadow-xs">
                <Camera className="w-5 h-5" />
              </div>
              <div className="leading-tight">
                <span className="font-bold text-base tracking-tight text-foreground block">
                  Studio Argentic
                </span>
                <span className="text-[11px] text-muted-foreground block">
                  Gestion du studio
                </span>
              </div>
            </div>

            <nav aria-label="Navigation principale" className="flex items-center gap-1">
              {visibleNavItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `inline-flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors whitespace-nowrap ${
                      isActive
                        ? "bg-primary text-primary-foreground shadow-xs"
                        : "text-muted-foreground hover:text-foreground hover:bg-muted"
                    }`
                  }
                >
                  {item.icon}
                  <span>{item.label}</span>
                </NavLink>
              ))}
            </nav>
          </div>

          {/* Utilisateur courant & Déconnexion */}
          {user && (
            <div className="flex items-center gap-3 shrink-0">
              <div className="hidden md:flex flex-col items-end">
                <span className="text-xs font-medium text-foreground">{user.email}</span>
                <span
                  className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold mt-0.5 ${
                    isGestionnaire
                      ? "bg-purple-100 text-purple-800 dark:bg-purple-950 dark:text-purple-300 border border-purple-200 dark:border-purple-800"
                      : "bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300 border border-blue-200 dark:border-blue-800"
                  }`}
                >
                  {isGestionnaire ? (
                    <ShieldCheck className="w-3 h-3" />
                  ) : (
                    <UserCheck className="w-3 h-3" />
                  )}
                  {user.role}
                </span>
              </div>

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setConfirmLogoutOpen(true)}
                className="cursor-pointer flex items-center gap-1.5"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Déconnexion</span>
              </Button>
            </div>
          )}
        </div>
      </header>

      {/* Contenu principal */}
      <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {children}
      </main>

      {/* Footer */}
      <footer className="w-full border-t border-border bg-background py-4">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between text-xs text-muted-foreground gap-2">
          <span>© {new Date().getFullYear()} Studio Argentic — Gestion de studio photographique</span>
          <span className="font-mono">API : {import.meta.env.VITE_API_URL || "http://localhost:8080"}</span>
        </div>
      </footer>

      {/* Modale de confirmation de déconnexion */}
      <ConfirmDialog
        open={confirmLogoutOpen}
        onOpenChange={setConfirmLogoutOpen}
        title="Se déconnecter ?"
        description="Votre session sera fermée et le jeton d'authentification sera supprimé de ce navigateur."
        confirmLabel="Se déconnecter"
        cancelLabel="Rester connecté"
        onConfirm={handleLogoutConfirm}
      />
    </div>
  );
}
