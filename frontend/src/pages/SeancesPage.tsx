import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Calendar } from "lucide-react";

export function SeancesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2.5">
          <Calendar className="w-6 h-6 text-primary" />
          <span>Séances / Planning</span>
        </h1>
        <p className="text-sm text-muted-foreground mt-1">
          Planification des séances photo, vérification des disponibilités et suivi des statuts.
        </p>
      </div>

      <Card className="border-dashed">
        <CardHeader className="text-center items-center py-12">
          <div className="p-3 rounded-2xl bg-muted text-muted-foreground mb-2">
            <Calendar className="w-8 h-8" />
          </div>
          <CardTitle>Rubrique Séances & Planning</CardTitle>
          <CardDescription className="max-w-md">
            Cette rubrique accueillera le planning des séances, le contrôle de non-chevauchement et
            les transitions de statut (PLANIFIEE, CONFIRMEE, TERMINEE, ANNULEE).
          </CardDescription>
        </CardHeader>
      </Card>
    </div>
  );
}
