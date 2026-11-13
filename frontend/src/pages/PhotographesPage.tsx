import { useEffect, useState } from "react";
import { apiGetPhotographe, apiGetPhotographes, type Photographe, type Specialite } from "@/lib/api";
import { ErrorAlert } from "@/components/shared/ErrorAlert";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Loader2, Mail, UserSquare2, X } from "lucide-react";

type FiltreActif = "tous" | "actifs" | "inactifs";

const FILTRES: { value: FiltreActif; label: string; actif?: boolean }[] = [
  { value: "tous", label: "Tous" },
  { value: "actifs", label: "Actifs", actif: true },
  { value: "inactifs", label: "Inactifs", actif: false },
];

function StatutBadge({ actif }: { actif: boolean }) {
  return actif ? (
    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-100 text-emerald-800 border border-emerald-300 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800">
      Actif
    </span>
  ) : (
    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-muted text-muted-foreground border border-border">
      Inactif
    </span>
  );
}

function SpecialitesBadges({ specialites }: { specialites: Specialite[] }) {
  return (
    <div className="flex flex-wrap gap-1">
      {specialites.map((spec) => (
        <span
          key={spec.id}
          className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-secondary text-secondary-foreground border border-border"
        >
          {spec.libelle}
        </span>
      ))}
    </div>
  );
}

function PhotographeDetail({ id, onClose }: { id: number; onClose: () => void }) {
  const [photographe, setPhotographe] = useState<Photographe | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let mounted = true;
    async function load() {
      setLoading(true);
      setError(null);
      setPhotographe(null);
      try {
        const data = await apiGetPhotographe(id);
        if (mounted) setPhotographe(data);
      } catch (err) {
        if (mounted) {
          setPhotographe(null);
          setError(err);
        }
      } finally {
        if (mounted) setLoading(false);
      }
    }
    load();
    return () => {
      mounted = false;
    };
  }, [id]);

  return (
    <Card>
      <CardHeader className="flex flex-row items-start justify-between gap-2">
        <div className="space-y-1">
          <CardTitle className="text-base">
            {photographe ? photographe.nom : "Détail du photographe"}
          </CardTitle>
          {photographe && (
            <CardDescription className="flex items-center gap-1.5">
              <Mail className="w-3.5 h-3.5" />
              <span>{photographe.email}</span>
            </CardDescription>
          )}
        </div>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          className="cursor-pointer"
          onClick={onClose}
          aria-label="Fermer le détail"
        >
          <X />
        </Button>
      </CardHeader>
      <CardContent className="space-y-4 text-sm">
        {loading ? (
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Loader2 className="w-3.5 h-3.5 animate-spin" />
            Chargement...
          </div>
        ) : error ? (
          <ErrorAlert error={error} />
        ) : (
          photographe && (
            <>
              <div className="space-y-1.5">
                <div className="text-xs font-medium text-muted-foreground">Statut</div>
                <StatutBadge actif={photographe.actif} />
              </div>
              <div className="space-y-1.5">
                <div className="text-xs font-medium text-muted-foreground">Spécialités</div>
                <SpecialitesBadges specialites={photographe.specialites} />
              </div>
            </>
          )
        )}
      </CardContent>
    </Card>
  );
}

export function PhotographesPage() {
  const [filtre, setFiltre] = useState<FiltreActif>("tous");
  const [photographes, setPhotographes] = useState<Photographe[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);

  useEffect(() => {
    let mounted = true;
    async function load() {
      setLoading(true);
      setLoadError(null);
      try {
        const actif = FILTRES.find((f) => f.value === filtre)?.actif;
        const data = await apiGetPhotographes(actif);
        if (mounted) setPhotographes(data);
      } catch (err) {
        if (mounted) {
          setPhotographes([]);
          setLoadError(err);
        }
      } finally {
        if (mounted) setLoading(false);
      }
    }
    load();
    return () => {
      mounted = false;
    };
  }, [filtre]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2.5">
            <UserSquare2 className="w-6 h-6 text-primary" />
            <span>Photographes</span>
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Gestion de l'équipe de photographes, de leurs spécialités et de leur statut actif/inactif.
          </p>
        </div>

        <div className="flex items-center gap-1" role="group" aria-label="Filtrer par statut">
          {FILTRES.map(({ value, label }) => (
            <Button
              key={value}
              type="button"
              size="sm"
              variant={filtre === value ? "default" : "outline"}
              className="cursor-pointer"
              aria-pressed={filtre === value}
              onClick={() => {
                setFiltre(value);
                setSelectedId(null);
              }}
            >
              {label}
            </Button>
          ))}
        </div>
      </div>

      <ErrorAlert error={loadError} />

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className={selectedId !== null ? "lg:col-span-2" : "lg:col-span-3"}>
          <CardContent className="overflow-x-auto">
            {loading ? (
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                Chargement des photographes...
              </div>
            ) : loadError ? null : photographes.length === 0 ? (
              <div className="text-xs text-muted-foreground">Aucun photographe trouvé.</div>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-xs text-muted-foreground">
                    <th className="py-2 pr-4 font-medium">Nom</th>
                    <th className="py-2 pr-4 font-medium">Email</th>
                    <th className="py-2 pr-4 font-medium">Spécialités</th>
                    <th className="py-2 font-medium">Statut</th>
                  </tr>
                </thead>
                <tbody>
                  {photographes.map((p) => (
                    <tr
                      key={p.id}
                      className={`border-b border-border last:border-0 hover:bg-muted/50 ${
                        selectedId === p.id ? "bg-muted" : ""
                      }`}
                    >
                      <td className="py-2.5 pr-4 font-medium">
                        <button
                          type="button"
                          className="cursor-pointer text-left hover:underline"
                          onClick={() => setSelectedId(p.id)}
                        >
                          {p.nom}
                        </button>
                      </td>
                      <td className="py-2.5 pr-4 text-muted-foreground">{p.email}</td>
                      <td className="py-2.5 pr-4">
                        <SpecialitesBadges specialites={p.specialites} />
                      </td>
                      <td className="py-2.5">
                        <StatutBadge actif={p.actif} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </CardContent>
        </Card>

        {selectedId !== null && (
          <PhotographeDetail id={selectedId} onClose={() => setSelectedId(null)} />
        )}
      </div>
    </div>
  );
}
