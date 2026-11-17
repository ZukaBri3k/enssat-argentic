import React, { useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { NavLink, useNavigate } from "@/lib/router";
import { useToast } from "@/components/shared/ToastProvider";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { Button } from "@/components/ui/button";
import { ROLE_LABELS } from "@/lib/api";
import { TONS } from "@/lib/tons";
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
  Menu,
  X,
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
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    if (!menuOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setMenuOpen(false);
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [menuOpen]);

  const isGestionnaire = user?.role === "GESTIONNAIRE";

  const visibleNavItems = NAV_ITEMS.filter(
    (item) => !item.gestionnaireOnly || isGestionnaire
  );

  const navLinkClassName = ({ isActive }: { isActive: boolean }) =>
    `inline-flex items-center gap-2 px-3 rounded-lg text-sm font-medium transition-colors whitespace-nowrap ${
      isActive
        ? "bg-primary text-primary-foreground shadow-xs"
        : "text-muted-foreground hover:text-foreground hover:bg-muted"
    }`;

  const roleBadge = user && (
    <span
      className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold border ${
        isGestionnaire ? TONS.accent.badge : TONS.info.badge
      }`}
    >
      {isGestionnaire ? <ShieldCheck className="w-3 h-3" /> : <UserCheck className="w-3 h-3" />}
      {ROLE_LABELS[user.role]}
    </span>
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
          <div className="flex items-center gap-6 min-w-0">
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

            <nav aria-label="Navigation principale" className="hidden lg:flex items-center gap-1">
              {visibleNavItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={(state) => `${navLinkClassName(state)} py-2`}
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
              <div className="hidden xl:flex flex-col items-end gap-0.5">
                <span className="text-xs font-medium text-foreground">{user.email}</span>
                {roleBadge}
              </div>

              <Button
                type="button"
                variant="outline"
                onClick={() => setConfirmLogoutOpen(true)}
                className="hidden lg:inline-flex cursor-pointer items-center gap-1.5"
                aria-label="Déconnexion"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span className="hidden xl:inline">Déconnexion</span>
              </Button>

              {/* Bouton du menu mobile */}
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={() => setMenuOpen((open) => !open)}
                className="lg:hidden cursor-pointer size-10"
                aria-label={menuOpen ? "Fermer le menu" : "Ouvrir le menu"}
                aria-expanded={menuOpen}
                aria-controls="menu-mobile"
              >
                {menuOpen ? <X className="size-5" /> : <Menu className="size-5" />}
              </Button>
            </div>
          )}
        </div>

        {/* Menu mobile */}
        {menuOpen && user && (
          <div id="menu-mobile" className="lg:hidden border-t border-border bg-background">
            <nav
              aria-label="Navigation principale"
              className="max-w-7xl mx-auto px-4 sm:px-6 py-3 flex flex-col gap-1"
            >
              {visibleNavItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={() => setMenuOpen(false)}
                  className={(state) => `${navLinkClassName(state)} py-3`}
                >
                  {item.icon}
                  <span>{item.label}</span>
                </NavLink>
              ))}
            </nav>
            <div className="max-w-7xl mx-auto px-4 sm:px-6 py-3 border-t border-border flex items-center justify-between gap-3">
              <div className="flex flex-col items-start gap-1 min-w-0">
                <span className="text-xs font-medium text-foreground truncate max-w-full">
                  {user.email}
                </span>
                {roleBadge}
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setMenuOpen(false);
                  setConfirmLogoutOpen(true);
                }}
                className="cursor-pointer flex items-center gap-1.5 shrink-0 h-10"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Déconnexion</span>
              </Button>
            </div>
          </div>
        )}
      </header>

      {/* Contenu principal */}
      <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        {children}
      </main>

      {/* Footer */}
      <footer className="w-full border-t border-border bg-background py-4">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center sm:text-left text-xs text-muted-foreground">
          © {new Date().getFullYear()} Studio Argentic — Gestion de studio photographique
        </div>
      </footer>

      {/* Modale de confirmation de déconnexion */}
      <ConfirmDialog
        open={confirmLogoutOpen}
        onOpenChange={setConfirmLogoutOpen}
        title="Se déconnecter ?"
        description="Votre session sera fermée sur ce navigateur."
        confirmLabel="Se déconnecter"
        cancelLabel="Rester connecté"
        onConfirm={handleLogoutConfirm}
      />
    </div>
  );
}
