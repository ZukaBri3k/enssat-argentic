import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  apiAnnulerFacture,
  apiEnregistrerPaiementFacture,
  apiGetFactureById,
  apiGetFactures,
  apiGetSeances,
  type FactureResponse,
  type SeanceResponse,
  type StatutPaiement,
} from "@/lib/api";
import { GenererFactureModal } from "@/components/factures/GenererFactureModal";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
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
  Ban,
  Building2,
  Calendar,
  Camera,
  CheckCircle2,
  Clock,
  Euro,
  Eye,
  FilePlus2,
  FileText,
  Filter,
  Loader2,
  Lock,
  Mail,
  MapPin,
  Receipt,
  RefreshCw,
  RotateCcw,
  Search,
  Sparkles,
  Tag,
  User,
  X,
} from "lucide-react";

const ALL_STATUTS_PAIEMENT: StatutPaiement[] = ["EN_ATTENTE", "PAYEE", "ANNULEE"];

interface StatutPaiementStyle {
  label: string;
  description: string;
  badgeClass: string;
  dotClass: string;
  pillActiveClass: string;
}

const STATUT_PAIEMENT_STYLES: Record<StatutPaiement, StatutPaiementStyle> = {
  EN_ATTENTE: {
    label: "En attente",
    description: "Facture émise, règlement en attente",
    badgeClass:
      "bg-amber-100 text-amber-800 border-amber-300 dark:bg-amber-950/70 dark:text-amber-300 dark:border-amber-800",
    dotClass: "bg-amber-500",
    pillActiveClass:
      "bg-amber-600 text-white border-amber-600 shadow-xs hover:bg-amber-700",
  },
  PAYEE: {
    label: "Payée",
    description: "Facture réglée par le client",
    badgeClass:
      "bg-emerald-100 text-emerald-800 border-emerald-300 dark:bg-emerald-950/70 dark:text-emerald-300 dark:border-emerald-800",
    dotClass: "bg-emerald-500",
    pillActiveClass:
      "bg-emerald-600 text-white border-emerald-600 shadow-xs hover:bg-emerald-700",
  },
  ANNULEE: {
    label: "Annulée",
    description: "Facture annulée définitivement (séance non refacturable)",
    badgeClass:
      "bg-rose-100 text-rose-800 border-rose-300 dark:bg-rose-950/70 dark:text-rose-300 dark:border-rose-800",
    dotClass: "bg-rose-500",
    pillActiveClass:
      "bg-rose-600 text-white border-rose-600 shadow-xs hover:bg-rose-700",
  },
};

// --- Utilitaires de dates et formatage ---

function toDateInputString(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function parseLocalDate(dateStr?: string | null): Date | null {
  if (!dateStr) return null;
  const clean = dateStr.slice(0, 10);
  const parts = clean.split("-").map(Number);
  if (parts.length !== 3 || parts.some((n) => Number.isNaN(n))) return null;
  const [y, m, d] = parts;
  return new Date(y, m - 1, d);
}

function formatDateShortFr(dateStr?: string | null): string {
  const parsed = parseLocalDate(dateStr);
  if (!parsed) return "—";
  return new Intl.DateTimeFormat("fr-FR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(parsed);
}

function formatDateLongFr(dateStr?: string | null): string {
  const parsed = parseLocalDate(dateStr);
  if (!parsed) return "—";
  return new Intl.DateTimeFormat("fr-FR", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(parsed);
}

function formatCurrency(value: number | undefined | null): string {
  const numeric = typeof value === "number" && !Number.isNaN(value) ? value : 0;
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "EUR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numeric);
}

function formatPercent(value: number): string {
  const numeric = Number.isFinite(value) ? value : 0;
  return `${new Intl.NumberFormat("fr-FR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numeric)} %`;
}

function formatDuration(minutes: number): string {
  if (!minutes || minutes <= 0) return "0 min";
  const hours = Math.floor(minutes / 60);
  const rem = minutes % 60;
  if (hours === 0) return `${rem} min`;
  if (rem === 0) return `${hours} h`;
  return `${hours} h ${String(rem).padStart(2, "0")}`;
}

// --- Helpers de normalisation des factures (imbriqué vs à plat) ---

function extractFactureSeanceId(f: FactureResponse): number | undefined {
  return f.seanceId ?? f.seance?.id;
}

function extractStatutPaiement(f: FactureResponse): StatutPaiement {
  const raw = f.statutPaiement ?? f.statut;
  if (raw === "EN_ATTENTE" || raw === "PAYEE" || raw === "ANNULEE") {
    return raw;
  }
  return "EN_ATTENTE";
}

function extractClientIdentite(f: FactureResponse): string {
  return (
    f.coordonneesClient?.identite ||
    f.clientIdentite ||
    "Client non renseigné"
  );
}

function extractClientAdresse(f: FactureResponse): string {
  return (
    f.coordonneesClient?.adressePostale ||
    f.clientAdressePostale ||
    "Adresse non renseignée"
  );
}

function extractClientEmail(f: FactureResponse): string {
  return f.coordonneesClient?.email || f.clientEmail || "—";
}

function extractClientSirenTva(f: FactureResponse): string | null {
  const value =
    f.coordonneesClient?.numeroSirenTva ?? f.clientNumeroSirenTva ?? null;
  return value && value.trim() !== "" ? value.trim() : null;
}

function extractPrestationDate(f: FactureResponse): string {
  const raw = f.prestation?.date || f.prestationDate || "";
  return raw.slice(0, 10);
}

function extractPrestationHeure(f: FactureResponse): string {
  const raw = f.prestation?.heureDebut || f.prestationHeureDebut || "";
  if (!raw) return "—";
  if (raw.includes("T")) return raw.split("T")[1].slice(0, 5);
  return raw.slice(0, 5);
}

function extractPrestationDuree(f: FactureResponse): number {
  const raw = f.prestation?.dureeMinutes ?? f.prestationDureeMinutes ?? 0;
  return Number(raw) || 0;
}

function extractPrestationType(f: FactureResponse): string {
  return (
    f.prestation?.typeSeance ||
    f.prestationType ||
    f.typeSeance ||
    "Prestation photo"
  );
}

function extractPhotographeNom(f: FactureResponse): string {
  return (
    f.prestation?.nomPhotographe ||
    f.nomPhotographe ||
    f.photographeNom ||
    "Photographe non renseigné"
  );
}

function extractMontantHt(f: FactureResponse): number {
  const val = Number(f.montantHt);
  return Number.isFinite(val) ? val : 0;
}

function extractTauxTva(f: FactureResponse): number {
  if (f.tauxTva === undefined || f.tauxTva === null) return 20;
  const val = Number(f.tauxTva);
  return Number.isFinite(val) ? val : 20;
}

function extractMontantTtc(f: FactureResponse): number {
  if (f.montantTtc !== undefined && f.montantTtc !== null) {
    const val = Number(f.montantTtc);
    if (Number.isFinite(val)) return val;
  }
  const ht = extractMontantHt(f);
  const tva = extractTauxTva(f);
  return Math.round(ht * (1 + tva / 100) * 100) / 100;
}

function extractMontantTva(f: FactureResponse): number {
  const ht = extractMontantHt(f);
  const ttc = extractMontantTtc(f);
  return Math.round((ttc - ht) * 100) / 100;
}

// --- Helpers de normalisation des séances terminées ---

function extractSeanceDate(s: SeanceResponse): string {
  if (s.date) return s.date.slice(0, 10);
  if (s.dateSeance) return s.dateSeance.slice(0, 10);
  if (s.debut && s.debut.includes("T")) return s.debut.split("T")[0];
  return "";
}

function extractSeanceHeureDebut(s: SeanceResponse): string {
  if (s.heureDebut) return s.heureDebut.slice(0, 5);
  if (s.debut && s.debut.includes("T")) return s.debut.split("T")[1].slice(0, 5);
  return "00:00";
}

function extractSeanceDuree(s: SeanceResponse): number {
  return s.dureeMinutes ?? s.duree ?? 0;
}

function extractSeanceHeureFin(s: SeanceResponse): string {
  const rawFin = s.fin || s.heureFin || s.dateFin;
  if (rawFin) {
    if (rawFin.includes("T")) return rawFin.split("T")[1].slice(0, 5);
    if (rawFin.length >= 5) return rawFin.slice(0, 5);
  }
  const [h, m] = extractSeanceHeureDebut(s).split(":").map(Number);
  const total = Math.max(0, Math.min(24 * 60, (h || 0) * 60 + (m || 0) + extractSeanceDuree(s)));
  return `${String(Math.floor(total / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
}

function extractSeanceSpecialite(s: SeanceResponse): string {
  return s.specialite?.libelle ?? s.specialiteLibelle ?? s.typeSeance ?? "Spécialité";
}

function extractSeanceClient(s: SeanceResponse): string {
  if (s.client) {
    if (s.client.identite) return s.client.identite;
    if (s.client.raisonSociale) return s.client.raisonSociale;
    const full = [s.client.nom, s.client.prenom].filter(Boolean).join(" ").trim();
    if (full) return full;
    if (s.client.email) return s.client.email;
  }
  return s.clientIdentite ?? s.clientNom ?? "Client non renseigné";
}

function extractSeanceClientEmail(s: SeanceResponse): string | undefined {
  return s.client?.email ?? s.clientEmail;
}

function extractSeancePhotographe(s: SeanceResponse): string {
  return s.photographe?.nom ?? s.photographeNom ?? "Photographe non renseigné";
}

function seanceHasLinkedFacture(
  seance: SeanceResponse,
  invoicedSeanceIds: Set<number>
): boolean {
  if (seance.facture !== null && seance.facture !== undefined) return true;
  if (seance.factureId !== null && seance.factureId !== undefined && seance.factureId > 0) {
    return true;
  }
  if (seance.factureNumero && seance.factureNumero.trim() !== "") return true;
  if (seance.factureStatut) return true;
  return invoicedSeanceIds.has(seance.id);
}

type PeriodPreset = "all" | "month" | "year" | "last30";

function getPresetRange(preset: PeriodPreset): { du: string; au: string } {
  const today = new Date();
  if (preset === "all") {
    return { du: "", au: "" };
  }
  if (preset === "month") {
    const first = new Date(today.getFullYear(), today.getMonth(), 1);
    const last = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    return { du: toDateInputString(first), au: toDateInputString(last) };
  }
  if (preset === "year") {
    const first = new Date(today.getFullYear(), 0, 1);
    const last = new Date(today.getFullYear(), 11, 31);
    return { du: toDateInputString(first), au: toDateInputString(last) };
  }
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  start.setDate(start.getDate() - 30);
  return { du: toDateInputString(start), au: toDateInputString(today) };
}

export function FacturesPage() {
  const { showApiError, success } = useToast();

  // Filtres (GET /factures)
  const [selectedStatut, setSelectedStatut] = useState<StatutPaiement | "">("");
  const [filterDu, setFilterDu] = useState<string>("");
  const [filterAu, setFilterAu] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState<string>("");

  // Données factures (liste filtrée + référentiel complet non filtré pour détecter les séances déjà facturées)
  const [factures, setFactures] = useState<FactureResponse[]>([]);
  const [allFactures, setAllFactures] = useState<FactureResponse[]>([]);
  const [hasGlobalFacturesSnapshot, setHasGlobalFacturesSnapshot] = useState<boolean>(false);
  const [loadingFactures, setLoadingFactures] = useState<boolean>(true);
  const [facturesError, setFacturesError] = useState<unknown>(null);

  // Séances terminées (pour repérer celles en attente de facturation)
  const [seancesTerminees, setSeancesTerminees] = useState<SeanceResponse[]>([]);
  const [loadingSeances, setLoadingSeances] = useState<boolean>(true);
  const [seancesError, setSeancesError] = useState<unknown>(null);
  const [locallyInvoicedSeanceIds, setLocallyInvoicedSeanceIds] = useState<number[]>([]);

  // Modale de génération d'une facture
  const [seanceToInvoice, setSeanceToInvoice] = useState<SeanceResponse | null>(null);

  // Modale de détail d'une facture (GET /factures/{id})
  const [selectedFactureId, setSelectedFactureId] = useState<number | null>(null);
  const [factureDetail, setFactureDetail] = useState<FactureResponse | null>(null);
  const [loadingDetail, setLoadingDetail] = useState<boolean>(false);
  const [detailError, setDetailError] = useState<unknown>(null);
  const activeDetailIdRef = useRef<number | null>(null);

  // Actions sur la facture (POST /factures/{id}/paiement et /annulation)
  const [datePaiementSaisie, setDatePaiementSaisie] = useState<string>(() =>
    toDateInputString(new Date())
  );
  const [isPaying, setIsPaying] = useState<boolean>(false);
  const [confirmAnnulationOpen, setConfirmAnnulationOpen] = useState<boolean>(false);
  const [isCancelling, setIsCancelling] = useState<boolean>(false);
  const [actionError, setActionError] = useState<unknown>(null);
  const actionEnCours = isPaying || isCancelling;

  // Compteur de rafraîchissement manuel
  const [refreshTick, setRefreshTick] = useState<number>(0);

  const triggerRefresh = useCallback(() => {
    setRefreshTick((t) => t + 1);
  }, []);

  // Chargement parallèle des factures (filtrées + non filtrées si besoin) et des séances TERMINEE
  useEffect(() => {
    let mounted = true;
    const hasBackendFilters = Boolean(selectedStatut || filterDu || filterAu);

    async function loadData() {
      setLoadingFactures(true);
      setLoadingSeances(true);
      setFacturesError(null);
      setSeancesError(null);

      const results = await Promise.allSettled([
        apiGetFactures({
          statut: selectedStatut || undefined,
          du: filterDu || undefined,
          au: filterAu || undefined,
        }),
        apiGetSeances({ statut: "TERMINEE" }),
        hasBackendFilters ? apiGetFactures() : Promise.resolve(null),
      ]);

      if (!mounted) return;

      const [facturesRes, seancesRes, allFacturesRes] = results;

      if (facturesRes.status === "fulfilled") {
        const list = Array.isArray(facturesRes.value) ? facturesRes.value : [];
        setFactures(list);
        if (!hasBackendFilters) {
          setAllFactures(list);
          setHasGlobalFacturesSnapshot(true);
        }
      } else {
        setFactures([]);
        setFacturesError(facturesRes.reason);
        showApiError(facturesRes.reason, "Impossible de charger les factures");
      }

      if (hasBackendFilters && allFacturesRes.status === "fulfilled" && Array.isArray(allFacturesRes.value)) {
        setAllFactures(allFacturesRes.value);
        setHasGlobalFacturesSnapshot(true);
      }

      if (seancesRes.status === "fulfilled") {
        const list = Array.isArray(seancesRes.value) ? seancesRes.value : [];
        setSeancesTerminees(list.filter((s) => s.statut === "TERMINEE"));
      } else {
        setSeancesError(seancesRes.reason);
      }

      setLoadingFactures(false);
      setLoadingSeances(false);
    }

    void loadData();
    return () => {
      mounted = false;
    };
  }, [selectedStatut, filterDu, filterAu, refreshTick, showApiError]);

  // Ensemble global des IDs de séances déjà facturées (même si des filtres sont actifs)
  const invoicedSeanceIds = useMemo(() => {
    const ids = new Set<number>(locallyInvoicedSeanceIds);
    for (const f of allFactures) {
      const sId = extractFactureSeanceId(f);
      if (sId !== undefined) ids.add(sId);
    }
    for (const f of factures) {
      const sId = extractFactureSeanceId(f);
      if (sId !== undefined) ids.add(sId);
    }
    return ids;
  }, [allFactures, factures, locallyInvoicedSeanceIds]);

  // Séances TERMINEE n'ayant pas encore de facture
  const seancesEnAttenteFacturation = useMemo(() => {
    return seancesTerminees
      .filter(
        (s) =>
          s.statut === "TERMINEE" &&
          !seanceHasLinkedFacture(s, invoicedSeanceIds)
      )
      .sort((a, b) => {
        const dateCmp = extractSeanceDate(b).localeCompare(extractSeanceDate(a));
        if (dateCmp !== 0) return dateCmp;
        return extractSeanceHeureDebut(b).localeCompare(extractSeanceHeureDebut(a));
      });
  }, [seancesTerminees, invoicedSeanceIds]);

  // Filtrage et tri des factures affichées (complément local aux filtres backend + recherche)
  const matchesLocalSearch = useCallback((f: FactureResponse, term: string) => {
    const q = term.trim().toLowerCase();
    if (!q) return true;
    const numero = (f.numero || "").toLowerCase();
    const client = extractClientIdentite(f).toLowerCase();
    const email = extractClientEmail(f).toLowerCase();
    const siren = (extractClientSirenTva(f) || "").toLowerCase();
    const photographe = extractPhotographeNom(f).toLowerCase();
    const prestation = extractPrestationType(f).toLowerCase();
    return (
      numero.includes(q) ||
      client.includes(q) ||
      email.includes(q) ||
      siren.includes(q) ||
      photographe.includes(q) ||
      prestation.includes(q)
    );
  }, []);

  const displayedFactures = useMemo(() => {
    return factures
      .filter((f) => {
        const st = extractStatutPaiement(f);
        if (selectedStatut && st !== selectedStatut) return false;
        const emission = (f.dateEmission || "").slice(0, 10);
        if (filterDu && emission && emission < filterDu) return false;
        if (filterAu && emission && emission > filterAu) return false;
        return matchesLocalSearch(f, searchTerm);
      })
      .sort((a, b) => {
        const dateCmp = (b.dateEmission || "").localeCompare(a.dateEmission || "");
        if (dateCmp !== 0) return dateCmp;
        return (b.numero || "").localeCompare(a.numero || "");
      });
  }, [factures, selectedStatut, filterDu, filterAu, searchTerm, matchesLocalSearch]);

  // Compteurs par statut pour les pastilles (calculés sur la période + recherche en cours sans restreindre au statut sélectionné)
  const countsByStatut = useMemo(() => {
    const source = hasGlobalFacturesSnapshot ? allFactures : factures;
    const scoped = source.filter((f) => {
      const emission = (f.dateEmission || "").slice(0, 10);
      if (filterDu && emission && emission < filterDu) return false;
      if (filterAu && emission && emission > filterAu) return false;
      return matchesLocalSearch(f, searchTerm);
    });

    const counts: Record<"ALL" | StatutPaiement, number> = {
      ALL: scoped.length,
      EN_ATTENTE: 0,
      PAYEE: 0,
      ANNULEE: 0,
    };

    for (const f of scoped) {
      const st = extractStatutPaiement(f);
      counts[st] += 1;
    }
    return counts;
  }, [hasGlobalFacturesSnapshot, allFactures, factures, filterDu, filterAu, searchTerm, matchesLocalSearch]);

  // KPIs calculés sur les factures affichées
  const kpis = useMemo(() => {
    let totalHt = 0;
    let totalTtc = 0;
    let enAttenteCount = 0;
    let enAttenteTtc = 0;
    let payeesCount = 0;
    let payeesTtc = 0;
    let annuleesCount = 0;
    let annuleesTtc = 0;

    for (const f of displayedFactures) {
      const ht = extractMontantHt(f);
      const ttc = extractMontantTtc(f);
      const st = extractStatutPaiement(f);

      totalHt += ht;
      totalTtc += ttc;

      if (st === "EN_ATTENTE") {
        enAttenteCount += 1;
        enAttenteTtc += ttc;
      } else if (st === "PAYEE") {
        payeesCount += 1;
        payeesTtc += ttc;
      } else if (st === "ANNULEE") {
        annuleesCount += 1;
        annuleesTtc += ttc;
      }
    }

    return {
      totalCount: displayedFactures.length,
      totalHt,
      totalTtc,
      enAttenteCount,
      enAttenteTtc,
      payeesCount,
      payeesTtc,
      annuleesCount,
      annuleesTtc,
    };
  }, [displayedFactures]);

  // Gestion du succès après génération dans GenererFactureModal
  const handleFactureGenerated = useCallback(
    (createdFacture: FactureResponse, seanceId: number) => {
      const st = extractStatutPaiement(createdFacture);
      setLocallyInvoicedSeanceIds((prev) =>
        prev.includes(seanceId) ? prev : [...prev, seanceId]
      );

      setSeancesTerminees((prev) =>
        prev.map((s) =>
          s.id === seanceId
            ? {
                ...s,
                factureId: createdFacture.id,
                factureNumero: createdFacture.numero,
                factureStatut: st,
                facture: {
                  id: createdFacture.id,
                  numero: createdFacture.numero,
                  dateEmission: createdFacture.dateEmission,
                  montantHt: createdFacture.montantHt,
                  tauxTva: createdFacture.tauxTva,
                  montantTtc: createdFacture.montantTtc,
                  statutPaiement: st,
                  datePaiement: createdFacture.datePaiement ?? null,
                },
              }
            : s
        )
      );

      setAllFactures((prev) => [
        createdFacture,
        ...prev.filter((f) => f.id !== createdFacture.id),
      ]);

      const emission = (createdFacture.dateEmission || "").slice(0, 10);
      const matchesFilters =
        (!selectedStatut || st === selectedStatut) &&
        (!filterDu || !emission || emission >= filterDu) &&
        (!filterAu || !emission || emission <= filterAu);

      if (matchesFilters) {
        setFactures((prev) => [
          createdFacture,
          ...prev.filter((f) => f.id !== createdFacture.id),
        ]);
      }

      setSeanceToInvoice(null);

      // Si GET /factures fonctionne déjà côté backend, on resynchronise avec le serveur
      if (!facturesError) {
        triggerRefresh();
      }
    },
    [selectedStatut, filterDu, filterAu, facturesError, triggerRefresh]
  );

  // Ouverture et fermeture de la modale de détail (GET /factures/{id})
  const handleOpenDetail = useCallback(
    async (facturePreview: FactureResponse) => {
      activeDetailIdRef.current = facturePreview.id;
      setSelectedFactureId(facturePreview.id);
      setFactureDetail(facturePreview);
      setLoadingDetail(true);
      setDetailError(null);
      setActionError(null);
      setDatePaiementSaisie(toDateInputString(new Date()));

      try {
        const fullDetail = await apiGetFactureById(facturePreview.id);
        if (activeDetailIdRef.current === facturePreview.id) {
          setFactureDetail(fullDetail);
        }
      } catch (err) {
        if (activeDetailIdRef.current === facturePreview.id) {
          setDetailError(err);
          showApiError(
            err,
            `Impossible de charger la facture ${facturePreview.numero}`
          );
        }
      } finally {
        if (activeDetailIdRef.current === facturePreview.id) {
          setLoadingDetail(false);
        }
      }
    },
    [showApiError]
  );

  const handleCloseDetail = useCallback(() => {
    activeDetailIdRef.current = null;
    setSelectedFactureId(null);
    setFactureDetail(null);
    setDetailError(null);
    setLoadingDetail(false);
    setActionError(null);
    setConfirmAnnulationOpen(false);
  }, []);

  useEffect(() => {
    if (selectedFactureId === null) return;
    const onKeyDown = (e: KeyboardEvent) => {
      // Échap ferme d'abord la confirmation d'annulation, pas le détail
      if (e.key === "Escape" && !confirmAnnulationOpen && !actionEnCours) {
        handleCloseDetail();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [selectedFactureId, handleCloseDetail, confirmAnnulationOpen, actionEnCours]);

  // Remplace la facture modifiée dans le détail et les listes, puis resynchronise avec le serveur
  const applyFactureUpdate = useCallback(
    (updated: FactureResponse) => {
      if (activeDetailIdRef.current === updated.id) {
        setFactureDetail(updated);
      }
      setFactures((prev) => prev.map((f) => (f.id === updated.id ? updated : f)));
      setAllFactures((prev) => prev.map((f) => (f.id === updated.id ? updated : f)));
      triggerRefresh();
    },
    [triggerRefresh]
  );

  const handleEnregistrerPaiement = useCallback(async () => {
    if (!factureDetail) return;
    setIsPaying(true);
    setActionError(null);
    try {
      const updated = await apiEnregistrerPaiementFacture(factureDetail.id, datePaiementSaisie);
      applyFactureUpdate(updated);
      success(
        "Paiement enregistré",
        `La facture ${updated.numero} est payée au ${formatDateShortFr(updated.datePaiement)}.`
      );
    } catch (err) {
      setActionError(err);
      showApiError(err, `Impossible d'enregistrer le paiement de la facture ${factureDetail.numero}`);
    } finally {
      setIsPaying(false);
    }
  }, [factureDetail, datePaiementSaisie, applyFactureUpdate, success, showApiError]);

  const handleConfirmAnnulation = useCallback(async () => {
    if (!factureDetail) return;
    setIsCancelling(true);
    setActionError(null);
    try {
      const updated = await apiAnnulerFacture(factureDetail.id);
      applyFactureUpdate(updated);
      success("Facture annulée", `La facture ${updated.numero} a été annulée définitivement.`);
    } catch (err) {
      setActionError(err);
      showApiError(err, `Impossible d'annuler la facture ${factureDetail.numero}`);
    } finally {
      setIsCancelling(false);
      setConfirmAnnulationOpen(false);
    }
  }, [factureDetail, applyFactureUpdate, success, showApiError]);

  const applyPeriodPreset = (preset: PeriodPreset) => {
    const { du, au } = getPresetRange(preset);
    setFilterDu(du);
    setFilterAu(au);
  };

  const resetFilters = () => {
    setSelectedStatut("");
    setFilterDu("");
    setFilterAu("");
    setSearchTerm("");
  };

  const hasActiveFilters = Boolean(
    selectedStatut || filterDu || filterAu || searchTerm.trim()
  );
  const invalidPeriodOrder = Boolean(filterDu && filterAu && filterDu > filterAu);

  return (
    <div className="space-y-6">
      {/* En-tête */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2.5">
            <FileText className="w-6 h-6 text-primary" />
            <span>Factures</span>
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Génération des factures pour les séances terminées, suivi des paiements et consultation des pièces figées.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          {seancesEnAttenteFacturation.length > 0 && (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-300 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800">
              <Sparkles className="w-3.5 h-3.5" />
              <span>
                {seancesEnAttenteFacturation.length} séance
                {seancesEnAttenteFacturation.length > 1 ? "s" : ""} à facturer
              </span>
            </span>
          )}
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={triggerRefresh}
            disabled={loadingFactures || loadingSeances}
            className="cursor-pointer gap-1.5"
          >
            <RefreshCw
              className={`w-3.5 h-3.5 ${
                loadingFactures || loadingSeances ? "animate-spin" : ""
              }`}
            />
            <span>Actualiser</span>
          </Button>
        </div>
      </div>

      {/* Section 1 : Séances terminées en attente de facturation */}
      {seancesEnAttenteFacturation.length > 0 && (
        <Card className="border-amber-500/35 bg-amber-50/35 dark:bg-amber-950/15 shadow-xs">
          <CardHeader className="pb-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="space-y-1">
                <CardTitle className="text-base flex items-center gap-2 text-foreground">
                  <Receipt className="w-5 h-5 text-amber-600 dark:text-amber-400" />
                  <span>Séances terminées en attente de facturation</span>
                  <span className="inline-flex items-center justify-center px-2 py-0.5 rounded-full text-xs font-bold bg-amber-500/20 text-amber-900 dark:text-amber-200 border border-amber-500/30">
                    {seancesEnAttenteFacturation.length}
                  </span>
                </CardTitle>
                <CardDescription>
                  Ces séances au statut <strong>TERMINEE</strong> ne disposent pas encore de facture.
                  Générez leur facture en saisissant le montant HT et le taux de TVA.
                </CardDescription>
              </div>
            </div>
          </CardHeader>

          <CardContent className="space-y-2.5">
            {seancesEnAttenteFacturation.map((seance) => {
              const dateStr = extractSeanceDate(seance);
              const heureDebut = extractSeanceHeureDebut(seance);
              const heureFin = extractSeanceHeureFin(seance);
              const duree = extractSeanceDuree(seance);
              const clientNom = extractSeanceClient(seance);
              const clientEmail = extractSeanceClientEmail(seance);
              const photographeNom = extractSeancePhotographe(seance);
              const specialite = extractSeanceSpecialite(seance);

              return (
                <div
                  key={seance.id}
                  className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 p-3.5 rounded-xl border border-border bg-card hover:bg-muted/30 transition-colors"
                >
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 flex-1 min-w-0">
                    {/* Date & créneau */}
                    <div className="space-y-0.5 min-w-0">
                      <div className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                        <Calendar className="w-3.5 h-3.5 text-primary shrink-0" />
                        <span>Date & Créneau</span>
                      </div>
                      <div className="text-sm font-semibold text-foreground">
                        {formatDateShortFr(dateStr)}
                      </div>
                      <div className="text-xs font-mono text-muted-foreground flex items-center gap-1">
                        <Clock className="w-3 h-3 shrink-0" />
                        <span>
                          {heureDebut} – {heureFin} ({formatDuration(duree)})
                        </span>
                      </div>
                    </div>

                    {/* Spécialité & Lieu */}
                    <div className="space-y-0.5 min-w-0">
                      <div className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                        <Tag className="w-3.5 h-3.5 text-primary shrink-0" />
                        <span>Spécialité & Lieu</span>
                      </div>
                      <div className="text-sm font-semibold text-foreground truncate">
                        {specialite}
                        <span className="ml-1.5 text-xs font-mono font-normal text-muted-foreground">
                          #{seance.id}
                        </span>
                      </div>
                      <div className="text-xs text-muted-foreground flex items-center gap-1 truncate">
                        <MapPin className="w-3 h-3 shrink-0" />
                        <span className="truncate">{seance.lieu}</span>
                      </div>
                    </div>

                    {/* Client */}
                    <div className="space-y-0.5 min-w-0">
                      <div className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                        <User className="w-3.5 h-3.5 text-primary shrink-0" />
                        <span>Client</span>
                      </div>
                      <div className="text-sm font-medium text-foreground truncate">
                        {clientNom}
                      </div>
                      {clientEmail && (
                        <div className="text-xs text-muted-foreground truncate">
                          {clientEmail}
                        </div>
                      )}
                    </div>

                    {/* Photographe */}
                    <div className="space-y-0.5 min-w-0">
                      <div className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                        <Camera className="w-3.5 h-3.5 text-primary shrink-0" />
                        <span>Photographe</span>
                      </div>
                      <div className="text-sm font-medium text-foreground truncate">
                        {photographeNom}
                      </div>
                      <div className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-purple-100 text-purple-800 border border-purple-200 dark:bg-purple-950/60 dark:text-purple-300 dark:border-purple-800">
                        TERMINEE
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-end shrink-0">
                    <Button
                      type="button"
                      size="sm"
                      onClick={() => setSeanceToInvoice(seance)}
                      className="cursor-pointer gap-1.5 w-full lg:w-auto"
                    >
                      <FilePlus2 className="w-4 h-4" />
                      <span>Générer la facture</span>
                    </Button>
                  </div>
                </div>
              );
            })}
          </CardContent>
        </Card>
      )}

      {/* Avertissement discret si le chargement des séances terminées a échoué */}
      <ErrorAlert
        title="Séances terminées indisponibles"
        error={seancesError}
        onDismiss={() => setSeancesError(null)}
      />

      {/* Section 2 : Cartes de synthèse (KPIs) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {/* KPI Total affiché */}
        <div className="p-4 rounded-xl border border-primary/25 bg-primary/5 shadow-xs space-y-1.5">
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-primary">
              Total affiché
            </span>
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-background border border-border text-foreground">
              {kpis.totalCount} facture{kpis.totalCount > 1 ? "s" : ""}
            </span>
          </div>
          <div className="text-2xl font-bold tracking-tight text-foreground">
            {loadingFactures ? "—" : formatCurrency(kpis.totalTtc)}
            <span className="text-xs font-semibold text-muted-foreground ml-1">TTC</span>
          </div>
          <div className="text-xs text-muted-foreground flex items-center gap-1.5">
            <Euro className="w-3.5 h-3.5 text-primary shrink-0" />
            <span>
              Cumul HT : <strong className="text-foreground">{formatCurrency(kpis.totalHt)}</strong>
            </span>
          </div>
        </div>

        {/* KPI En attente */}
        <div className="p-4 rounded-xl border border-amber-200 dark:border-amber-900/60 bg-amber-50/40 dark:bg-amber-950/15 shadow-xs space-y-1.5">
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-amber-800 dark:text-amber-300 flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5" />
              <span>En attente</span>
            </span>
            <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-300 dark:bg-amber-950/70 dark:text-amber-300 dark:border-amber-800">
              {kpis.enAttenteCount}
            </span>
          </div>
          <div className="text-2xl font-bold tracking-tight text-foreground">
            {loadingFactures ? "—" : formatCurrency(kpis.enAttenteTtc)}
            <span className="text-xs font-semibold text-muted-foreground ml-1">TTC</span>
          </div>
          <div className="text-xs text-muted-foreground">
            Factures en attente de règlement
          </div>
        </div>

        {/* KPI Payées */}
        <div className="p-4 rounded-xl border border-emerald-200 dark:border-emerald-900/60 bg-emerald-50/40 dark:bg-emerald-950/15 shadow-xs space-y-1.5">
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-emerald-800 dark:text-emerald-300 flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Payées</span>
            </span>
            <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-300 dark:bg-emerald-950/70 dark:text-emerald-300 dark:border-emerald-800">
              {kpis.payeesCount}
            </span>
          </div>
          <div className="text-2xl font-bold tracking-tight text-foreground">
            {loadingFactures ? "—" : formatCurrency(kpis.payeesTtc)}
            <span className="text-xs font-semibold text-muted-foreground ml-1">TTC</span>
          </div>
          <div className="text-xs text-muted-foreground">
            Montant total encaissé
          </div>
        </div>

        {/* KPI Annulées */}
        <div className="p-4 rounded-xl border border-rose-200 dark:border-rose-900/60 bg-rose-50/40 dark:bg-rose-950/15 shadow-xs space-y-1.5">
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-rose-800 dark:text-rose-300 flex items-center gap-1.5">
              <Ban className="w-3.5 h-3.5" />
              <span>Annulées</span>
            </span>
            <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800 border border-rose-300 dark:bg-rose-950/70 dark:text-rose-300 dark:border-rose-800">
              {kpis.annuleesCount}
            </span>
          </div>
          <div className="text-2xl font-bold tracking-tight text-foreground">
            {loadingFactures ? "—" : formatCurrency(kpis.annuleesTtc)}
            <span className="text-xs font-semibold text-muted-foreground ml-1">TTC</span>
          </div>
          <div className="text-xs text-muted-foreground">
            Annulations définitives
          </div>
        </div>
      </div>

      {/* Section 3 : Barre de filtres (GET /factures) */}
      <Card>
        <CardContent className="pt-5 space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-end">
            {/* Recherche textuelle locale */}
            <div className="space-y-1.5">
              <Label htmlFor="factures-search" className="flex items-center gap-1.5 text-xs">
                <Search className="w-3.5 h-3.5 text-muted-foreground" />
                <span>Recherche</span>
              </Label>
              <div className="relative">
                <Search className="w-3.5 h-3.5 text-muted-foreground absolute left-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <Input
                  id="factures-search"
                  type="search"
                  placeholder="N° FAC-..., client, photographe..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>

            {/* Filtre par statut (<select>) */}
            <div className="space-y-1.5">
              <Label htmlFor="factures-statut" className="flex items-center gap-1.5 text-xs">
                <Filter className="w-3.5 h-3.5 text-muted-foreground" />
                <span>Statut de paiement</span>
              </Label>
              <select
                id="factures-statut"
                value={selectedStatut}
                onChange={(e) =>
                  setSelectedStatut(e.target.value as StatutPaiement | "")
                }
                className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 dark:bg-input/30"
              >
                <option value="">Tous les statuts</option>
                {ALL_STATUTS_PAIEMENT.map((st) => (
                  <option key={st} value={st}>
                    {STATUT_PAIEMENT_STYLES[st].label} ({st})
                  </option>
                ))}
              </select>
            </div>

            {/* Période d'émission : Du */}
            <div className="space-y-1.5">
              <Label htmlFor="factures-du" className="flex items-center gap-1.5 text-xs">
                <Calendar className="w-3.5 h-3.5 text-muted-foreground" />
                <span>Période du</span>
              </Label>
              <Input
                id="factures-du"
                type="date"
                value={filterDu}
                onChange={(e) => setFilterDu(e.target.value)}
                aria-invalid={invalidPeriodOrder ? true : undefined}
              />
            </div>

            {/* Période d'émission : Au */}
            <div className="space-y-1.5">
              <Label htmlFor="factures-au" className="flex items-center gap-1.5 text-xs">
                <Calendar className="w-3.5 h-3.5 text-muted-foreground" />
                <span>Au</span>
              </Label>
              <Input
                id="factures-au"
                type="date"
                value={filterAu}
                onChange={(e) => setFilterAu(e.target.value)}
                aria-invalid={invalidPeriodOrder ? true : undefined}
              />
            </div>
          </div>

          {invalidPeriodOrder && (
            <p className="text-xs text-destructive font-medium">
              La date de début (« Période du ») est postérieure à la date de fin (« Au »).
            </p>
          )}

          {/* Raccourcis de période + actions */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 pt-2 border-t border-border">
            <div className="flex flex-wrap items-center gap-1.5">
              <span className="text-xs text-muted-foreground mr-1">
                Période d'émission :
              </span>
              {(
                [
                  { id: "all", label: "Toutes les dates" },
                  { id: "month", label: "Ce mois-ci" },
                  { id: "year", label: "Cette année" },
                  { id: "last30", label: "30 derniers jours" },
                ] as const
              ).map((preset) => {
                const range = getPresetRange(preset.id);
                const isActive = filterDu === range.du && filterAu === range.au;
                return (
                  <Button
                    key={preset.id}
                    type="button"
                    variant={isActive ? "secondary" : "outline"}
                    size="xs"
                    onClick={() => applyPeriodPreset(preset.id)}
                    className="cursor-pointer"
                  >
                    {preset.label}
                  </Button>
                );
              })}
            </div>

            <div className="flex flex-wrap items-center gap-2">
              {hasActiveFilters && (
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
              <Button
                type="button"
                variant="outline"
                size="xs"
                onClick={triggerRefresh}
                disabled={loadingFactures}
                className="cursor-pointer gap-1"
              >
                <RefreshCw
                  className={`w-3 h-3 ${loadingFactures ? "animate-spin" : ""}`}
                />
                <span>Actualiser</span>
              </Button>
            </div>
          </div>

          {/* Pastilles colorées cliquables avec compteurs */}
          <div className="pt-3 border-t border-border flex flex-wrap items-center gap-2">
            <span className="text-xs font-medium text-muted-foreground mr-1">
              Statut :
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
              <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-background/20 font-mono">
                {countsByStatut.ALL}
              </span>
            </button>

            {ALL_STATUTS_PAIEMENT.map((st) => {
              const style = STATUT_PAIEMENT_STYLES[st];
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
        </CardContent>
      </Card>

      {/* Affichage des erreurs backend éventuelles sur GET /factures */}
      <ErrorAlert
        error={facturesError}
        onDismiss={() => setFacturesError(null)}
      />

      {/* Section 4 : Tableau de la liste des factures */}
      <Card>
        <CardHeader className="pb-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <CardTitle className="text-base">
                Liste des factures ({displayedFactures.length})
              </CardTitle>
              <CardDescription>
                Données figées au moment de l'émission : coordonnées client, détail de prestation et montants calculés.
              </CardDescription>
            </div>
          </div>
        </CardHeader>

        <CardContent>
          {loadingFactures ? (
            <div className="py-16 flex flex-col items-center justify-center gap-3 text-muted-foreground">
              <Loader2 className="w-7 h-7 animate-spin text-primary" />
              <p className="text-sm font-medium">Chargement des factures...</p>
            </div>
          ) : displayedFactures.length === 0 ? (
            <div className="py-12 text-center space-y-3 border border-dashed rounded-xl">
              <FileText className="w-8 h-8 text-muted-foreground mx-auto" />
              <div className="space-y-1">
                <div className="text-sm font-medium text-foreground">
                  {hasActiveFilters
                    ? "Aucune facture ne correspond aux critères sélectionnés"
                    : "Aucune facture enregistrée pour le moment"}
                </div>
                <p className="text-xs text-muted-foreground max-w-md mx-auto">
                  {hasActiveFilters
                    ? "Modifiez le statut, élargissez la période d'émission ou réinitialisez la recherche."
                    : "Les factures apparaîtront ici dès qu'une séance terminée sera facturée."}
                </p>
              </div>
              {hasActiveFilters && (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={resetFilters}
                  className="cursor-pointer gap-1.5"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Réinitialiser les filtres</span>
                </Button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-border">
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="border-b border-border bg-muted/50 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    <th className="py-3 px-4">Numéro & Émission</th>
                    <th className="py-3 px-4">Client (copie figée)</th>
                    <th className="py-3 px-4">Prestation & Photographe</th>
                    <th className="py-3 px-4 text-right">Montant HT & TVA</th>
                    <th className="py-3 px-4 text-right">Montant TTC</th>
                    <th className="py-3 px-4">Statut de paiement</th>
                    <th className="py-3 px-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border bg-card">
                  {displayedFactures.map((facture) => {
                    const statut = extractStatutPaiement(facture);
                    const stStyle =
                      STATUT_PAIEMENT_STYLES[statut] ??
                      STATUT_PAIEMENT_STYLES.EN_ATTENTE;
                    const clientIdentite = extractClientIdentite(facture);
                    const clientEmail = extractClientEmail(facture);
                    const clientSiren = extractClientSirenTva(facture);
                    const prestationType = extractPrestationType(facture);
                    const prestationDate = extractPrestationDate(facture);
                    const prestationHeure = extractPrestationHeure(facture);
                    const prestationDuree = extractPrestationDuree(facture);
                    const photographeNom = extractPhotographeNom(facture);
                    const ht = extractMontantHt(facture);
                    const tauxTva = extractTauxTva(facture);
                    const montantTva = extractMontantTva(facture);
                    const ttc = extractMontantTtc(facture);

                    return (
                      <tr
                        key={facture.id}
                        onClick={() => void handleOpenDetail(facture)}
                        className="cursor-pointer hover:bg-muted/40 transition-colors"
                      >
                        {/* Numéro & Date d'émission */}
                        <td className="py-3 px-4 whitespace-nowrap">
                          <div className="font-mono font-bold text-foreground">
                            {facture.numero}
                          </div>
                          <div className="text-xs text-muted-foreground mt-0.5">
                            Émise le {formatDateShortFr(facture.dateEmission)}
                          </div>
                        </td>

                        {/* Client (copie figée) */}
                        <td className="py-3 px-4">
                          <div className="font-medium text-foreground">
                            {clientIdentite}
                          </div>
                          <div className="text-xs text-muted-foreground">
                            {clientEmail}
                          </div>
                          {clientSiren && (
                            <div className="text-[11px] font-mono text-muted-foreground mt-0.5">
                              SIREN / TVA : {clientSiren}
                            </div>
                          )}
                        </td>

                        {/* Prestation & Photographe (copie figée) */}
                        <td className="py-3 px-4">
                          <div className="font-medium text-foreground flex items-center gap-1.5">
                            <Tag className="w-3.5 h-3.5 text-primary shrink-0" />
                            <span>{prestationType}</span>
                          </div>
                          <div className="text-xs text-muted-foreground mt-0.5">
                            {formatDateShortFr(prestationDate)}
                            {prestationHeure !== "—" && ` à ${prestationHeure}`}
                            {prestationDuree > 0 &&
                              ` (${formatDuration(prestationDuree)})`}
                          </div>
                          <div className="text-xs text-muted-foreground flex items-center gap-1 mt-0.5">
                            <Camera className="w-3 h-3 shrink-0" />
                            <span>{photographeNom}</span>
                          </div>
                        </td>

                        {/* Montant HT & TVA */}
                        <td className="py-3 px-4 text-right whitespace-nowrap">
                          <div className="font-medium text-foreground">
                            {formatCurrency(ht)} HT
                          </div>
                          <div className="text-xs text-muted-foreground mt-0.5">
                            TVA {formatPercent(tauxTva)} ({formatCurrency(montantTva)})
                          </div>
                        </td>

                        {/* Montant TTC */}
                        <td className="py-3 px-4 text-right whitespace-nowrap">
                          <div className="text-base font-bold text-foreground">
                            {formatCurrency(ttc)}
                          </div>
                          <div className="text-[11px] text-muted-foreground">TTC</div>
                        </td>

                        {/* Statut de paiement */}
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
                            {statut === "PAYEE" && facture.datePaiement && (
                              <span className="text-[11px] text-muted-foreground">
                                Payée le {formatDateShortFr(facture.datePaiement)}
                              </span>
                            )}
                          </div>
                        </td>

                        {/* Action */}
                        <td
                          className="py-3 px-4 text-right whitespace-nowrap"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <Button
                            type="button"
                            variant="outline"
                            size="xs"
                            onClick={() => void handleOpenDetail(facture)}
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

      {/* Modale de génération de facture */}
      <GenererFactureModal
        seance={seanceToInvoice}
        onClose={() => setSeanceToInvoice(null)}
        onSuccess={handleFactureGenerated}
      />

      {/* Modale de détail d'une facture (GET /factures/{id}) */}
      {selectedFactureId !== null && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="facture-detail-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 overflow-y-auto"
          onClick={() => {
            if (!actionEnCours) handleCloseDetail();
          }}
        >
          <div
            className="w-full max-w-2xl text-left my-8"
            onClick={(e) => e.stopPropagation()}
          >
            <Card className="shadow-2xl border-border">
              <CardHeader className="border-b border-border pb-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="space-y-1.5">
                    <div className="text-xs font-bold uppercase tracking-widest text-primary flex items-center gap-1.5">
                      <Camera className="w-3.5 h-3.5" />
                      <span>Studio Argentic</span>
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                      <CardTitle
                        id="facture-detail-title"
                        className="text-xl font-mono"
                      >
                        {factureDetail?.numero ?? `Facture #${selectedFactureId}`}
                      </CardTitle>
                      {factureDetail && (
                        <span
                          className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${
                            STATUT_PAIEMENT_STYLES[extractStatutPaiement(factureDetail)]
                              .badgeClass
                          }`}
                        >
                          <span
                            className={`w-2 h-2 rounded-full ${
                              STATUT_PAIEMENT_STYLES[extractStatutPaiement(factureDetail)]
                                .dotClass
                            }`}
                          />
                          {
                            STATUT_PAIEMENT_STYLES[extractStatutPaiement(factureDetail)]
                              .label
                          }
                        </span>
                      )}
                    </div>
                    <CardDescription>
                      Fiche complète de la facture récupérée depuis{" "}
                      <code className="text-xs">GET /factures/{selectedFactureId}</code>
                    </CardDescription>
                  </div>

                  <button
                    type="button"
                    onClick={handleCloseDetail}
                    className="cursor-pointer p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted"
                    aria-label="Fermer le détail de la facture"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
              </CardHeader>

              <CardContent className="pt-5 space-y-5">
                {loadingDetail && (
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Loader2 className="w-4 h-4 animate-spin text-primary" />
                    <span>Actualisation des données de la facture...</span>
                  </div>
                )}

                <ErrorAlert
                  error={detailError}
                  onDismiss={() => setDetailError(null)}
                />

                {factureDetail &&
                  (() => {
                    const statut = extractStatutPaiement(factureDetail);
                    const seanceId = extractFactureSeanceId(factureDetail);
                    const clientIdentite = extractClientIdentite(factureDetail);
                    const clientAdresse = extractClientAdresse(factureDetail);
                    const clientEmail = extractClientEmail(factureDetail);
                    const clientSiren = extractClientSirenTva(factureDetail);
                    const prestationType = extractPrestationType(factureDetail);
                    const prestationDate = extractPrestationDate(factureDetail);
                    const prestationHeure = extractPrestationHeure(factureDetail);
                    const prestationDuree = extractPrestationDuree(factureDetail);
                    const photographeNom = extractPhotographeNom(factureDetail);
                    const ht = extractMontantHt(factureDetail);
                    const tauxTva = extractTauxTva(factureDetail);
                    const montantTva = extractMontantTva(factureDetail);
                    const ttc = extractMontantTtc(factureDetail);

                    return (
                      <>
                        {/* Métadonnées d'émission et de paiement */}
                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 p-4 rounded-xl bg-muted/40 border border-border">
                          <div className="space-y-1">
                            <div className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                              <Calendar className="w-3.5 h-3.5" />
                              <span>Date d'émission</span>
                            </div>
                            <div className="text-sm font-semibold text-foreground capitalize">
                              {formatDateLongFr(factureDetail.dateEmission)}
                            </div>
                          </div>

                          <div className="space-y-1">
                            <div className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              <span>Paiement</span>
                            </div>
                            <div className="text-sm font-semibold text-foreground capitalize">
                              {statut === "PAYEE" && factureDetail.datePaiement
                                ? formatDateLongFr(factureDetail.datePaiement)
                                : statut === "ANNULEE"
                                  ? "Facture annulée"
                                  : "En attente de règlement"}
                            </div>
                          </div>

                          <div className="space-y-1">
                            <div className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                              <FileText className="w-3.5 h-3.5" />
                              <span>Référence</span>
                            </div>
                            <div className="text-sm font-mono font-semibold text-foreground">
                              {factureDetail.numero}
                            </div>
                            {seanceId !== undefined && (
                              <div className="text-[11px] text-muted-foreground">
                                Séance associée #{seanceId}
                              </div>
                            )}
                          </div>
                        </div>

                        {/* Blocs figés : Coordonnées du client & Prestation facturée */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                          {/* Coordonnées du client (copie figée) */}
                          <div className="p-4 rounded-xl border border-border bg-card space-y-2.5">
                            <div className="flex items-center justify-between gap-2">
                              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                                <User className="w-3.5 h-3.5 text-primary" />
                                <span>Coordonnées du client</span>
                              </span>
                              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-muted text-muted-foreground border border-border">
                                <Lock className="w-2.5 h-2.5" />
                                Copie figée
                              </span>
                            </div>

                            <div className="text-sm font-bold text-foreground">
                              {clientIdentite}
                            </div>

                            <div className="text-xs text-muted-foreground flex items-start gap-1.5">
                              <MapPin className="w-3.5 h-3.5 shrink-0 mt-0.5" />
                              <span>{clientAdresse}</span>
                            </div>

                            <div className="text-xs text-muted-foreground flex items-center gap-1.5">
                              <Mail className="w-3.5 h-3.5 shrink-0" />
                              <span>{clientEmail}</span>
                            </div>

                            {clientSiren && (
                              <div className="text-xs font-mono text-muted-foreground flex items-center gap-1.5 pt-0.5">
                                <Building2 className="w-3.5 h-3.5 shrink-0" />
                                <span>SIREN / TVA : {clientSiren}</span>
                              </div>
                            )}
                          </div>

                          {/* Prestation facturée (copie figée) */}
                          <div className="p-4 rounded-xl border border-border bg-card space-y-2.5">
                            <div className="flex items-center justify-between gap-2">
                              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                                <Tag className="w-3.5 h-3.5 text-primary" />
                                <span>Prestation facturée</span>
                              </span>
                              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-muted text-muted-foreground border border-border">
                                <Lock className="w-2.5 h-2.5" />
                                Copie figée
                              </span>
                            </div>

                            <div className="text-sm font-bold text-foreground">
                              {prestationType}
                            </div>

                            <div className="text-xs text-muted-foreground flex items-center gap-1.5 capitalize">
                              <Calendar className="w-3.5 h-3.5 shrink-0" />
                              <span>{formatDateLongFr(prestationDate)}</span>
                            </div>

                            <div className="text-xs font-mono text-muted-foreground flex items-center gap-1.5">
                              <Clock className="w-3.5 h-3.5 shrink-0" />
                              <span>
                                Début à {prestationHeure}
                                {prestationDuree > 0 &&
                                  ` • Durée ${formatDuration(prestationDuree)}`}
                              </span>
                            </div>

                            <div className="text-xs text-muted-foreground flex items-center gap-1.5">
                              <Camera className="w-3.5 h-3.5 shrink-0" />
                              <span>Photographe : <strong className="text-foreground">{photographeNom}</strong></span>
                            </div>
                          </div>
                        </div>

                        {/* Récapitulatif financier */}
                        <div className="p-4 rounded-xl border border-primary/25 bg-primary/5 space-y-3">
                          <div className="text-xs font-semibold uppercase tracking-wider text-primary flex items-center gap-1.5">
                            <Euro className="w-3.5 h-3.5" />
                            <span>Récapitulatif financier</span>
                          </div>

                          <div className="space-y-2 text-sm">
                            <div className="flex items-center justify-between">
                              <span className="text-muted-foreground">Montant HT</span>
                              <span className="font-semibold text-foreground">
                                {formatCurrency(ht)}
                              </span>
                            </div>

                            <div className="flex items-center justify-between">
                              <span className="text-muted-foreground">
                                TVA ({formatPercent(tauxTva)})
                              </span>
                              <span className="font-medium text-foreground">
                                {formatCurrency(montantTva)}
                              </span>
                            </div>

                            <div className="pt-2 border-t border-primary/20 flex items-center justify-between">
                              <span className="text-base font-bold text-foreground">
                                Montant TTC
                              </span>
                              <span className="text-xl font-bold text-primary">
                                {formatCurrency(ttc)}
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* Actions : enregistrer le paiement ou annuler (facture EN_ATTENTE uniquement) */}
                        {statut === "EN_ATTENTE" && (
                          <div className="p-4 rounded-xl border border-border bg-card space-y-3">
                            <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                              <Receipt className="w-3.5 h-3.5 text-primary" />
                              <span>Règlement de la facture</span>
                            </div>

                            <ErrorAlert
                              error={actionError}
                              onDismiss={() => setActionError(null)}
                            />

                            <div className="flex flex-col sm:flex-row sm:items-end gap-3">
                              <div className="space-y-1.5 flex-1">
                                <Label htmlFor="facture-date-paiement" className="text-xs">
                                  Date de paiement
                                </Label>
                                <Input
                                  id="facture-date-paiement"
                                  type="date"
                                  value={datePaiementSaisie}
                                  min={(factureDetail.dateEmission || "").slice(0, 10) || undefined}
                                  max={toDateInputString(new Date())}
                                  onChange={(e) => setDatePaiementSaisie(e.target.value)}
                                  disabled={actionEnCours}
                                />
                              </div>
                              <Button
                                type="button"
                                onClick={() => void handleEnregistrerPaiement()}
                                disabled={actionEnCours || !datePaiementSaisie}
                                className="cursor-pointer flex items-center gap-1.5"
                              >
                                {isPaying ? (
                                  <Loader2 className="w-4 h-4 animate-spin" />
                                ) : (
                                  <CheckCircle2 className="w-4 h-4" />
                                )}
                                <span>Enregistrer le paiement</span>
                              </Button>
                            </div>

                            <div className="flex flex-wrap items-center justify-between gap-2 pt-3 border-t border-border">
                              <span className="text-xs text-muted-foreground">
                                L'annulation est définitive : la séance ne pourra pas être refacturée.
                              </span>
                              <Button
                                type="button"
                                variant="destructive"
                                onClick={() => setConfirmAnnulationOpen(true)}
                                disabled={actionEnCours}
                                className="cursor-pointer flex items-center gap-1.5"
                              >
                                <Ban className="w-4 h-4" />
                                <span>Annuler la facture</span>
                              </Button>
                            </div>
                          </div>
                        )}
                      </>
                    );
                  })()}

                <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-border">
                  <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Lock className="w-3.5 h-3.5" />
                    <span>Coordonnées client et prestation figées à l'émission</span>
                  </span>

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

      <ConfirmDialog
        open={confirmAnnulationOpen && factureDetail !== null}
        onOpenChange={(open) => {
          if (!open) setConfirmAnnulationOpen(false);
        }}
        title="Annuler cette facture ?"
        description={
          factureDetail
            ? `La facture ${factureDetail.numero} (${formatCurrency(
                extractMontantTtc(factureDetail)
              )} TTC) passera au statut « Annulée ». Cette action est définitive : la séance associée ne pourra pas être refacturée.`
            : undefined
        }
        confirmLabel="Annuler définitivement"
        cancelLabel="Conserver la facture"
        variant="destructive"
        isLoading={isCancelling}
        onConfirm={handleConfirmAnnulation}
      />
    </div>
  );
}
