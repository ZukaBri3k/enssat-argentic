import { useEffect, useState } from "react";
import {
  apiActiverPhotographe,
  apiDeletePhotographe,
  apiDesactiverPhotographe,
  apiGetPhotographe,
  apiGetPhotographes,
  apiGetSeancesAVenir,
  type Photographe,
  type SeanceAVenir,
  type Specialite,
} from "@/lib/api";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { ErrorAlert } from "@/components/shared/ErrorAlert";
import { useToast } from "@/components/shared/ToastProvider";
import { PhotographeForm } from "@/components/photographes/PhotographeForm";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Loader2,
  Mail,
  Pencil,
  Plus,
  Power,
  PowerOff,
  Trash2,
  UserSquare2,
  X,
} from "lucide-react";

type FiltreActif = "tous" | "actifs" | "inactifs";

// Contenu du panneau latéral
type Panneau =
  | { mode: "detail"; id: number }
  | { mode: "creation" }
  | { mode: "edition"; photographe: Photographe };

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

const FORMAT_JOUR = new Intl.DateTimeFormat("fr-FR", {
  weekday: "short",
  day: "numeric",
  month: "short",
  year: "numeric",
});
const FORMAT_HEURE = new Intl.DateTimeFormat("fr-FR", { hour: "2-digit", minute: "2-digit" });

function SeancesAAnnulerListe({ seances }: { seances: SeanceAVenir[] }) {
  return (
    <ul className="max-h-60 overflow-y-auto divide-y divide-border rounded-lg border border-border">
      {seances.map((seance) => {
        const debut = new Date(seance.debut);
        return (
          <li key={seance.id} className="px-3 py-2 space-y-0.5">
            <div className="font-medium">
              {FORMAT_JOUR.format(debut)}, {FORMAT_HEURE.format(debut)} –{" "}
              {FORMAT_HEURE.format(new Date(seance.fin))}
            </div>
            <div className="text-xs text-muted-foreground">
              {seance.specialite.libelle} · {seance.clientIdentite} · {seance.lieu}
            </div>
          </li>
        );
      })}
    </ul>
  );
}

interface PhotographeDetailProps {
  id: number;
  onEdit: (photographe: Photographe) => void;
  onDeleted: (photographe: Photographe) => void;
  onStatutChanged: (photographe: Photographe) => void;
  onClose: () => void;
}

function PhotographeDetail({
  id,
  onEdit,
  onDeleted,
  onStatutChanged,
  onClose,
}: PhotographeDetailProps) {
  const [photographe, setPhotographe] = useState<Photographe | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<unknown>(null);
  // Séances à venir affichées dans la confirmation de désactivation (null : modale fermée)
  const [seancesAAnnuler, setSeancesAAnnuler] = useState<SeanceAVenir[] | null>(null);
  const [changingStatut, setChangingStatut] = useState(false);
  const [statutError, setStatutError] = useState<unknown>(null);

  useEffect(() => {
    let mounted = true;
    async function load() {
      setLoading(true);
      setError(null);
      setPhotographe(null);
      setDeleteError(null);
      setStatutError(null);
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

  const handleDelete = async () => {
    if (!photographe) return;
    setDeleting(true);
    try {
      await apiDeletePhotographe(photographe.id);
      onDeleted(photographe);
    } catch (err) {
      // Refus (409 si le photographe a des séances) : message du back affiché dans le panneau
      setDeleteError(err);
    } finally {
      setDeleting(false);
      setConfirmDelete(false);
    }
  };

  const desactiver = async () => {
    if (!photographe) return;
    try {
      const desactive = await apiDesactiverPhotographe(photographe.id);
      setPhotographe(desactive);
      onStatutChanged(desactive);
    } catch (err) {
      setStatutError(err);
    } finally {
      setSeancesAAnnuler(null);
    }
  };

  // Sans séance à venir, la désactivation est immédiate ; sinon une confirmation liste les séances qui seront annulées
  const handleDesactiver = async () => {
    if (!photographe) return;
    setStatutError(null);
    setChangingStatut(true);
    try {
      const seances = await apiGetSeancesAVenir(photographe.id);
      if (seances.length === 0) {
        await desactiver();
      } else {
        setSeancesAAnnuler(seances);
      }
    } catch (err) {
      setStatutError(err);
    } finally {
      setChangingStatut(false);
    }
  };

  const handleConfirmDesactiver = async () => {
    if (!seancesAAnnuler) return;
    setChangingStatut(true);
    await desactiver();
    setChangingStatut(false);
  };

  const handleActiver = async () => {
    if (!photographe) return;
    setStatutError(null);
    setChangingStatut(true);
    try {
      const active = await apiActiverPhotographe(photographe.id);
      setPhotographe(active);
      onStatutChanged(active);
    } catch (err) {
      setStatutError(err);
    } finally {
      setChangingStatut(false);
    }
  };

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
        <div className="flex items-center gap-1">
          {photographe && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="cursor-pointer flex items-center gap-1.5"
              onClick={() => onEdit(photographe)}
            >
              <Pencil className="w-3.5 h-3.5" />
              Modifier
            </Button>
          )}
          {photographe && (
            <Button
              type="button"
              variant="destructive"
              size="sm"
              className="cursor-pointer flex items-center gap-1.5"
              onClick={() => {
                setDeleteError(null);
                setConfirmDelete(true);
              }}
            >
              <Trash2 className="w-3.5 h-3.5" />
              Supprimer
            </Button>
          )}
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
        </div>
      </CardHeader>
      <CardContent className="space-y-4 text-sm">
        <ErrorAlert
          error={deleteError}
          title="Suppression impossible"
          onDismiss={() => setDeleteError(null)}
        />
        <ErrorAlert
          error={statutError}
          title="Changement de statut impossible"
          onDismiss={() => setStatutError(null)}
        />
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
                <div className="flex items-center gap-2">
                  <StatutBadge actif={photographe.actif} />
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="cursor-pointer flex items-center gap-1.5"
                    disabled={changingStatut}
                    onClick={photographe.actif ? handleDesactiver : handleActiver}
                  >
                    {changingStatut ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : photographe.actif ? (
                      <PowerOff className="w-3.5 h-3.5" />
                    ) : (
                      <Power className="w-3.5 h-3.5" />
                    )}
                    {photographe.actif ? "Désactiver" : "Activer"}
                  </Button>
                </div>
              </div>
              <div className="space-y-1.5">
                <div className="text-xs font-medium text-muted-foreground">Spécialités</div>
                <SpecialitesBadges specialites={photographe.specialites} />
              </div>
            </>
          )
        )}
      </CardContent>
      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="Supprimer ce photographe ?"
        description={
          photographe
            ? `${photographe.nom} sera définitivement supprimé. Cette action est irréversible.`
            : undefined
        }
        confirmLabel="Supprimer"
        variant="destructive"
        isLoading={deleting}
        onConfirm={handleDelete}
      />
      <ConfirmDialog
        open={seancesAAnnuler !== null}
        onOpenChange={(open) => {
          if (!open) setSeancesAAnnuler(null);
        }}
        title="Désactiver ce photographe ?"
        description={
          photographe && seancesAAnnuler
            ? `${photographe.nom} a ${seancesAAnnuler.length} séance(s) à venir. Elles seront toutes annulées.`
            : undefined
        }
        confirmLabel="Désactiver et annuler les séances"
        variant="destructive"
        isLoading={changingStatut}
        onConfirm={handleConfirmDesactiver}
      >
        {seancesAAnnuler && <SeancesAAnnulerListe seances={seancesAAnnuler} />}
      </ConfirmDialog>
    </Card>
  );
}

export function PhotographesPage() {
  const [filtre, setFiltre] = useState<FiltreActif>("tous");
  const [photographes, setPhotographes] = useState<Photographe[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [panneau, setPanneau] = useState<Panneau | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const { success } = useToast();

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
  }, [filtre, reloadKey]);

  const selectedId = panneau?.mode === "detail" ? panneau.id : null;

  const handleSaved = (saved: Photographe, creation: boolean) => {
    success(
      creation ? "Photographe créé" : "Photographe modifié",
      `${saved.nom} a bien été ${creation ? "créé" : "modifié"}.`
    );
    setReloadKey((k) => k + 1);
    setPanneau({ mode: "detail", id: saved.id });
  };

  const handleStatutChanged = (photographe: Photographe) => {
    success(
      photographe.actif ? "Photographe activé" : "Photographe désactivé",
      photographe.actif
        ? `${photographe.nom} peut de nouveau être assigné à des séances.`
        : `${photographe.nom} a été désactivé.`
    );
    setReloadKey((k) => k + 1);
  };

  const handleDeleted = (supprime: Photographe) => {
    success("Photographe supprimé", `${supprime.nom} a bien été supprimé.`);
    setReloadKey((k) => k + 1);
    setPanneau(null);
  };

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

        <div className="flex flex-wrap items-center gap-3">
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
                  // Un formulaire en cours de saisie reste ouvert
                  setPanneau((courant) => (courant?.mode === "detail" ? null : courant));
                }}
              >
                {label}
              </Button>
            ))}
          </div>
          <Button
            type="button"
            size="sm"
            className="cursor-pointer flex items-center gap-1.5"
            onClick={() => setPanneau({ mode: "creation" })}
          >
            <Plus className="w-3.5 h-3.5" />
            Nouveau photographe
          </Button>
        </div>
      </div>

      <ErrorAlert error={loadError} />

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className={panneau !== null ? "lg:col-span-2" : "lg:col-span-3"}>
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
                          onClick={() => setPanneau({ mode: "detail", id: p.id })}
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

        {panneau?.mode === "detail" && (
          <PhotographeDetail
            id={panneau.id}
            onEdit={(photographe) => setPanneau({ mode: "edition", photographe })}
            onDeleted={handleDeleted}
            onStatutChanged={handleStatutChanged}
            onClose={() => setPanneau(null)}
          />
        )}
        {panneau?.mode === "creation" && (
          <PhotographeForm
            key="creation"
            onSaved={(saved) => handleSaved(saved, true)}
            onCancel={() => setPanneau(null)}
          />
        )}
        {panneau?.mode === "edition" && (
          <PhotographeForm
            key={`edition-${panneau.photographe.id}`}
            photographe={panneau.photographe}
            onSaved={(saved) => handleSaved(saved, false)}
            onCancel={() => setPanneau({ mode: "detail", id: panneau.photographe.id })}
          />
        )}
      </div>
    </div>
  );
}
