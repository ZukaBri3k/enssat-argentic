import React, { useCallback, useEffect, useState } from "react";
import {
  apiGetChiffreAffaires,
  apiGetTopPhotographesDuree,
  apiGetTopPhotographesSeances,
  type ChiffreAffairesResponse,
  type TopPhotographeDuree,
  type TopPhotographeSeances,
} from "@/lib/api";
import { ErrorAlert } from "@/components/shared/ErrorAlert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Award,
  BarChart3,
  Calendar,
  Camera,
  Clock,
  Euro,
  Loader2,
  RefreshCw,
  ShieldCheck,
  TrendingUp,
} from "lucide-react";

function formatDateInput(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatCurrency(value: number | undefined | null): string {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "EUR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value ?? 0);
}

function formatDuration(minutes: number): string {
  if (!minutes || minutes <= 0) return "0 min";
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  if (hours === 0) return `${remainingMinutes} min`;
  if (remainingMinutes === 0) return `${hours} h`;
  return `${hours} h ${String(remainingMinutes).padStart(2, "0")} min`;
}

function getDureeMinutes(item: TopPhotographeDuree): number {
  return (
    item.dureeTotaleMinutes ??
    item.dureeMinutes ??
    item.dureeCumulee ??
    item.duree ??
    0
  );
}

function getMontantHt(ca: ChiffreAffairesResponse | null): number {
  if (!ca) return 0;
  return ca.montantHt ?? ca.chiffreAffairesHt ?? ca.ht ?? 0;
}

function getMontantTtc(ca: ChiffreAffairesResponse | null): number {
  if (!ca) return 0;
  return ca.montantTtc ?? ca.chiffreAffairesTtc ?? ca.ttc ?? 0;
}

function getRankBadgeClass(index: number): string {
  if (index === 0) {
    return "bg-amber-100 text-amber-800 border-amber-300 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800";
  }
  if (index === 1) {
    return "bg-slate-100 text-slate-700 border-slate-300 dark:bg-slate-800 dark:text-slate-200 dark:border-slate-700";
  }
  if (index === 2) {
    return "bg-orange-100 text-orange-800 border-orange-300 dark:bg-orange-950/60 dark:text-orange-300 dark:border-orange-800";
  }
  return "bg-muted text-muted-foreground border-border";
}

export function StatistiquesPage() {
  // Période par défaut : 180 derniers jours à aujourd'hui (couvre toutes les factures du seed)
  const [du, setDu] = useState<string>(() => {
    const d = new Date();
    d.setDate(d.getDate() - 180);
    return formatDateInput(d);
  });
  const [au, setAu] = useState<string>(() => formatDateInput(new Date()));

  const [topSeances, setTopSeances] = useState<TopPhotographeSeances[]>([]);
  const [topDuree, setTopDuree] = useState<TopPhotographeDuree[]>([]);
  const [loadingRankings, setLoadingRankings] = useState<boolean>(true);
  const [rankingsError, setRankingsError] = useState<unknown>(null);

  const [caData, setCaData] = useState<ChiffreAffairesResponse | null>(null);
  const [loadingCa, setLoadingCa] = useState<boolean>(true);
  const [caError, setCaError] = useState<unknown>(null);

  const fetchRankings = useCallback(async () => {
    setLoadingRankings(true);
    setRankingsError(null);
    try {
      const [seancesRes, dureeRes] = await Promise.all([
        apiGetTopPhotographesSeances(),
        apiGetTopPhotographesDuree(),
      ]);
      setTopSeances(seancesRes);
      setTopDuree(dureeRes);
    } catch (err) {
      setRankingsError(err);
    } finally {
      setLoadingRankings(false);
    }
  }, []);

  const fetchChiffreAffaires = useCallback(async (dateDu: string, dateAu: string) => {
    setLoadingCa(true);
    setCaError(null);
    try {
      const res = await apiGetChiffreAffaires(dateDu, dateAu);
      setCaData(res);
    } catch (err) {
      setCaError(err);
      setCaData(null);
    } finally {
      setLoadingCa(false);
    }
  }, []);

  useEffect(() => {
    void fetchRankings();
    void fetchChiffreAffaires(du, au);
    // Chargement initial uniquement
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleCaSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    void fetchChiffreAffaires(du, au);
  };

  const applyPresetPeriod = (daysBack: number | "year") => {
    const end = new Date();
    const start = new Date();
    if (daysBack === "year") {
      start.setMonth(0, 1);
    } else {
      start.setDate(end.getDate() - daysBack);
    }
    const nextDu = formatDateInput(start);
    const nextAu = formatDateInput(end);
    setDu(nextDu);
    setAu(nextAu);
    void fetchChiffreAffaires(nextDu, nextAu);
  };

  const maxSeances = Math.max(...topSeances.map((p) => p.nombreSeances || 0), 1);
  const maxDuree = Math.max(...topDuree.map((p) => getDureeMinutes(p)), 1);

  const montantHt = getMontantHt(caData);
  const montantTtc = getMontantTtc(caData);
  const montantTva = Math.max(0, montantTtc - montantHt);

  return (
    <div className="space-y-6">
      {/* En-tête */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2.5">
            <BarChart3 className="w-6 h-6 text-primary" />
            <span>Statistiques du studio</span>
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Classements des photographes sur les séances terminées et chiffre d'affaires encaissé.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-purple-100 text-purple-800 dark:bg-purple-950 dark:text-purple-300 border border-purple-200 dark:border-purple-800">
            <ShieldCheck className="w-3.5 h-3.5" />
            GESTIONNAIRE
          </span>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="cursor-pointer gap-1.5"
            onClick={() => {
              void fetchRankings();
              void fetchChiffreAffaires(du, au);
            }}
            disabled={loadingRankings || loadingCa}
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loadingRankings || loadingCa ? "animate-spin" : ""}`} />
            <span>Actualiser</span>
          </Button>
        </div>
      </div>

      {/* Section 1 : Chiffre d'affaires sur une période */}
      <Card>
        <CardHeader className="pb-4">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
            <div>
              <CardTitle className="flex items-center gap-2 text-lg">
                <Euro className="w-5 h-5 text-primary" />
                <span>Chiffre d'affaires sur une période</span>
              </CardTitle>
              <CardDescription>
                Somme des factures au statut <strong>PAYEE</strong> dont la date de paiement est comprise dans la période sélectionnée.
              </CardDescription>
            </div>

            {/* Raccourcis de période */}
            <div className="flex flex-wrap items-center gap-1.5">
              <Button
                type="button"
                variant="outline"
                size="xs"
                className="cursor-pointer"
                onClick={() => applyPresetPeriod(30)}
              >
                30 jours
              </Button>
              <Button
                type="button"
                variant="outline"
                size="xs"
                className="cursor-pointer"
                onClick={() => applyPresetPeriod(90)}
              >
                90 jours
              </Button>
              <Button
                type="button"
                variant="outline"
                size="xs"
                className="cursor-pointer"
                onClick={() => applyPresetPeriod(180)}
              >
                6 mois
              </Button>
              <Button
                type="button"
                variant="outline"
                size="xs"
                className="cursor-pointer"
                onClick={() => applyPresetPeriod("year")}
              >
                Année en cours
              </Button>
            </div>
          </div>
        </CardHeader>

        <CardContent className="space-y-5">
          <form
            onSubmit={handleCaSubmit}
            className="grid grid-cols-1 sm:grid-cols-3 gap-4 items-end p-4 rounded-xl bg-muted/40 border border-border"
          >
            <div className="space-y-1.5">
              <Label htmlFor="stats-du" className="flex items-center gap-1.5 text-xs">
                <Calendar className="w-3.5 h-3.5 text-muted-foreground" />
                Date de début (du)
              </Label>
              <Input
                id="stats-du"
                type="date"
                value={du}
                onChange={(e) => setDu(e.target.value)}
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="stats-au" className="flex items-center gap-1.5 text-xs">
                <Calendar className="w-3.5 h-3.5 text-muted-foreground" />
                Date de fin (au)
              </Label>
              <Input
                id="stats-au"
                type="date"
                value={au}
                onChange={(e) => setAu(e.target.value)}
                required
              />
            </div>

            <Button type="submit" className="cursor-pointer w-full gap-2" disabled={loadingCa}>
              {loadingCa ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Calcul en cours...</span>
                </>
              ) : (
                <>
                  <TrendingUp className="w-4 h-4" />
                  <span>Calculer le CA</span>
                </>
              )}
            </Button>
          </form>

          <ErrorAlert error={caError} onDismiss={() => setCaError(null)} />

          {/* Cartes de résultat HT / TTC */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-4 rounded-xl border border-border bg-card shadow-xs space-y-1">
              <div className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                Chiffre d'affaires (HT)
              </div>
              <div className="text-2xl font-bold tracking-tight text-foreground">
                {loadingCa ? "—" : formatCurrency(montantHt)}
              </div>
              <div className="text-xs text-muted-foreground">
                Hors taxes sur la période sélectionnée
              </div>
            </div>

            <div className="p-4 rounded-xl border border-primary/25 bg-primary/5 shadow-xs space-y-1">
              <div className="text-xs font-medium uppercase tracking-wider text-primary">
                Total encaissé (TTC)
              </div>
              <div className="text-2xl font-bold tracking-tight text-foreground">
                {loadingCa ? "—" : formatCurrency(montantTtc)}
              </div>
              <div className="text-xs text-muted-foreground">
                Toutes taxes comprises
              </div>
            </div>

            <div className="p-4 rounded-xl border border-border bg-muted/30 space-y-1">
              <div className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                TVA collectée
              </div>
              <div className="text-2xl font-semibold tracking-tight text-muted-foreground">
                {loadingCa ? "—" : formatCurrency(montantTva)}
              </div>
              <div className="text-xs text-muted-foreground">
                Différence TTC − HT
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Erreur éventuelle sur les classements */}
      <ErrorAlert error={rankingsError} onDismiss={() => setRankingsError(null)} />

      {/* Section 2 : Les deux classements Top 5 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Classement 1 : Top 5 par nombre de séances TERMINEE */}
        <Card className="flex flex-col">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2 text-base">
                <Camera className="w-4 h-4 text-primary" />
                <span>Top 5 Photographes — Séances réalisées</span>
              </CardTitle>
              <span className="text-xs font-mono text-muted-foreground bg-muted px-2 py-0.5 rounded">
                TERMINEE
              </span>
            </div>
            <CardDescription>
              Classement par nombre total de séances terminées.
            </CardDescription>
          </CardHeader>

          <CardContent className="flex-1">
            {loadingRankings ? (
              <div className="py-12 flex flex-col items-center justify-center gap-2 text-muted-foreground text-sm">
                <Loader2 className="w-5 h-5 animate-spin" />
                <span>Chargement du classement...</span>
              </div>
            ) : topSeances.length === 0 ? (
              <div className="py-10 text-center text-sm text-muted-foreground border border-dashed rounded-xl">
                Aucune séance terminée enregistrée.
              </div>
            ) : (
              <div className="space-y-3">
                {topSeances.map((item, idx) => {
                  const pct = Math.round(((item.nombreSeances || 0) / maxSeances) * 100);
                  return (
                    <div
                      key={item.photographeId ?? item.id ?? item.nom}
                      className="p-3 rounded-xl border border-border bg-card hover:bg-muted/30 transition-colors space-y-2"
                    >
                      <div className="flex items-center justify-between gap-3">
                        <div className="flex items-center gap-3 min-w-0">
                          <span
                            className={`w-7 h-7 rounded-lg border text-xs font-bold flex items-center justify-center shrink-0 ${getRankBadgeClass(idx)}`}
                          >
                            #{idx + 1}
                          </span>
                          <div className="min-w-0">
                            <div className="font-medium text-sm text-foreground truncate flex items-center gap-1.5">
                              <span>{item.nom}</span>
                              {idx === 0 && <Award className="w-3.5 h-3.5 text-amber-500 shrink-0" />}
                            </div>
                            {item.email && (
                              <div className="text-xs text-muted-foreground truncate">
                                {item.email}
                              </div>
                            )}
                          </div>
                        </div>

                        <div className="text-right shrink-0">
                          <span className="text-base font-bold text-foreground">
                            {item.nombreSeances}
                          </span>{" "}
                          <span className="text-xs text-muted-foreground">
                            séance{item.nombreSeances > 1 ? "s" : ""}
                          </span>
                        </div>
                      </div>

                      <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
                        <div
                          className="h-full rounded-full bg-primary transition-all duration-300"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Classement 2 : Top 5 par durée cumulée des séances TERMINEE */}
        <Card className="flex flex-col">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2 text-base">
                <Clock className="w-4 h-4 text-primary" />
                <span>Top 5 Photographes — Durée cumulée</span>
              </CardTitle>
              <span className="text-xs font-mono text-muted-foreground bg-muted px-2 py-0.5 rounded">
                TERMINEE
              </span>
            </div>
            <CardDescription>
              Classement par durée totale des séances terminées (en heures et minutes).
            </CardDescription>
          </CardHeader>

          <CardContent className="flex-1">
            {loadingRankings ? (
              <div className="py-12 flex flex-col items-center justify-center gap-2 text-muted-foreground text-sm">
                <Loader2 className="w-5 h-5 animate-spin" />
                <span>Chargement du classement...</span>
              </div>
            ) : topDuree.length === 0 ? (
              <div className="py-10 text-center text-sm text-muted-foreground border border-dashed rounded-xl">
                Aucune séance terminée enregistrée.
              </div>
            ) : (
              <div className="space-y-3">
                {topDuree.map((item, idx) => {
                  const minutes = getDureeMinutes(item);
                  const pct = Math.round((minutes / maxDuree) * 100);
                  return (
                    <div
                      key={item.photographeId ?? item.id ?? item.nom}
                      className="p-3 rounded-xl border border-border bg-card hover:bg-muted/30 transition-colors space-y-2"
                    >
                      <div className="flex items-center justify-between gap-3">
                        <div className="flex items-center gap-3 min-w-0">
                          <span
                            className={`w-7 h-7 rounded-lg border text-xs font-bold flex items-center justify-center shrink-0 ${getRankBadgeClass(idx)}`}
                          >
                            #{idx + 1}
                          </span>
                          <div className="min-w-0">
                            <div className="font-medium text-sm text-foreground truncate flex items-center gap-1.5">
                              <span>{item.nom}</span>
                              {idx === 0 && <Award className="w-3.5 h-3.5 text-amber-500 shrink-0" />}
                            </div>
                            {item.email && (
                              <div className="text-xs text-muted-foreground truncate">
                                {item.email}
                              </div>
                            )}
                          </div>
                        </div>

                        <div className="text-right shrink-0">
                          <div className="text-sm font-bold text-foreground">
                            {formatDuration(minutes)}
                          </div>
                          <div className="text-[11px] text-muted-foreground">
                            {minutes} min
                          </div>
                        </div>
                      </div>

                      <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
                        <div
                          className="h-full rounded-full bg-primary transition-all duration-300"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
