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
import { Loader2 } from "lucide-react";

interface PhotographeFormProps {
  // Absent : création ; présent : modification de ce photographe
  photographe?: Photographe;
  onSaved: (photographe: Photographe) => void;
  onCancel: () => void;
}

export function PhotographeForm({ photographe, onSaved, onCancel }: PhotographeFormProps) {
  const [nom, setNom] = useState(photographe?.nom ?? "");
  const [email, setEmail] = useState(photographe?.email ?? "");
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
      setError("Au moins une spécialité est requise.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const request = { nom: nom.trim(), email: email.trim(), specialiteIds };
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

          <fieldset className="space-y-1.5">
            <legend className="text-sm font-medium leading-none mb-1.5">Spécialités</legend>
            {loadingSpecs ? (
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                Chargement des spécialités...
              </div>
            ) : (
              <div className="space-y-1.5">
                {specialites.map((spec) => (
                  <label key={spec.id} className="flex items-center gap-2 text-sm cursor-pointer">
                    <input
                      type="checkbox"
                      className="size-4 accent-primary cursor-pointer"
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
        <CardFooter className="flex items-center justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            className="cursor-pointer"
            onClick={onCancel}
            disabled={submitting}
          >
            Annuler
          </Button>
          <Button
            type="submit"
            className="cursor-pointer flex items-center gap-1.5"
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
