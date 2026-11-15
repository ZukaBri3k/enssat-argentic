import { useCallback, useEffect, useMemo, useState } from "react";
import {
  apiGetClients,
  apiGetPhotographes,
  apiGetSeanceById,
  apiGetSeances,
  type ClientSummary,
  type FactureSummary,
  type PhotographeSummary,
  type SeanceResponse,
  type StatutPaiement,
  type StatutSeance,
} from "@/lib/api";
import { ErrorAlert } from "@/components/shared/ErrorAlert";
import { useToast } from "@/components/shared/ToastProvider";
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
  Calendar,
  CalendarDays,
  Camera,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock,
  Eye,
  FileText,
  Filter,
  LayoutGrid,
  List,
  Loader2,
  MapPin,
  RefreshCw,
  RotateCcw,
  Tag,
  User,
  Users,
  X,
  XCircle,
} from "lucide-react";

type ViewMode = "semaine" | "jour" | "liste";

interface StatutStyle {
  label: string;
  description: string;
  badgeClass: string;
  cardClass: string;
  dotClass: string;
  pillActiveClass: string;
}

const STATUT_STYLES: Record<StatutSeance, StatutStyle> = {
  PLANIFIEE: {
    label: "Planifiée",
    description: "Séance programmée, en attente de confirmation",
    badgeClass:
      "bg-blue-100 text-blue-800 border-blue-300 dark:bg-blue-950/70 dark:text-blue-300 dark:border-blue-800",
    cardClass:
      "border-l-4 border-l-blue-500 bg-blue-50/60 hover:bg-blue-100/60 dark:bg-blue-950/25 dark:hover:bg-blue-950/45 border-blue-200/80 dark:border-blue-900/60",
    dotClass: "bg-blue-500",
    pillActiveClass:
      "bg-blue-600 text-white border-blue-600 shadow-xs hover:bg-blue-700",
  },
  CONFIRMEE: {
    label: "Confirmée",
    description: "Séance confirmée avec le client",
    badgeClass:
      "bg-emerald-100 text-emerald-800 border-emerald-300 dark:bg-emerald-950/70 dark:text-emerald-300 dark:border-emerald-800",
    cardClass:
      "border-l-4 border-l-emerald-500 bg-emerald-50/60 hover:bg-emerald-100/60 dark:bg-emerald-950/25 dark:hover:bg-emerald-950/45 border-emerald-200/80 dark:border-emerald-900/60",
    dotClass: "bg-emerald-500",
    pillActiveClass:
      "bg-emerald-600 text-white border-emerald-600 shadow-xs hover:bg-emerald-700",
  },
  TERMINEE: {
    label: "Terminée",
    description: "Séance réalisée (statut final, facturable)",
    badgeClass:
      "bg-purple-100 text-purple-800 border-purple-300 dark:bg-purple-950/70 dark:text-purple-300 dark:border-purple-800",
    cardClass:
      "border-l-4 border-l-purple-500 bg-purple-50/60 hover:bg-purple-100/60 dark:bg-purple-950/25 dark:hover:bg-purple-950/45 border-purple-200/80 dark:border-purple-900/60",
    dotClass: "bg-purple-500",
    pillActiveClass:
      "bg-purple-600 text-white border-purple-600 shadow-xs hover:bg-purple-700",
  },
  ANNULEE: {
    label: "Annulée",
    description: "Séance annulée (statut final, ignorée dans les chevauchements)",
    badgeClass:
      "bg-rose-100 text-rose-800 border-rose-300 dark:bg-rose-950/70 dark:text-rose-300 dark:border-rose-800",
    cardClass:
      "border-l-4 border-l-rose-500 bg-rose-50/50 hover:bg-rose-100/50 dark:bg-rose-950/20 dark:hover:bg-rose-950/40 border-rose-200/80 dark:border-rose-900/60 opacity-80",
    dotClass: "bg-rose-500",
    pillActiveClass:
      "bg-rose-600 text-white border-rose-600 shadow-xs hover:bg-rose-700",
  },
};

const ALL_STATUTS: StatutSeance[] = [
  "PLANIFIEE",
  "CONFIRMEE",
  "TERMINEE",
  "ANNULEE",
];

const STATUT_PAIEMENT_BADGE: Record<
  StatutPaiement,
  { label: string; className: string }
> = {
  EN_ATTENTE: {
    label: "En attente",
    className:
      "bg-amber-100 text-amber-800 border-amber-300 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800",
  },
  PAYEE: {
    label: "Payée",
    className:
      "bg-emerald-100 text-emerald-800 border-emerald-300 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800",
  },
  ANNULEE: {
    label: "Annulée",
    className:
      "bg-rose-100 text-rose-800 border-rose-300 dark:bg-rose-950/60 dark:text-rose-300 dark:border-rose-800",
  },
};

const HOURS_TIMELINE = Array.from({ length: 13 }, (_, i) => i + 8); // 08:00 -> 20:00

// --- Fonctions utilitaires de dates et normalisation ---

function toDateInputString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function parseLocalDate(dateStr: string): Date {
  const [y, m, d] = dateStr.split("-").map(Number);
  return new Date(y, (m || 1) - 1, d || 1);
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  next.setDate(next.getDate() + days);
  return next;
}

function getMondayOfWeek(date: Date): Date {
  const copy = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const day = copy.getDay(); // 0 = Dimanche, 1 = Lundi, ...
  const diff = day === 0 ? -6 : 1 - day;
  copy.setDate(copy.getDate() + diff);
  return copy;
}

function formatDateLongFr(dateStr: string): string {
  if (!dateStr) return "—";
  const d = parseLocalDate(dateStr);
  return new Intl.DateTimeFormat("fr-FR", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(d);
}

function formatDateShortFr(dateStr: string): string {
  if (!dateStr) return "—";
  const d = parseLocalDate(dateStr);
  return new Intl.DateTimeFormat("fr-FR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(d);
}

function formatWeekRangeFr(monday: Date): string {
  const sunday = addDays(monday, 6);
  const sameMonth = monday.getMonth() === sunday.getMonth();
  const startFmt = new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: sameMonth ? undefined : "short",
  }).format(monday);
  const endFmt = new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(sunday);
  return `${startFmt} – ${endFmt}`;
}

function formatDuration(minutes: number): string {
  if (!minutes || minutes <= 0) return "0 min";
  const hours = Math.floor(minutes / 60);
  const rem = minutes % 60;
  if (hours === 0) return `${rem} min`;
  if (rem === 0) return `${hours} h`;
  return `${hours} h ${String(rem).padStart(2, "0")}`;
}

function formatCurrency(value: number | undefined | null): string {
  if (value === undefined || value === null) return "—";
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "EUR",
  }).format(value);
}

function extractSeanceDate(s: SeanceResponse): string {
  if (s.date) return s.date.slice(0, 10);
  if (s.dateSeance) return s.dateSeance.slice(0, 10);
  if (s.debut && s.debut.includes("T")) return s.debut.split("T")[0];
  return "";
}

function extractHeureDebut(s: SeanceResponse): string {
  if (s.heureDebut) return s.heureDebut.slice(0, 5);
  if (s.debut && s.debut.includes("T")) {
    return s.debut.split("T")[1].slice(0, 5);
  }
  return "00:00";
}

function extractDureeMinutes(s: SeanceResponse): number {
  return s.dureeMinutes ?? s.duree ?? 0;
}

function timeToMinutes(hhmm: string): number {
  const [h, m] = hhmm.split(":").map(Number);
  return (h || 0) * 60 + (m || 0);
}

function minutesToTime(totalMinutes: number): string {
  const clamped = Math.max(0, Math.min(24 * 60, totalMinutes));
  const h = Math.floor(clamped / 60);
  const m = clamped % 60;
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
}

function extractHeureFin(s: SeanceResponse): string {
  const rawFin = s.fin || s.heureFin || s.dateFin;
  if (rawFin) {
    if (rawFin.includes("T")) {
      return rawFin.split("T")[1].slice(0, 5);
    }
    if (rawFin.length >= 5) {
      return rawFin.slice(0, 5);
    }
  }
  const startMin = timeToMinutes(extractHeureDebut(s));
  const duree = extractDureeMinutes(s);
  return minutesToTime(startMin + duree);
}

function extractFinIsoOrFormatted(s: SeanceResponse): string {
  const dateStr = extractSeanceDate(s);
  const heureFin = extractHeureFin(s);
  if (!dateStr) return heureFin;
  return `${formatDateShortFr(dateStr)} à ${heureFin}`;
}

function extractPhotographeId(s: SeanceResponse): number | undefined {
  return s.photographe?.id ?? s.photographeId;
}

function extractPhotographeNom(s: SeanceResponse): string {
  return s.photographe?.nom ?? s.photographeNom ?? "Photographe non renseigné";
}

function extractPhotographeEmail(s: SeanceResponse): string | undefined {
  return s.photographe?.email ?? s.photographeEmail;
}

function getClientDisplayName(c?: ClientSummary): string {
  if (!c) return "Client non renseigné";
  if (c.identite) return c.identite;
  if (c.raisonSociale) return c.raisonSociale;
  const fullName = [c.nom, c.prenom].filter(Boolean).join(" ").trim();
  return fullName || c.email || `Client #${c.id}`;
}

function extractClientId(s: SeanceResponse): number | undefined {
  return s.client?.id ?? s.clientId;
}

function extractClientNom(s: SeanceResponse): string {
  if (s.client) return getClientDisplayName(s.client);
  return s.clientIdentite ?? s.clientNom ?? "Client non renseigné";
}

function extractSpecialiteLibelle(s: SeanceResponse): string {
  return (
    s.specialite?.libelle ??
    s.specialiteLibelle ??
    s.typeSeance ??
    "Spécialité"
  );
}

function extractFacture(s: SeanceResponse): FactureSummary | null {
  if (s.facture) return s.facture;
  if (s.factureId || s.factureNumero) {
    return {
      id: s.factureId ?? 0,
      numero: s.factureNumero ?? `Facture #${s.factureId}`,
      statutPaiement: s.factureStatut ?? undefined,
    };
  }
  return null;
}

export function SeancesPage() {
  const { showApiError } = useToast();

  // Mode d'affichage : Semaine (défaut), Jour ou Liste
  const [viewMode, setViewMode] = useState<ViewMode>("semaine");

  // Date de référence pour les vues Semaine et Jour
  const [referenceDate, setReferenceDate] = useState<Date>(() => new Date());

  // Filtres de période pour la vue Liste (optionnels)
  const [listDu, setListDu] = useState<string>("");
  const [listAu, setListAu] = useState<string>("");

  // Filtres communs : photographe, client, statut
  const [selectedPhotographeId, setSelectedPhotographeId] = useState<string>("");
  const [selectedClientId, setSelectedClientId] = useState<string>("");
  const [selectedStatut, setSelectedStatut] = useState<StatutSeance | "">("");

  // Données chargées
  const [seances, setSeances] = useState<SeanceResponse[]>([]);
  const [loadingSeances, setLoadingSeances] = useState<boolean>(true);
  const [seancesError, setSeancesError] = useState<unknown>(null);

  const [photographesApi, setPhotographesApi] = useState<PhotographeSummary[]>([]);
  const [clientsApi, setClientsApi] = useState<ClientSummary[]>([]);

  // État pour la modale de détail d'une séance (GET /seances/{id})
  const [selectedSeanceId, setSelectedSeanceId] = useState<number | null>(null);
  const [seanceDetail, setSeanceDetail] = useState<SeanceResponse | null>(null);
  const [loadingDetail, setLoadingDetail] = useState<boolean>(false);
  const [detailError, setDetailError] = useState<unknown>(null);

  // Calcul des bornes de la semaine courante (Lundi -> Dimanche)
  const mondayOfWeek = useMemo(() => getMondayOfWeek(referenceDate), [referenceDate]);
  const sundayOfWeek = useMemo(() => addDays(mondayOfWeek, 6), [mondayOfWeek]);
  const weekDays = useMemo(
    () => Array.from({ length: 7 }, (_, i) => addDays(mondayOfWeek, i)),
    [mondayOfWeek]
  );

  // Bornes `du` / `au` effectives envoyées au backend selon la vue active
  const effectivePeriod = useMemo(() => {
    if (viewMode === "semaine") {
      return {
        du: toDateInputString(mondayOfWeek),
        au: toDateInputString(sundayOfWeek),
      };
    }
    if (viewMode === "jour") {
      const dayStr = toDateInputString(referenceDate);
      return {
        du: dayStr,
        au: dayStr,
      };
    }
    return {
      du: listDu || undefined,
      au: listAu || undefined,
    };
  }, [viewMode, mondayOfWeek, sundayOfWeek, referenceDate, listDu, listAu]);

  // Chargement optionnel des listes de photographes et clients pour enrichir les filtres
  useEffect(() => {
    let mounted = true;
    async function loadFilterReferentials() {
      const [pRes, cRes] = await Promise.allSettled([
        apiGetPhotographes(),
        apiGetClients(),
      ]);
      if (!mounted) return;
      if (pRes.status === "fulfilled" && Array.isArray(pRes.value)) {
        setPhotographesApi(pRes.value);
      }
      if (cRes.status === "fulfilled" && Array.isArray(cRes.value)) {
        setClientsApi(cRes.value);
      }
    }
    void loadFilterReferentials();
    return () => {
      mounted = false;
    };
  }, []);

  // Compteur d'actualisation manuelle
  const [refreshTick, setRefreshTick] = useState<number>(0);

  const triggerRefresh = useCallback(() => {
    setRefreshTick((t) => t + 1);
  }, []);

  // Chargement des séances depuis GET /seances avec les filtres actifs
  useEffect(() => {
    let mounted = true;
    async function loadSeances() {
      setLoadingSeances(true);
      setSeancesError(null);
      try {
        const data = await apiGetSeances({
          du: effectivePeriod.du,
          au: effectivePeriod.au,
          photographeId: selectedPhotographeId || undefined,
          clientId: selectedClientId || undefined,
          statut: selectedStatut || undefined,
        });
        if (mounted) {
          setSeances(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (mounted) {
          setSeancesError(err);
          showApiError(err, "Impossible de charger le planning des séances");
        }
      } finally {
        if (mounted) {
          setLoadingSeances(false);
        }
      }
    }
    void loadSeances();
    return () => {
      mounted = false;
    };
  }, [
    effectivePeriod.du,
    effectivePeriod.au,
    selectedPhotographeId,
    selectedClientId,
    selectedStatut,
    refreshTick,
    showApiError,
  ]);

  // Liste consolidée des photographes (API + extraits des séances chargées)
  const photographeOptions = useMemo(() => {
    const map = new Map<number, { id: number; nom: string; actif?: boolean }>();
    for (const p of photographesApi) {
      if (p.id !== undefined) {
        map.set(p.id, { id: p.id, nom: p.nom, actif: p.actif });
      }
    }
    for (const s of seances) {
      const id = extractPhotographeId(s);
      const nom = extractPhotographeNom(s);
      if (id !== undefined && !map.has(id)) {
        map.set(id, { id, nom, actif: s.photographe?.actif });
      }
    }
    return Array.from(map.values()).sort((a, b) => a.nom.localeCompare(b.nom, "fr"));
  }, [photographesApi, seances]);

  // Liste consolidée des clients (API + extraits des séances chargées)
  const clientOptions = useMemo(() => {
    const map = new Map<number, { id: number; nom: string }>();
    for (const c of clientsApi) {
      if (c.id !== undefined) {
        map.set(c.id, { id: c.id, nom: getClientDisplayName(c) });
      }
    }
    for (const s of seances) {
      const id = extractClientId(s);
      const nom = extractClientNom(s);
      if (id !== undefined && !map.has(id)) {
        map.set(id, { id, nom });
      }
    }
    return Array.from(map.values()).sort((a, b) => a.nom.localeCompare(b.nom, "fr"));
  }, [clientsApi, seances]);

  // Filtrage et tri côté client en complément du filtrage backend
  const filteredSeances = useMemo(() => {
    return seances
      .filter((s) => {
        const sDate = extractSeanceDate(s);
        if (effectivePeriod.du && sDate && sDate < effectivePeriod.du) {
          return false;
        }
        if (effectivePeriod.au && sDate && sDate > effectivePeriod.au) {
          return false;
        }
        if (
          selectedPhotographeId &&
          String(extractPhotographeId(s)) !== String(selectedPhotographeId)
        ) {
          return false;
        }
        if (
          selectedClientId &&
          String(extractClientId(s)) !== String(selectedClientId)
        ) {
          return false;
        }
        if (selectedStatut && s.statut !== selectedStatut) {
          return false;
        }
        return true;
      })
      .sort((a, b) => {
        const dateCmp = extractSeanceDate(a).localeCompare(extractSeanceDate(b));
        if (dateCmp !== 0) return dateCmp;
        return extractHeureDebut(a).localeCompare(extractHeureDebut(b));
      });
  }, [
    seances,
    effectivePeriod.du,
    effectivePeriod.au,
    selectedPhotographeId,
    selectedClientId,
    selectedStatut,
  ]);

  // Compteurs par statut sur l'ensemble des séances de la période
  const countsByStatut = useMemo(() => {
    const counts: Record<StatutSeance, number> = {
      PLANIFIEE: 0,
      CONFIRMEE: 0,
      TERMINEE: 0,
      ANNULEE: 0,
    };
    for (const s of filteredSeances) {
      if (counts[s.statut] !== undefined) {
        counts[s.statut] += 1;
      }
    }
    return counts;
  }, [filteredSeances]);

  // Ouverture du détail d'une séance via GET /seances/{id}
  const handleOpenDetail = useCallback(
    async (seancePreview: SeanceResponse) => {
      setSelectedSeanceId(seancePreview.id);
      setSeanceDetail(seancePreview);
      setLoadingDetail(true);
      setDetailError(null);
      try {
        const fullDetail = await apiGetSeanceById(seancePreview.id);
        setSeanceDetail(fullDetail);
      } catch (err) {
        setDetailError(err);
        showApiError(err, `Impossible de charger la séance #${seancePreview.id}`);
      } finally {
        setLoadingDetail(false);
      }
    },
    [showApiError]
  );

  const handleCloseDetail = useCallback(() => {
    setSelectedSeanceId(null);
    setSeanceDetail(null);
    setDetailError(null);
    setLoadingDetail(false);
  }, []);

  // Navigation temporelle
  const goToPrevious = () => {
    setReferenceDate((prev) => addDays(prev, viewMode === "semaine" ? -7 : -1));
  };

  const goToNext = () => {
    setReferenceDate((prev) => addDays(prev, viewMode === "semaine" ? 7 : 1));
  };

  const goToToday = () => {
    setReferenceDate(new Date());
  };

  const resetFilters = () => {
    setSelectedPhotographeId("");
    setSelectedClientId("");
    setSelectedStatut("");
    setListDu("");
    setListAu("");
  };

  const applyListPreset = (preset: "all" | "week" | "month" | "upcoming" | "past") => {
    const today = new Date();
    if (preset === "all") {
      setListDu("");
      setListAu("");
    } else if (preset === "week") {
      const mon = getMondayOfWeek(today);
      const sun = addDays(mon, 6);
      setListDu(toDateInputString(mon));
      setListAu(toDateInputString(sun));
    } else if (preset === "month") {
      const first = new Date(today.getFullYear(), today.getMonth(), 1);
      const last = new Date(today.getFullYear(), today.getMonth() + 1, 0);
      setListDu(toDateInputString(first));
      setListAu(toDateInputString(last));
    } else if (preset === "upcoming") {
      setListDu(toDateInputString(today));
      setListAu("");
    } else if (preset === "past") {
      setListDu("");
      setListAu(toDateInputString(today));
    }
  };

  const hasActiveFilters = Boolean(
    selectedPhotographeId ||
      selectedClientId ||
      selectedStatut ||
      (viewMode === "liste" && (listDu || listAu))
  );

  const todayStr = toDateInputString(new Date());
  const referenceDateStr = toDateInputString(referenceDate);

  return (
    <div className="space-y-6">
      {/* En-tête de la page */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2.5">
            <Calendar className="w-6 h-6 text-primary" />
            <span>Planning & Séances photo</span>
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Consultez le planning par semaine ou par jour, parcourez la liste des séances et
            inspectez le détail complet de chaque prestation.
          </p>
        </div>

        {/* Sélecteur de vue (Semaine / Jour / Liste) + bouton Actualiser */}
        <div className="flex flex-wrap items-center gap-2">
          <div
            role="tablist"
            aria-label="Mode d'affichage du planning"
            className="inline-flex items-center rounded-xl bg-muted p-1 border border-border"
          >
            <button
              type="button"
              role="tab"
              aria-selected={viewMode === "semaine"}
              onClick={() => setViewMode("semaine")}
              className={`cursor-pointer inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                viewMode === "semaine"
                  ? "bg-background text-foreground shadow-xs"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              <LayoutGrid className="w-3.5 h-3.5" />
              <span>Semaine</span>
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={viewMode === "jour"}
              onClick={() => setViewMode("jour")}
              className={`cursor-pointer inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                viewMode === "jour"
                  ? "bg-background text-foreground shadow-xs"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              <CalendarDays className="w-3.5 h-3.5" />
              <span>Jour</span>
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={viewMode === "liste"}
              onClick={() => setViewMode("liste")}
              className={`cursor-pointer inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                viewMode === "liste"
                  ? "bg-background text-foreground shadow-xs"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              <List className="w-3.5 h-3.5" />
              <span>Liste</span>
            </button>
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={triggerRefresh}
            disabled={loadingSeances}
            className="cursor-pointer gap-1.5"
          >
            <RefreshCw
              className={`w-3.5 h-3.5 ${loadingSeances ? "animate-spin" : ""}`}
            />
            <span>Actualiser</span>
          </Button>
        </div>
      </div>

      {/* Barre de filtres et légende des statuts */}
      <Card>
        <CardContent className="pt-5 space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-end">
            {/* Filtre par Photographe */}
            <div className="space-y-1.5">
              <Label
                htmlFor="filter-photographe"
                className="flex items-center gap-1.5 text-xs"
              >
                <Camera className="w-3.5 h-3.5 text-muted-foreground" />
                <span>Photographe</span>
              </Label>
              <select
                id="filter-photographe"
                value={selectedPhotographeId}
                onChange={(e) => setSelectedPhotographeId(e.target.value)}
                className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 dark:bg-input/30"
              >
                <option value="">Tous les photographes</option>
                {photographeOptions.map((p) => (
                  <option key={p.id} value={String(p.id)}>
                    {p.nom}
                    {p.actif === false ? " (inactif)" : ""}
                  </option>
                ))}
              </select>
            </div>

            {/* Filtre par Statut (menu déroulant) */}
            <div className="space-y-1.5">
              <Label
                htmlFor="filter-statut"
                className="flex items-center gap-1.5 text-xs"
              >
                <Filter className="w-3.5 h-3.5 text-muted-foreground" />
                <span>Statut de la séance</span>
              </Label>
              <select
                id="filter-statut"
                value={selectedStatut}
                onChange={(e) =>
                  setSelectedStatut(e.target.value as StatutSeance | "")
                }
                className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 dark:bg-input/30"
              >
                <option value="">Tous les statuts</option>
                {ALL_STATUTS.map((st) => (
                  <option key={st} value={st}>
                    {STATUT_STYLES[st].label} ({st})
                  </option>
                ))}
              </select>
            </div>

            {/* Filtre par Client */}
            <div className="space-y-1.5">
              <Label
                htmlFor="filter-client"
                className="flex items-center gap-1.5 text-xs"
              >
                <Users className="w-3.5 h-3.5 text-muted-foreground" />
                <span>Client</span>
              </Label>
              <select
                id="filter-client"
                value={selectedClientId}
                onChange={(e) => setSelectedClientId(e.target.value)}
                className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 dark:bg-input/30"
              >
                <option value="">Tous les clients</option>
                {clientOptions.map((c) => (
                  <option key={c.id} value={String(c.id)}>
                    {c.nom}
                  </option>
                ))}
              </select>
            </div>

            {/* Sélecteur de date de référence (Semaine/Jour) ou bouton de réinitialisation */}
            {viewMode !== "liste" ? (
              <div className="space-y-1.5">
                <Label
                  htmlFor="planning-reference-date"
                  className="flex items-center gap-1.5 text-xs"
                >
                  <Calendar className="w-3.5 h-3.5 text-muted-foreground" />
                  <span>Aller à la date</span>
                </Label>
                <Input
                  id="planning-reference-date"
                  type="date"
                  value={referenceDateStr}
                  onChange={(e) => {
                    if (e.target.value) {
                      setReferenceDate(parseLocalDate(e.target.value));
                    }
                  }}
                />
              </div>
            ) : (
              <div className="flex items-center justify-end h-8">
                {hasActiveFilters && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={resetFilters}
                    className="cursor-pointer w-full gap-1.5"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>Réinitialiser les filtres</span>
                  </Button>
                )}
              </div>
            )}
          </div>

          {/* Filtres de période spécifiques au mode Liste */}
          {viewMode === "liste" && (
            <div className="pt-2 border-t border-border flex flex-col md:flex-row md:items-end justify-between gap-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 flex-1 max-w-md">
                <div className="space-y-1">
                  <Label htmlFor="list-du" className="text-xs">
                    Période du
                  </Label>
                  <Input
                    id="list-du"
                    type="date"
                    value={listDu}
                    onChange={(e) => setListDu(e.target.value)}
                  />
                </div>
                <div className="space-y-1">
                  <Label htmlFor="list-au" className="text-xs">
                    Au
                  </Label>
                  <Input
                    id="list-au"
                    type="date"
                    value={listAu}
                    onChange={(e) => setListAu(e.target.value)}
                  />
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-1.5">
                <span className="text-xs text-muted-foreground mr-1">
                  Raccourcis :
                </span>
                <Button
                  type="button"
                  variant="outline"
                  size="xs"
                  className="cursor-pointer"
                  onClick={() => applyListPreset("all")}
                >
                  Toutes les dates
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="xs"
                  className="cursor-pointer"
                  onClick={() => applyListPreset("week")}
                >
                  Cette semaine
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="xs"
                  className="cursor-pointer"
                  onClick={() => applyListPreset("month")}
                >
                  Ce mois-ci
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="xs"
                  className="cursor-pointer"
                  onClick={() => applyListPreset("upcoming")}
                >
                  À venir
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="xs"
                  className="cursor-pointer"
                  onClick={() => applyListPreset("past")}
                >
                  Passées
                </Button>
              </div>
            </div>
          )}

          {/* Code couleur par statut (pastilles interactives + compteurs) */}
          <div className="pt-3 border-t border-border flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs font-medium text-muted-foreground mr-1">
                Code couleur statuts :
              </span>

              <button
                type="button"
                onClick={() => setSelectedStatut("")}
                className={`cursor-pointer inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border transition-all ${
                  selectedStatut === ""
                    ? "bg-foreground text-background border-foreground shadow-xs"
                    : "bg-muted/60 text-muted-foreground border-border hover:text-foreground"
                }`}
              >
                <span>Tous</span>
                <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-background/20">
                  {filteredSeances.length}
                </span>
              </button>

              {ALL_STATUTS.map((st) => {
                const style = STATUT_STYLES[st];
                const isSelected = selectedStatut === st;
                return (
                  <button
                    key={st}
                    type="button"
                    onClick={() =>
                      setSelectedStatut((prev) => (prev === st ? "" : st))
                    }
                    title={style.description}
                    className={`cursor-pointer inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border transition-all ${
                      isSelected ? style.pillActiveClass : style.badgeClass
                    }`}
                  >
                    <span
                      className={`w-2 h-2 rounded-full ${
                        isSelected ? "bg-white" : style.dotClass
                      }`}
                    />
                    <span>{style.label}</span>
                    <span className="text-[11px] opacity-80 font-mono">
                      ({countsByStatut[st]})
                    </span>
                  </button>
                );
              })}
            </div>

            {hasActiveFilters && viewMode !== "liste" && (
              <Button
                type="button"
                variant="ghost"
                size="xs"
                onClick={resetFilters}
                className="cursor-pointer gap-1 text-muted-foreground hover:text-foreground"
              >
                <RotateCcw className="w-3 h-3" />
                <span>Réinitialiser les filtres</span>
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Affichage des erreurs backend éventuelles */}
      <ErrorAlert
        error={seancesError}
        onDismiss={() => setSeancesError(null)}
      />

      {/* Barre de navigation de période pour les vues Semaine et Jour */}
      {viewMode !== "liste" && (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-card p-4 rounded-xl border border-border shadow-xs">
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="icon-sm"
              onClick={goToPrevious}
              aria-label={
                viewMode === "semaine" ? "Semaine précédente" : "Jour précédent"
              }
              className="cursor-pointer"
            >
              <ChevronLeft className="w-4 h-4" />
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={goToToday}
              className="cursor-pointer text-xs font-medium"
            >
              Aujourd'hui
            </Button>
            <Button
              type="button"
              variant="outline"
              size="icon-sm"
              onClick={goToNext}
              aria-label={
                viewMode === "semaine" ? "Semaine suivante" : "Jour suivant"
              }
              className="cursor-pointer"
            >
              <ChevronRight className="w-4 h-4" />
            </Button>
          </div>

          <div className="text-base sm:text-lg font-bold text-foreground capitalize">
            {viewMode === "semaine"
              ? `Semaine du ${formatWeekRangeFr(mondayOfWeek)}`
              : formatDateLongFr(referenceDateStr)}
          </div>

          <div className="text-xs text-muted-foreground font-medium">
            {filteredSeances.length} séance
            {filteredSeances.length > 1 ? "s" : ""} sur cette période
          </div>
        </div>
      )}

      {/* Contenu principal selon le mode sélectionné */}
      {loadingSeances ? (
        <Card>
          <CardContent className="py-16 flex flex-col items-center justify-center gap-3 text-muted-foreground">
            <Loader2 className="w-7 h-7 animate-spin text-primary" />
            <p className="text-sm font-medium">
              Chargement du planning des séances...
            </p>
          </CardContent>
        </Card>
      ) : viewMode === "semaine" ? (
        /* ==================== VUE PLANNING PAR SEMAINE ==================== */
        <div className="grid grid-cols-1 md:grid-cols-7 gap-3">
          {weekDays.map((dayDate) => {
            const dayStr = toDateInputString(dayDate);
            const isToday = dayStr === todayStr;
            const daySeances = filteredSeances.filter(
              (s) => extractSeanceDate(s) === dayStr
            );
            const dayName = new Intl.DateTimeFormat("fr-FR", {
              weekday: "short",
            }).format(dayDate);
            const dayNum = new Intl.DateTimeFormat("fr-FR", {
              day: "2-digit",
              month: "short",
            }).format(dayDate);

            return (
              <div
                key={dayStr}
                className={`flex flex-col rounded-xl border bg-card overflow-hidden transition-colors min-h-[260px] ${
                  isToday
                    ? "border-primary ring-2 ring-primary/20"
                    : "border-border"
                }`}
              >
                {/* En-tête du jour (cliquable pour basculer en Vue Jour) */}
                <button
                  type="button"
                  onClick={() => {
                    setReferenceDate(dayDate);
                    setViewMode("jour");
                  }}
                  title="Voir le planning détaillé de cette journée"
                  className={`cursor-pointer w-full px-3 py-2.5 border-b text-left flex items-center justify-between transition-colors ${
                    isToday
                      ? "bg-primary/10 border-primary/20"
                      : "bg-muted/40 border-border hover:bg-muted/70"
                  }`}
                >
                  <div>
                    <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
                      {dayName}
                    </div>
                    <div className="text-sm font-semibold text-foreground flex items-center gap-1.5">
                      <span>{dayNum}</span>
                      {isToday && (
                        <span className="px-1.5 py-0.2 text-[10px] font-bold rounded-full bg-primary text-primary-foreground">
                          Auj.
                        </span>
                      )}
                    </div>
                  </div>
                  <span className="text-xs font-mono px-2 py-0.5 rounded-full bg-background border border-border text-muted-foreground">
                    {daySeances.length}
                  </span>
                </button>

                {/* Liste des séances du jour */}
                <div className="p-2 flex-1 space-y-2">
                  {daySeances.length === 0 ? (
                    <div className="h-full min-h-[160px] flex items-center justify-center text-center p-2">
                      <span className="text-xs text-muted-foreground/60">
                        Aucune séance
                      </span>
                    </div>
                  ) : (
                    daySeances.map((seance) => {
                      const stStyle =
                        STATUT_STYLES[seance.statut] ?? STATUT_STYLES.PLANIFIEE;
                      const heureDebut = extractHeureDebut(seance);
                      const heureFin = extractHeureFin(seance);
                      const photographeNom = extractPhotographeNom(seance);
                      const clientNom = extractClientNom(seance);
                      const specialiteLibelle = extractSpecialiteLibelle(seance);

                      return (
                        <button
                          key={seance.id}
                          type="button"
                          onClick={() => void handleOpenDetail(seance)}
                          className={`cursor-pointer w-full text-left rounded-lg border p-2.5 transition-all shadow-2xs space-y-1.5 ${stStyle.cardClass}`}
                        >
                          <div className="flex items-center justify-between gap-1">
                            <span className="text-xs font-bold font-mono text-foreground">
                              {heureDebut} – {heureFin}
                            </span>
                            <span
                              className={`text-[10px] font-semibold px-1.5 py-0.5 rounded border ${stStyle.badgeClass}`}
                            >
                              {stStyle.label}
                            </span>
                          </div>

                          <div className="text-xs font-semibold text-foreground truncate">
                            {specialiteLibelle}
                          </div>

                          <div className="space-y-0.5 text-[11px] text-muted-foreground">
                            <div className="flex items-center gap-1 truncate">
                              <Camera className="w-3 h-3 shrink-0" />
                              <span className="truncate">{photographeNom}</span>
                            </div>
                            <div className="flex items-center gap-1 truncate">
                              <User className="w-3 h-3 shrink-0" />
                              <span className="truncate">{clientNom}</span>
                            </div>
                            <div className="flex items-center gap-1 truncate">
                              <MapPin className="w-3 h-3 shrink-0" />
                              <span className="truncate">{seance.lieu}</span>
                            </div>
                          </div>
                        </button>
                      );
                    })
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : viewMode === "jour" ? (
        /* ==================== VUE PLANNING PAR JOUR ==================== */
        <div className="space-y-4">
          {/* Barre d'accès rapide aux 7 jours de la semaine courante */}
          <div className="grid grid-cols-7 gap-2">
            {weekDays.map((d) => {
              const dStr = toDateInputString(d);
              const isSelected = dStr === referenceDateStr;
              const isToday = dStr === todayStr;
              return (
                <button
                  key={dStr}
                  type="button"
                  onClick={() => setReferenceDate(d)}
                  className={`cursor-pointer p-2 rounded-xl border text-center transition-all ${
                    isSelected
                      ? "bg-primary text-primary-foreground border-primary shadow-xs font-semibold"
                      : isToday
                        ? "bg-primary/10 text-foreground border-primary/40 hover:bg-primary/15"
                        : "bg-card text-muted-foreground border-border hover:text-foreground hover:bg-muted/50"
                  }`}
                >
                  <div className="text-[11px] uppercase">
                    {new Intl.DateTimeFormat("fr-FR", {
                      weekday: "short",
                    }).format(d)}
                  </div>
                  <div className="text-sm font-bold">{d.getDate()}</div>
                </button>
              );
            })}
          </div>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base flex items-center justify-between">
                <span>Créneaux horaires du {formatDateLongFr(referenceDateStr)}</span>
                <span className="text-xs font-normal text-muted-foreground">
                  Cliquez sur une séance pour consulter sa fiche complète
                </span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              {filteredSeances.length === 0 ? (
                <div className="py-12 text-center space-y-2 border border-dashed rounded-xl">
                  <CalendarDays className="w-8 h-8 text-muted-foreground mx-auto" />
                  <div className="text-sm font-medium text-foreground">
                    Aucune séance programmée pour cette journée
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Utilisez les flèches de navigation ou modifiez les filtres pour explorer d'autres dates.
                  </p>
                </div>
              ) : (
                <div className="divide-y divide-border border border-border rounded-xl overflow-hidden">
                  {HOURS_TIMELINE.map((hour) => {
                    const hourLabel = `${String(hour).padStart(2, "0")}:00`;
                    const slotSeances = filteredSeances.filter((s) => {
                      const [h] = extractHeureDebut(s).split(":").map(Number);
                      if (hour === 8) return h <= 8;
                      if (hour === 20) return h >= 20;
                      return h === hour;
                    });

                    return (
                      <div
                        key={hour}
                        className={`flex flex-col sm:flex-row sm:items-start gap-3 p-3 transition-colors ${
                          slotSeances.length > 0
                            ? "bg-card"
                            : "bg-muted/15 text-muted-foreground/60"
                        }`}
                      >
                        <div className="w-20 shrink-0 font-mono text-xs font-semibold pt-1">
                          {hourLabel}
                        </div>
                        <div className="flex-1 grid grid-cols-1 md:grid-cols-2 gap-2.5">
                          {slotSeances.length === 0 ? (
                            <span className="text-xs italic">Disponible</span>
                          ) : (
                            slotSeances.map((seance) => {
                              const stStyle =
                                STATUT_STYLES[seance.statut] ??
                                STATUT_STYLES.PLANIFIEE;
                              const heureDebut = extractHeureDebut(seance);
                              const heureFin = extractHeureFin(seance);
                              const dureeMin = extractDureeMinutes(seance);

                              return (
                                <button
                                  key={seance.id}
                                  type="button"
                                  onClick={() => void handleOpenDetail(seance)}
                                  className={`cursor-pointer text-left rounded-xl border p-3.5 transition-all shadow-xs space-y-2 ${stStyle.cardClass}`}
                                >
                                  <div className="flex items-center justify-between gap-2">
                                    <div className="flex items-center gap-2">
                                      <span className="font-mono text-sm font-bold text-foreground">
                                        {heureDebut} → {heureFin}
                                      </span>
                                      <span className="text-xs text-muted-foreground">
                                        ({formatDuration(dureeMin)})
                                      </span>
                                    </div>
                                    <span
                                      className={`text-xs font-semibold px-2 py-0.5 rounded-full border ${stStyle.badgeClass}`}
                                    >
                                      {stStyle.label}
                                    </span>
                                  </div>

                                  <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
                                    <Tag className="w-3.5 h-3.5 text-primary shrink-0" />
                                    <span>{extractSpecialiteLibelle(seance)}</span>
                                  </div>

                                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5 text-xs text-muted-foreground pt-1">
                                    <div className="flex items-center gap-1.5">
                                      <Camera className="w-3.5 h-3.5 shrink-0" />
                                      <span className="font-medium text-foreground truncate">
                                        {extractPhotographeNom(seance)}
                                      </span>
                                    </div>
                                    <div className="flex items-center gap-1.5">
                                      <User className="w-3.5 h-3.5 shrink-0" />
                                      <span className="font-medium text-foreground truncate">
                                        {extractClientNom(seance)}
                                      </span>
                                    </div>
                                    <div className="flex items-center gap-1.5 sm:col-span-2">
                                      <MapPin className="w-3.5 h-3.5 shrink-0" />
                                      <span className="truncate">
                                        {seance.lieu}
                                      </span>
                                    </div>
                                  </div>
                                </button>
                              );
                            })
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      ) : (
        /* ==================== VUE LISTE DES SÉANCES ==================== */
        <Card>
          <CardHeader className="pb-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <CardTitle className="text-base">
                  Liste des séances ({filteredSeances.length})
                </CardTitle>
                <CardDescription>
                  Vue tabulaire complète avec statut coloré, photographe, client, spécialité et fin calculée.
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            {filteredSeances.length === 0 ? (
              <div className="py-12 text-center space-y-2 border border-dashed rounded-xl">
                <List className="w-8 h-8 text-muted-foreground mx-auto" />
                <div className="text-sm font-medium text-foreground">
                  Aucune séance ne correspond aux critères sélectionnés
                </div>
                <p className="text-xs text-muted-foreground">
                  Essayez d'élargir la période ou de réinitialiser les filtres.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto rounded-xl border border-border">
                <table className="w-full text-left text-sm border-collapse">
                  <thead>
                    <tr className="border-b border-border bg-muted/50 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                      <th className="py-3 px-4">Date & Horaire</th>
                      <th className="py-3 px-4">Spécialité & Lieu</th>
                      <th className="py-3 px-4">Photographe</th>
                      <th className="py-3 px-4">Client</th>
                      <th className="py-3 px-4">Statut</th>
                      <th className="py-3 px-4 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border bg-card">
                    {filteredSeances.map((seance) => {
                      const stStyle =
                        STATUT_STYLES[seance.statut] ?? STATUT_STYLES.PLANIFIEE;
                      const dateStr = extractSeanceDate(seance);
                      const heureDebut = extractHeureDebut(seance);
                      const heureFin = extractHeureFin(seance);
                      const duree = extractDureeMinutes(seance);
                      const facture = extractFacture(seance);

                      return (
                        <tr
                          key={seance.id}
                          onClick={() => void handleOpenDetail(seance)}
                          className="cursor-pointer hover:bg-muted/40 transition-colors"
                        >
                          <td className="py-3 px-4 whitespace-nowrap">
                            <div className="font-semibold text-foreground">
                              {formatDateShortFr(dateStr)}
                            </div>
                            <div className="text-xs font-mono text-muted-foreground flex items-center gap-1 mt-0.5">
                              <Clock className="w-3 h-3" />
                              <span>
                                {heureDebut} → {heureFin} ({formatDuration(duree)})
                              </span>
                            </div>
                          </td>

                          <td className="py-3 px-4">
                            <div className="font-medium text-foreground flex items-center gap-1.5">
                              <Tag className="w-3.5 h-3.5 text-primary shrink-0" />
                              <span>{extractSpecialiteLibelle(seance)}</span>
                            </div>
                            <div className="text-xs text-muted-foreground flex items-center gap-1 mt-0.5">
                              <MapPin className="w-3 h-3 shrink-0" />
                              <span className="truncate max-w-xs">
                                {seance.lieu}
                              </span>
                            </div>
                          </td>

                          <td className="py-3 px-4 whitespace-nowrap">
                            <div className="font-medium text-foreground">
                              {extractPhotographeNom(seance)}
                            </div>
                            {extractPhotographeEmail(seance) && (
                              <div className="text-xs text-muted-foreground">
                                {extractPhotographeEmail(seance)}
                              </div>
                            )}
                          </td>

                          <td className="py-3 px-4 whitespace-nowrap">
                            <div className="font-medium text-foreground">
                              {extractClientNom(seance)}
                            </div>
                            {seance.client?.email && (
                              <div className="text-xs text-muted-foreground">
                                {seance.client.email}
                              </div>
                            )}
                          </td>

                          <td className="py-3 px-4 whitespace-nowrap">
                            <div className="flex flex-col items-start gap-1">
                              <span
                                className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${stStyle.badgeClass}`}
                              >
                                <span
                                  className={`w-1.5 h-1.5 rounded-full ${stStyle.dotClass}`}
                                />
                                {stStyle.label}
                              </span>
                              {facture && (
                                <span className="inline-flex items-center gap-1 text-[11px] font-mono text-muted-foreground">
                                  <FileText className="w-3 h-3" />
                                  {facture.numero}
                                </span>
                              )}
                            </div>
                          </td>

                          <td
                            className="py-3 px-4 text-right whitespace-nowrap"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <Button
                              type="button"
                              variant="outline"
                              size="xs"
                              onClick={() => void handleOpenDetail(seance)}
                              className="cursor-pointer gap-1"
                            >
                              <Eye className="w-3.5 h-3.5" />
                              <span>Détails</span>
                            </Button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* ==================== MODALE DE DÉTAIL D'UNE SÉANCE (GET /seances/{id}) ==================== */}
      {selectedSeanceId !== null && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="seance-detail-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 overflow-y-auto"
          onClick={handleCloseDetail}
        >
          <div
            className="w-full max-w-2xl text-left my-8"
            onClick={(e) => e.stopPropagation()}
          >
            <Card className="shadow-2xl border-border">
              <CardHeader className="border-b border-border pb-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <CardTitle id="seance-detail-title" className="text-lg">
                        Séance photo #{selectedSeanceId}
                      </CardTitle>
                      {seanceDetail && (
                        <span
                          className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${
                            (
                              STATUT_STYLES[seanceDetail.statut] ??
                              STATUT_STYLES.PLANIFIEE
                            ).badgeClass
                          }`}
                        >
                          <span
                            className={`w-2 h-2 rounded-full ${
                              (
                                STATUT_STYLES[seanceDetail.statut] ??
                                STATUT_STYLES.PLANIFIEE
                              ).dotClass
                            }`}
                          />
                          {
                            (
                              STATUT_STYLES[seanceDetail.statut] ??
                              STATUT_STYLES.PLANIFIEE
                            ).label
                          }
                        </span>
                      )}
                    </div>
                    <CardDescription>
                      Fiche détaillée de la séance récupérée depuis{" "}
                      <code className="text-xs">
                        GET /seances/{selectedSeanceId}
                      </code>
                    </CardDescription>
                  </div>

                  <button
                    type="button"
                    onClick={handleCloseDetail}
                    className="cursor-pointer p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted"
                    aria-label="Fermer le détail"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
              </CardHeader>

              <CardContent className="pt-5 space-y-5">
                {loadingDetail && (
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Loader2 className="w-4 h-4 animate-spin text-primary" />
                    <span>Actualisation des données détaillées...</span>
                  </div>
                )}

                <ErrorAlert
                  error={detailError}
                  onDismiss={() => setDetailError(null)}
                />

                {seanceDetail && (
                  <>
                    {/* Bloc 1 : Créneau, Fin calculée, Spécialité et Lieu */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 p-4 rounded-xl bg-muted/40 border border-border">
                      <div className="space-y-1">
                        <div className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5" />
                          <span>Date de la séance</span>
                        </div>
                        <div className="text-sm font-semibold text-foreground capitalize">
                          {formatDateLongFr(extractSeanceDate(seanceDetail))}
                        </div>
                      </div>

                      <div className="space-y-1">
                        <div className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                          <Clock className="w-3.5 h-3.5" />
                          <span>Horaires & Fin calculée</span>
                        </div>
                        <div className="text-sm font-semibold font-mono text-foreground">
                          {extractHeureDebut(seanceDetail)} →{" "}
                          {extractHeureFin(seanceDetail)}{" "}
                          <span className="font-sans font-normal text-xs text-muted-foreground">
                            ({formatDuration(extractDureeMinutes(seanceDetail))})
                          </span>
                        </div>
                        <div className="text-[11px] text-muted-foreground">
                          Fin calculée : {extractFinIsoOrFormatted(seanceDetail)}
                        </div>
                      </div>

                      <div className="space-y-1">
                        <div className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                          <Tag className="w-3.5 h-3.5" />
                          <span>Spécialité (Type de séance)</span>
                        </div>
                        <div className="text-sm font-semibold text-foreground">
                          {extractSpecialiteLibelle(seanceDetail)}
                        </div>
                      </div>

                      <div className="space-y-1">
                        <div className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                          <MapPin className="w-3.5 h-3.5" />
                          <span>Lieu</span>
                        </div>
                        <div className="text-sm font-semibold text-foreground">
                          {seanceDetail.lieu}
                        </div>
                      </div>
                    </div>

                    {/* Bloc 2 : Photographe & Client */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      {/* Photographe */}
                      <div className="p-4 rounded-xl border border-border bg-card space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                            <Camera className="w-3.5 h-3.5 text-primary" />
                            Photographe
                          </span>
                          {seanceDetail.photographe?.actif !== undefined && (
                            <span
                              className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                                seanceDetail.photographe.actif
                                  ? "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"
                                  : "bg-muted text-muted-foreground"
                              }`}
                            >
                              {seanceDetail.photographe.actif ? (
                                <>
                                  <CheckCircle2 className="w-3 h-3" /> Actif
                                </>
                              ) : (
                                <>
                                  <XCircle className="w-3 h-3" /> Inactif
                                </>
                              )}
                            </span>
                          )}
                        </div>
                        <div className="text-sm font-bold text-foreground">
                          {extractPhotographeNom(seanceDetail)}
                        </div>
                        {extractPhotographeEmail(seanceDetail) && (
                          <div className="text-xs text-muted-foreground">
                            {extractPhotographeEmail(seanceDetail)}
                          </div>
                        )}
                      </div>

                      {/* Client */}
                      <div className="p-4 rounded-xl border border-border bg-card space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                            <User className="w-3.5 h-3.5 text-primary" />
                            Client
                          </span>
                          {(seanceDetail.client?.typeClient ||
                            seanceDetail.client?.type) && (
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-secondary text-secondary-foreground border border-border">
                              {seanceDetail.client.typeClient ??
                                seanceDetail.client.type}
                            </span>
                          )}
                        </div>
                        <div className="text-sm font-bold text-foreground">
                          {extractClientNom(seanceDetail)}
                        </div>
                        {(seanceDetail.client?.email ||
                          seanceDetail.clientEmail) && (
                          <div className="text-xs text-muted-foreground">
                            {seanceDetail.client?.email ??
                              seanceDetail.clientEmail}
                          </div>
                        )}
                        {seanceDetail.client?.adressePostale && (
                          <div className="text-xs text-muted-foreground">
                            {seanceDetail.client.adressePostale}
                          </div>
                        )}
                        {seanceDetail.client?.numeroSirenTva && (
                          <div className="text-[11px] font-mono text-muted-foreground">
                            SIREN / TVA : {seanceDetail.client.numeroSirenTva}
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Bloc 3 : Facture éventuelle */}
                    <div className="p-4 rounded-xl border border-border bg-card space-y-2.5">
                      <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                        <FileText className="w-3.5 h-3.5 text-primary" />
                        <span>Facture associée</span>
                      </div>

                      {(() => {
                        const facture = extractFacture(seanceDetail);
                        if (!facture) {
                          return (
                            <div className="text-xs text-muted-foreground italic">
                              Aucune facture émise pour cette séance.
                            </div>
                          );
                        }

                        const stPaiement =
                          facture.statutPaiement ?? facture.statut;
                        const paiementBadge = stPaiement
                          ? STATUT_PAIEMENT_BADGE[stPaiement]
                          : null;

                        return (
                          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
                            <div>
                              <div className="text-xs text-muted-foreground">
                                Numéro
                              </div>
                              <div className="text-sm font-mono font-bold text-foreground">
                                {facture.numero}
                              </div>
                              {facture.dateEmission && (
                                <div className="text-[11px] text-muted-foreground">
                                  Émise le{" "}
                                  {formatDateShortFr(facture.dateEmission)}
                                </div>
                              )}
                            </div>

                            <div>
                              <div className="text-xs text-muted-foreground">
                                Montant
                              </div>
                              <div className="text-sm font-bold text-foreground">
                                {facture.montantTtc !== undefined
                                  ? `${formatCurrency(facture.montantTtc)} TTC`
                                  : facture.montantHt !== undefined
                                    ? `${formatCurrency(facture.montantHt)} HT`
                                    : "—"}
                              </div>
                              {facture.montantHt !== undefined && (
                                <div className="text-[11px] text-muted-foreground">
                                  HT : {formatCurrency(facture.montantHt)}
                                  {facture.tauxTva !== undefined
                                    ? ` (TVA ${facture.tauxTva}%)`
                                    : ""}
                                </div>
                              )}
                            </div>

                            <div>
                              <div className="text-xs text-muted-foreground">
                                Statut de paiement
                              </div>
                              {paiementBadge ? (
                                <span
                                  className={`mt-0.5 inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold border ${paiementBadge.className}`}
                                >
                                  {paiementBadge.label}
                                </span>
                              ) : (
                                <div className="text-sm font-medium">—</div>
                              )}
                              {facture.datePaiement && (
                                <div className="text-[11px] text-muted-foreground mt-0.5">
                                  Payée le{" "}
                                  {formatDateShortFr(facture.datePaiement)}
                                </div>
                              )}
                            </div>
                          </div>
                        );
                      })()}
                    </div>
                  </>
                )}

                <div className="flex justify-end pt-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={handleCloseDetail}
                    className="cursor-pointer"
                  >
                    Fermer
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      )}
    </div>
  );
}
