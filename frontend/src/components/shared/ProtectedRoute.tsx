import React from "react";
import { useAuth } from "@/context/AuthContext";
import { Navigate, Link } from "@/lib/router";
import type { Role } from "@/lib/api";
import { Loader2, ShieldAlert, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from "@/components/ui/card";

interface ProtectedRouteProps {
  requiredRole?: Role;
  children: React.ReactNode;
}

export function hasRequiredRole(userRole: Role, requiredRole?: Role): boolean {
  if (!requiredRole) return true;
  if (userRole === "GESTIONNAIRE") return true; // GESTIONNAIRE possède aussi tous les droits de SECRETAIRE
  return userRole === requiredRole;
}

export function ProtectedRoute({ requiredRole, children }: ProtectedRouteProps) {
  const { user, isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="flex items-center gap-2.5 text-sm text-muted-foreground">
          <Loader2 className="w-5 h-5 animate-spin text-primary" />
          <span>Vérification de la session...</span>
        </div>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (!hasRequiredRole(user.role, requiredRole)) {
    return (
      <div className="max-w-md mx-auto my-12 text-left">
        <Card className="border-destructive/30">
          <CardHeader className="space-y-2">
            <div className="w-10 h-10 rounded-xl bg-destructive/15 text-destructive flex items-center justify-center">
              <ShieldAlert className="w-5 h-5" />
            </div>
            <CardTitle className="text-lg">Accès refusé (403)</CardTitle>
            <CardDescription>
              Cette rubrique nécessite le rôle{" "}
              <span className="font-semibold text-foreground">{requiredRole}</span>. Votre compte
              actuel dispose du rôle{" "}
              <span className="font-semibold text-foreground">{user.role}</span>.
            </CardDescription>
          </CardHeader>
          <CardContent className="text-xs text-muted-foreground">
            Seul un gestionnaire peut consulter les statistiques du studio.
          </CardContent>
          <CardFooter>
            <Link to="/photographes" className="w-full">
              <Button variant="outline" className="w-full flex items-center gap-2 cursor-pointer">
                <ArrowLeft className="w-4 h-4" />
                Retour aux photographes
              </Button>
            </Link>
          </CardFooter>
        </Card>
      </div>
    );
  }

  return <>{children}</>;
}
