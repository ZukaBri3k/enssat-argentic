import { useEffect, useState } from "react";
import { apiGetStatsPing } from "@/lib/api";
import { ErrorAlert } from "@/components/shared/ErrorAlert";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { BarChart3, CheckCircle2, ShieldCheck } from "lucide-react";

export function StatistiquesPage() {
  const [pingStatus, setPingStatus] = useState<string | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let mounted = true;
    async function verifyStatsAccess() {
      try {
        const res = await apiGetStatsPing();
        if (mounted) {
          setPingStatus(res.status);
        }
      } catch (err) {
        if (mounted) {
          setError(err);
        }
      }
    }
    verifyStatsAccess();
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2.5">
            <BarChart3 className="w-6 h-6 text-primary" />
            <span>Statistiques</span>
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Indicateurs de performance du studio réservés au rôle GESTIONNAIRE.
          </p>
        </div>

        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-purple-100 text-purple-800 dark:bg-purple-950 dark:text-purple-300 border border-purple-200 dark:border-purple-800 w-fit">
          <ShieldCheck className="w-3.5 h-3.5" />
          Accès réservé GESTIONNAIRE
        </span>
      </div>

      <ErrorAlert error={error} />

      {pingStatus && (
        <Card>
          <CardContent className="py-3 flex items-center gap-2 text-xs text-green-700 dark:text-green-400">
            <CheckCircle2 className="w-4 h-4 shrink-0" />
            <span>
              Vérification d'accès API <code className="font-mono">GET /stats/ping</code> réussie (statut :{" "}
              <span className="font-semibold">{pingStatus}</span>)
            </span>
          </CardContent>
        </Card>
      )}

      <Card className="border-dashed">
        <CardHeader className="text-center items-center py-12">
          <div className="p-3 rounded-2xl bg-muted text-muted-foreground mb-2">
            <BarChart3 className="w-8 h-8" />
          </div>
          <CardTitle>Rubrique Statistiques</CardTitle>
          <CardDescription className="max-w-md">
            Cette rubrique accueillera le Top 5 des photographes par nombre de séances terminées, le
            Top 5 par durée cumulée et le chiffre d'affaires HT/TTC sur une période.
          </CardDescription>
        </CardHeader>
      </Card>
    </div>
  );
}
