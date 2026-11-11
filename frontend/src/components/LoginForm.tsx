import React, { useState } from "react";
import { useAuth } from "@/context/AuthContext";
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
  AlertCircle,
  Loader2,
  Sparkles,
  ShieldCheck,
  UserCheck,
  LogOut,
  Copy,
  Check,
  ExternalLink,
  Play,
} from "lucide-react";
import { ApiError, API_URL, apiGetSpecialites, apiGetStatsPing } from "@/lib/api";

export function LoginForm() {
  const { user, token, isAuthenticated, login, logout, isLoading: authLoading } = useAuth();

  const [email, setEmail] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [copiedToken, setCopiedToken] = useState(false);
  const [apiTestResult, setApiTestResult] = useState<{
    route: string;
    status: number;
    ok: boolean;
    data: string;
  } | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!email.trim() || !motDePasse) {
      setError("Veuillez renseigner votre email et votre mot de passe.");
      return;
    }

    setIsSubmitting(true);
    try {
      await login(email.trim(), motDePasse);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        if (err.status === 401) {
          setError("Identifiants invalides. Veuillez vérifier votre adresse email et votre mot de passe.");
        } else if (err.status === 400) {
          setError(err.message || "Requête invalide. Vérifiez le format de vos données.");
        } else {
          setError(err.message || "Une erreur est survenue lors de la connexion.");
        }
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError("Impossible de joindre le serveur. Assurez-vous que le backend est démarré.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const fillCredentials = (quickEmail: string, quickPass: string) => {
    setEmail(quickEmail);
    setMotDePasse(quickPass);
    setError(null);
  };

  const handleCopyToken = () => {
    if (!token) return;
    navigator.clipboard.writeText(token);
    setCopiedToken(true);
    setTimeout(() => setCopiedToken(false), 2000);
  };

  const testRoute = async (route: string, fn: () => Promise<unknown>) => {
    setApiTestResult(null);
    try {
      const res = await fn();
      setApiTestResult({
        route,
        status: 200,
        ok: true,
        data: JSON.stringify(res, null, 2),
      });
    } catch (err) {
      if (err instanceof ApiError) {
        setApiTestResult({
          route,
          status: err.status,
          ok: false,
          data: JSON.stringify(err.problem ?? { error: err.message }, null, 2),
        });
      } else {
        setApiTestResult({
          route,
          status: 500,
          ok: false,
          data: err instanceof Error ? err.message : "Erreur réseau",
        });
      }
    }
  };

  // État connecté : afficher le profil et l'accès
  if (isAuthenticated && user) {
    const isGestionnaire = user.role === "GESTIONNAIRE";

    return (
      <div className="w-full max-w-md mx-auto my-auto text-left">
        <Card className="shadow-lg border-border">
          <CardHeader className="space-y-1 pb-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-lg bg-primary text-primary-foreground">
                  <Camera className="w-5 h-5" />
                </div>
                <div>
                  <CardTitle className="text-xl">Studio Argentic</CardTitle>
                  <CardDescription>Session active</CardDescription>
                </div>
              </div>
              <span
                className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                  isGestionnaire
                    ? "bg-purple-100 text-purple-800 dark:bg-purple-950 dark:text-purple-300 border border-purple-200 dark:border-purple-800"
                    : "bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300 border border-blue-200 dark:border-blue-800"
                }`}
              >
                {isGestionnaire ? <ShieldCheck className="w-3.5 h-3.5" /> : <UserCheck className="w-3.5 h-3.5" />}
                {user.role}
              </span>
            </div>
          </CardHeader>

          <CardContent className="space-y-4 pt-2">
            <div className="p-3 rounded-lg bg-muted/60 border border-border space-y-1.5">
              <div className="text-xs text-muted-foreground uppercase tracking-wider font-semibold">
                Utilisateur connecté
              </div>
              <div className="font-medium text-foreground flex items-center justify-between">
                <span>{user.email}</span>
                <span className="text-xs text-muted-foreground">ID: #{user.id}</span>
              </div>
              <div className="text-xs text-muted-foreground pt-1">
                {isGestionnaire
                  ? "Accès complet : Gestion studio, photographes, clients, factures et statistiques (/stats/**)."
                  : "Accès secrétaire : Gestion des photographes, clients, séances et facturation."}
              </div>
            </div>

            {token && (
              <div className="space-y-3">
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between text-xs text-muted-foreground">
                    <span className="font-medium">Tester les routes API du Backend ({API_URL})</span>
                    <a
                      href={`${API_URL}/swagger-ui/index.html`}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-1 text-primary hover:underline"
                    >
                      <span>Swagger UI</span>
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      className="text-xs cursor-pointer flex items-center gap-1.5"
                      onClick={() => testRoute("GET /specialites", () => apiGetSpecialites(token))}
                    >
                      <Play className="w-3 h-3" />
                      GET /specialites
                    </Button>
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      className="text-xs cursor-pointer flex items-center gap-1.5"
                      onClick={() => testRoute("GET /stats/ping", () => apiGetStatsPing(token))}
                    >
                      <Play className="w-3 h-3" />
                      GET /stats/ping
                    </Button>
                  </div>

                  {apiTestResult && (
                    <div
                      className={`p-2.5 rounded-lg border text-xs font-mono space-y-1 ${
                        apiTestResult.ok
                          ? "bg-green-500/10 border-green-500/30 text-green-700 dark:text-green-300"
                          : "bg-destructive/10 border-destructive/30 text-destructive"
                      }`}
                    >
                      <div className="flex items-center justify-between font-sans font-semibold">
                        <span>{apiTestResult.route}</span>
                        <span>HTTP {apiTestResult.status}</span>
                      </div>
                      <pre className="text-[11px] overflow-x-auto whitespace-pre-wrap max-h-32">
                        {apiTestResult.data}
                      </pre>
                    </div>
                  )}
                </div>

                <div className="space-y-1.5">
                  <div className="flex items-center justify-between text-xs text-muted-foreground">
                    <span className="font-medium">Token JWT (Bearer HMAC)</span>
                    <button
                      type="button"
                      onClick={handleCopyToken}
                      className="inline-flex items-center gap-1 text-primary hover:underline cursor-pointer"
                    >
                      {copiedToken ? (
                        <>
                          <Check className="w-3.5 h-3.5 text-green-600" />
                          <span className="text-green-600">Copié</span>
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5" />
                          <span>Copier</span>
                        </>
                      )}
                    </button>
                  </div>
                  <div className="p-2 rounded bg-muted/40 border border-border text-[11px] font-mono text-muted-foreground break-all select-all max-h-20 overflow-y-auto">
                    {token}
                  </div>
                </div>
              </div>
            )}
          </CardContent>

          <CardFooter className="pt-2">
            <Button
              variant="outline"
              className="w-full flex items-center justify-center gap-2 cursor-pointer"
              onClick={() => {
                setApiTestResult(null);
                logout();
              }}
            >
              <LogOut className="w-4 h-4" />
              Se déconnecter
            </Button>
          </CardFooter>
        </Card>
      </div>
    );
  }

  return (
    <div className="w-full max-w-md mx-auto my-auto text-left">
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
            {error && (
              <div
                className="flex items-start gap-2.5 p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-sm"
                role="alert"
              >
                <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
                <div className="leading-snug">{error}</div>
              </div>
            )}

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

            {/* Comptes de test rapides */}
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
  );
}
