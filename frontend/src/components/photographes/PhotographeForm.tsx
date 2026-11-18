import React, { useEffect, useState } from "react";
import {
  ApiError,
  apiCreatePhotographe,
  apiGetSpecialites,
  apiUpdatePhotographe,
  type Photographe,
  type Specialite,
} from "@/lib/api";
import { ErrorAlert } from "@/components/shared/ErrorAlert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Eye, EyeOff, Loader2 } from "lucide-react";

interface PhotographeFormProps {
  // Absent : création ; présent : modification de ce photographe
  photographe?: Photographe;
  onSaved: (photographe: Photographe) => void;
  onCancel: () => void;
}

export function PhotographeForm({ photographe, onSaved, onCancel }: PhotographeFormProps) {
  const [nom, setNom] = useState(photographe?.nom ?? "");
  const [email, setEmail] = useState(photographe?.email ?? "");
  const [motDePasse, setMotDePasse] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [specialiteIds, setSpecialiteIds] = useState<number[]>(
    () => photographe?.specialites.map((s) => s.id) ?? []
  );
  const [specialites, setSpecialites] = useState<Specialite[]>([]);
  const [loadingSpecs, setLoadingSpecs] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        const data = await apiGetSpecialites();
        if (mounted) setSpecialites(data);
      } catch (err) {
        if (mounted) setError(err);
      } finally {
        if (mounted) setLoadingSpecs(false);
      }
    }
    load();
    return () => {
      mounted = false;
    };
  }, []);

  const fieldErrors = error instanceof ApiError ? error.fieldErrors : undefined;

  const toggleSpecialite = (id: number) => {
    setSpecialiteIds((ids) => (ids.includes(id) ? ids.filter((i) => i !== id) : [...ids, id]));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (specialiteIds.length === 0) {
      setError("Veuillez sélectionner au moins une spécialité.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const request = {
        nom: nom.trim(),
        email: email.trim(),
        specialiteIds,
        ...(!photographe && motDePasse.trim() ? { motDePasse: motDePasse.trim() } : {}),
      };
      const saved = photographe
        ? await apiUpdatePhotographe(photographe.id, request)
        : await apiCreatePhotographe(request);
      onSaved(saved);
    } catch (err) {
      setError(err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">
          {photographe ? "Modifier le photographe" : "Nouveau photographe"}
        </CardTitle>
      </CardHeader>
      <form onSubmit={handleSubmit}>
        <CardContent className="space-y-4">
          <ErrorAlert error={error} />

          <div className="space-y-1.5">
            <Label htmlFor="photographe-nom">Nom</Label>
            <Input
              id="photographe-nom"
              value={nom}
              onChange={(e) => setNom(e.target.value)}
              maxLength={150}
              required
              aria-invalid={fieldErrors?.nom ? true : undefined}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="photographe-email">Adresse email</Label>
            <Input
              id="photographe-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              maxLength={254}
              required
              aria-invalid={fieldErrors?.email ? true : undefined}
            />
          </div>

          {!photographe && (
            <div className="space-y-1.5">
              <Label htmlFor="photographe-mdp">Mot de passe de connexion</Label>
              <div className="relative">
                <Input
                  id="photographe-mdp"
                  type={showPassword ? "text" : "password"}
                  value={motDePasse}
                  onChange={(e) => setMotDePasse(e.target.value)}
                  minLength={4}
                  maxLength={100}
                  required
                  placeholder="••••••••"
                  className="pr-10"
                  aria-invalid={fieldErrors?.motDePasse ? true : undefined}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="absolute right-1 top-1/2 -translate-y-1/2 p-1.5 text-muted-foreground hover:text-foreground cursor-pointer"
                  aria-label={showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              <p className="text-[11px] text-muted-foreground">
                Crée automatiquement le compte permettant au photographe de consulter son planning.
              </p>
            </div>
          )}

          <fieldset className="space-y-1.5">
            <legend className="text-sm font-medium leading-none mb-1.5">Spécialités</legend>
            {loadingSpecs ? (
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                Chargement des spécialités...
              </div>
            ) : (
              <div className="space-y-0.5 sm:space-y-1">
                {specialites.map((spec) => (
                  <label key={spec.id} className="flex min-h-9 items-center gap-2 text-sm cursor-pointer sm:min-h-7">
                    <input
                      type="checkbox"
                      className="size-4 shrink-0 accent-primary cursor-pointer"
                      checked={specialiteIds.includes(spec.id)}
                      onChange={() => toggleSpecialite(spec.id)}
                    />
                    {spec.libelle}
                  </label>
                ))}
              </div>
            )}
          </fieldset>
        </CardContent>
        <CardFooter className="flex flex-col-reverse items-stretch gap-2 sm:flex-row sm:items-center sm:justify-end">
          <Button
            type="button"
            variant="outline"
            className="cursor-pointer w-full h-9 sm:w-auto sm:h-8"
            onClick={onCancel}
            disabled={submitting}
          >
            Annuler
          </Button>
          <Button
            type="submit"
            className="cursor-pointer flex items-center gap-1.5 w-full h-9 sm:w-auto sm:h-8"
            disabled={submitting || loadingSpecs}
          >
            {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
            <span>{photographe ? "Enregistrer" : "Créer"}</span>
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
