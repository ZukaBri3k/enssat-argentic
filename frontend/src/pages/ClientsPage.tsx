import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Users } from "lucide-react";

export function ClientsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2.5">
          <Users className="w-6 h-6 text-primary" />
          <span>Clients</span>
        </h1>
        <p className="text-sm text-muted-foreground mt-1">
          Gestion du carnet clients : particuliers et entreprises.
        </p>
      </div>

      <Card className="border-dashed">
        <CardHeader className="text-center items-center py-12">
          <div className="p-3 rounded-2xl bg-muted text-muted-foreground mb-2">
            <Users className="w-8 h-8" />
          </div>
          <CardTitle>Rubrique Clients</CardTitle>
          <CardDescription className="max-w-md">
            Cette rubrique accueillera la liste des clients (Particuliers et Entreprises), la
            création, la modification et la suppression.
          </CardDescription>
        </CardHeader>
      </Card>
    </div>
  );
}
