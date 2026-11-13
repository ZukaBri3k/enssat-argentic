import { useEffect, useState } from "react";
import { apiGetSpecialites, type Specialite } from "@/lib/api";
import { useToast } from "@/components/shared/ToastProvider";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { ErrorAlert } from "@/components/shared/ErrorAlert";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { UserSquare2, Tag, Bell, ShieldAlert } from "lucide-react";

export function PhotographesPage() {
  const { success, error: toastError, showApiError } = useToast();
  const [specialites, setSpecialites] = useState<Specialite[]>([]);
  const [loadingSpecs, setLoadingSpecs] = useState(true);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [demoModalOpen, setDemoModalOpen] = useState(false);

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoadingSpecs(true);
        const data = await apiGetSpecialites();
        if (mounted) {
          setSpecialites(data);
        }
      } catch (err) {
        if (mounted) {
          setLoadError(err);
          showApiError(err, "Impossible de charger les spécialités");
        }
      } finally {
        if (mounted) {
          setLoadingSpecs(false);
        }
      }
    }
    load();
    return () => {
      mounted = false;
    };
  }, [showApiError]);

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

        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="cursor-pointer flex items-center gap-1.5"
            onClick={() =>
              success("Notification Toast", "Le système de notifications partagé fonctionne !")
            }
          >
            <Bell className="w-3.5 h-3.5" />
            Tester Toast
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="cursor-pointer flex items-center gap-1.5"
            onClick={() => setDemoModalOpen(true)}
          >
            <ShieldAlert className="w-3.5 h-3.5" />
            Tester Modale
          </Button>
        </div>
      </div>

      <ErrorAlert error={loadError} />

      {/* Spécialités disponibles chargées depuis GET /specialites */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Tag className="w-4 h-4 text-primary" />
            <span>Spécialités du studio (GET /specialites)</span>
          </CardTitle>
          <CardDescription>
            Liste fixe en lecture seule récupérée depuis le backend avec le token Bearer.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loadingSpecs ? (
            <div className="text-xs text-muted-foreground">Chargement des spécialités...</div>
          ) : specialites.length === 0 ? (
            <div className="text-xs text-muted-foreground">Aucune spécialité trouvée.</div>
          ) : (
            <div className="flex flex-wrap gap-2">
              {specialites.map((spec) => (
                <span
                  key={spec.id}
                  className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-secondary text-secondary-foreground border border-border"
                >
                  <span className="text-muted-foreground font-mono">#{spec.id}</span>
                  <span>{spec.libelle}</span>
                </span>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Squelette de la rubrique Photographes */}
      <Card className="border-dashed">
        <CardHeader className="text-center items-center py-10">
          <div className="p-3 rounded-2xl bg-muted text-muted-foreground mb-2">
            <UserSquare2 className="w-8 h-8" />
          </div>
          <CardTitle>Rubrique Photographes</CardTitle>
          <CardDescription className="max-w-md">
            Cette rubrique accueillera la liste des photographes, la création/modification et la
            désactivation avec annulation des séances à venir.
          </CardDescription>
        </CardHeader>
      </Card>

      {/* Modale de démonstration du composant partagé ConfirmDialog */}
      <ConfirmDialog
        open={demoModalOpen}
        onOpenChange={setDemoModalOpen}
        title="Désactiver ce photographe ?"
        description="Exemple de modale de confirmation partagée : la désactivation annulera automatiquement les séances planifiées ou confirmées à venir."
        confirmLabel="Confirmer la désactivation"
        cancelLabel="Annuler"
        variant="destructive"
        onConfirm={() => {
          setDemoModalOpen(false);
          toastError(
            "Action simulée",
            "Exemple d'erreur ProblemDetail avec détails par champ",
            { specialites: "Au moins une spécialité est requise" }
          );
        }}
      />
    </div>
  );
}
