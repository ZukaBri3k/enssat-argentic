import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { FileText } from "lucide-react";

export function FacturesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2.5">
          <FileText className="w-6 h-6 text-primary" />
          <span>Factures</span>
        </h1>
        <p className="text-sm text-muted-foreground mt-1">
          Génération des factures pour les séances terminées, suivi des paiements et export PDF.
        </p>
      </div>

      <Card className="border-dashed">
        <CardHeader className="text-center items-center py-12">
          <div className="p-3 rounded-2xl bg-muted text-muted-foreground mb-2">
            <FileText className="w-8 h-8" />
          </div>
          <CardTitle>Rubrique Factures</CardTitle>
          <CardDescription className="max-w-md">
            Cette rubrique accueillera la liste des factures, la génération (HT + TVA), le passage
            à PAYEE ou ANNULEE et le téléchargement PDF.
          </CardDescription>
        </CardHeader>
      </Card>
    </div>
  );
}
