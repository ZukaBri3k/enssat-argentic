import React, { useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { Navigate, useNavigate } from "@/lib/router";
import { useToast } from "@/components/shared/ToastProvider";
import { ErrorAlert } from "@/components/shared/ErrorAlert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
} from "@/components/ui/card";
import {
  Camera,
  Mail,
  Lock,
  Eye,
  EyeOff,
  Loader2,
  Sparkles,
} from "lucide-react";

export function LoginPage() {
  const { isAuthenticated, login, isLoading: authLoading } = useAuth();
  const navigate = useNavigate();
  const { success } = useToast();

  const [email, setEmail] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/photographes" replace />;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!email.trim() || !motDePasse) {
      setError("Veuillez renseigner votre adresse email et votre mot de passe.");
      return;
    }

    setIsSubmitting(true);
    try {
      await login(email.trim(), motDePasse);
      success("Connexion réussie", `Bienvenue sur Studio Argentic (${email.trim()})`);
      navigate("/photographes", { replace: true });
    } catch (err: unknown) {
      setError(err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const fillCredentials = (quickEmail: string, quickPass: string) => {
    setEmail(quickEmail);
    setMotDePasse(quickPass);
    setError(null);
  };

  return (
    <div className="min-h-screen w-full flex flex-col justify-between p-4 md:p-8 bg-gradient-to-b from-background via-muted/30 to-background">
      <header className="w-full max-w-5xl mx-auto flex items-center justify-between py-2">
        <div className="flex items-center gap-2 text-foreground font-semibold tracking-tight">
          <span className="w-2.5 h-2.5 rounded-full bg-primary inline-block"></span>
          <span>Studio Argentic</span>
        </div>
        <span className="text-xs text-muted-foreground font-mono">
          API : {import.meta.env.VITE_API_URL || "http://localhost:8080"}
        </span>
      </header>

      <main className="w-full flex items-center justify-center py-6">
        <div className="w-full max-w-md mx-auto text-left">
          <Card className="shadow-xl border-border">
            <CardHeader className="space-y-2 text-center items-center pb-4">
              <div className="p-3 rounded-2xl bg-primary text-primary-foreground shadow-sm">
                <Camera className="w-7 h-7" />
              </div>
              <div>
                <CardTitle className="text-2xl font-bold tracking-tight">Studio Argentic</CardTitle>
                <CardDescription className="text-sm text-muted-foreground mt-1">
                  Connectez-vous à votre espace de gestion
                </CardDescription>
              </div>
            </CardHeader>

            <form onSubmit={handleSubmit}>
              <CardContent className="space-y-4">
                <ErrorAlert error={error} />

                <div className="space-y-1.5">
                  <Label htmlFor="email">Adresse email</Label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground pointer-events-none" />
                    <Input
                      id="email"
                      type="email"
                      placeholder="nom@argentic.fr"
                      autoComplete="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="pl-9"
                      required
                      disabled={isSubmitting || authLoading}
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="motDePasse">Mot de passe</Label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground pointer-events-none" />
                    <Input
                      id="motDePasse"
                      type={showPassword ? "text" : "password"}
                      placeholder="••••••••"
                      autoComplete="current-password"
                      value={motDePasse}
                      onChange={(e) => setMotDePasse(e.target.value)}
                      className="pl-9 pr-10"
                      required
                      disabled={isSubmitting || authLoading}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-2.5 top-2.5 text-muted-foreground hover:text-foreground cursor-pointer"
                      aria-label={showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                    >
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                {/* Comptes de test rapides (seed) */}
                <div className="pt-2">
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground mb-2">
                    <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                    <span>Comptes de test (seed) :</span>
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      className="w-full text-xs cursor-pointer flex flex-col items-start py-2 h-auto"
                      onClick={() => fillCredentials("secretaire@argentic.fr", "secretaire")}
                      disabled={isSubmitting || authLoading}
                    >
                      <span className="font-semibold text-foreground">Secrétaire</span>
                      <span className="text-[10px] text-muted-foreground">secretaire@argentic.fr</span>
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      className="w-full text-xs cursor-pointer flex flex-col items-start py-2 h-auto"
                      onClick={() => fillCredentials("gestionnaire@argentic.fr", "gestionnaire")}
                      disabled={isSubmitting || authLoading}
                    >
                      <span className="font-semibold text-foreground">Gestionnaire</span>
                      <span className="text-[10px] text-muted-foreground">gestionnaire@argentic.fr</span>
                    </Button>
                  </div>
                </div>
              </CardContent>

              <CardFooter className="pt-2">
                <Button
                  type="submit"
                  className="w-full flex items-center justify-center gap-2 cursor-pointer font-medium"
                  disabled={isSubmitting || authLoading}
                >
                  {isSubmitting || authLoading ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      <span>Connexion en cours...</span>
                    </>
                  ) : (
                    <span>Se connecter</span>
                  )}
                </Button>
              </CardFooter>
            </form>
          </Card>
        </div>
      </main>

      <footer className="w-full max-w-5xl mx-auto text-center py-4 text-xs text-muted-foreground">
        © {new Date().getFullYear()} Studio Argentic — Plateforme de gestion de studio photographique
      </footer>
    </div>
  );
}
