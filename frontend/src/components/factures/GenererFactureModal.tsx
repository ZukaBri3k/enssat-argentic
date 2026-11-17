import React, { useEffect, useState } from "react";
import {
  ApiError,
  apiGenererFacture,
  type FactureResponse,
  type SeanceResponse,
} from "@/lib/api";
import { TONS } from "@/lib/tons";
import { ErrorAlert } from "@/components/shared/ErrorAlert";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Calculator,
  Calendar,
  Camera,
  Clock,
  Euro,
  FileText,
  Loader2,
  MapPin,
  Receipt,
  Tag,
  User,
  X,
} from "lucide-react";

export interface GenererFactureModalProps {
  seance: SeanceResponse | null;
  onClose: () => void;
  onSuccess: (facture: FactureResponse, seanceId: number) => void;
}

const MONTANT_HT_PRESETS = [
  { value: 150, label: "150 €" },
  { value: 250, label: "250 €" },
  { value: 450, label: "450 €" },
  { value: 750, label: "750 €" },
  { value: 1200, label: "1200 €" },
  { value: 1800, label: "1800 €" },
];

const TAUX_TVA_PRESETS = [
  { value: 20, inputValue: "20.00", label: "20 %" },
  { value: 10, inputValue: "10.00", label: "10 %" },
  { value: 5.5, inputValue: "5.50", label: "5,5 %" },
  { value: 0, inputValue: "0.00", label: "0 %" },
];

const DEFAULT_TAUX_TVA = 20;

const CURRENCY_FORMATTER = new Intl.NumberFormat("fr-FR", {
  style: "currency",
  currency: "EUR",
});

const DATE_LONG_FORMATTER = new Intl.DateTimeFormat("fr-FR", {
  day: "2-digit",
  month: "long",
  year: "numeric",
});

function formatCurrency(value: number): string {
  return CURRENCY_FORMATTER.format(value);
}

function parseDecimal(raw: string): number {
  const normalized = raw.trim().replace(",", ".");
  if (!normalized) return Number.NaN;
  return Number(normalized);
}

function formatSeanceDate(seance: SeanceResponse): string {
  const raw = seance.date ?? seance.dateSeance ?? seance.debut;
  if (!raw) return "—";
  const datePart = raw.includes("T") ? raw.split("T")[0] : raw.slice(0, 10);
  const [y, m, d] = datePart.split("-").map(Number);
  if (!y || !m || !d) return raw;
  const parsed = new Date(y, m - 1, d);
  if (Number.isNaN(parsed.getTime())) return raw;
  return new Intl.DateTimeFormat("fr-FR", {
    weekday: "short",
    day: "2-digit",
    month: "long",
    year: "numeric",
  }).format(parsed);
}

function formatSeanceHeure(seance: SeanceResponse): string {
  if (seance.heureDebut) return seance.heureDebut.slice(0, 5);
  if (seance.debut && seance.debut.includes("T")) {
    return seance.debut.split("T")[1].slice(0, 5);
  }
  return "—";
}

function formatSeanceDuree(seance: SeanceResponse): string {
  const minutes = seance.dureeMinutes ?? seance.duree;
  if (!minutes || minutes <= 0) return "—";
  const hours = Math.floor(minutes / 60);
  const rem = minutes % 60;
  if (hours === 0) return `${rem} min`;
  if (rem === 0) return `${hours} h (${minutes} min)`;
  return `${hours} h ${String(rem).padStart(2, "0")} (${minutes} min)`;
}

function extractSpecialite(seance: SeanceResponse): string {
  return (
    seance.specialite?.libelle ??
    seance.specialiteLibelle ??
    seance.typeSeance ??
    "Spécialité non renseignée"
  );
}

function extractClientNom(seance: SeanceResponse): string {
  if (seance.clientIdentite) return seance.clientIdentite;
  if (seance.client?.identite) return seance.client.identite;
  if (seance.clientNom) return seance.clientNom;
  if (seance.client?.raisonSociale) return seance.client.raisonSociale;
  const fullName = [seance.client?.nom, seance.client?.prenom]
    .filter(Boolean)
    .join(" ")
    .trim();
  return fullName || "Client non renseigné";
}

function extractClientEmail(seance: SeanceResponse): string | undefined {
  return seance.clientEmail ?? seance.client?.email;
}

function extractPhotographeNom(seance: SeanceResponse): string {
  return (
    seance.photographeNom ??
    seance.photographe?.nom ??
    "Photographe non renseigné"
  );
}

interface GenererFactureModalContentProps {
  seance: SeanceResponse;
  onClose: () => void;
  onSuccess: (facture: FactureResponse, seanceId: number) => void;
}

function GenererFactureModalContent({
  seance,
  onClose,
  onSuccess,
}: GenererFactureModalContentProps) {
  const { success, showApiError } = useToast();

  const [montantHt, setMontantHt] = useState("");
  const [tauxTva, setTauxTva] = useState("20.00");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<unknown>(null);
  const [localFieldErrors, setLocalFieldErrors] = useState<{
    montantHt?: boolean;
    tauxTva?: boolean;
  }>({});

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !isSubmitting) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isSubmitting, onClose]);

  const parsedHt = parseDecimal(montantHt);
  const isTvaEmpty = tauxTva.trim() === "";
  const parsedTva = isTvaEmpty ? DEFAULT_TAUX_TVA : parseDecimal(tauxTva);

  const isPreviewValid =
    Number.isFinite(parsedHt) &&
    parsedHt > 0 &&
    Number.isFinite(parsedTva) &&
    parsedTva >= 0 &&
    parsedTva <= 100;

  const previewTtc = isPreviewValid
    ? Math.round(parsedHt * (1 + parsedTva / 100) * 100) / 100
    : null;
  const previewTvaAmount =
    previewTtc !== null
      ? Math.round((previewTtc - parsedHt) * 100) / 100
      : null;

  const apiFieldErrors =
    submitError instanceof ApiError ? submitError.fieldErrors : undefined;

  const hasMontantHtError = Boolean(
    localFieldErrors.montantHt ||
      apiFieldErrors?.montantHt ||
      apiFieldErrors?.montantHT
  );
  const hasTauxTvaError = Boolean(
    localFieldErrors.tauxTva ||
      apiFieldErrors?.tauxTva ||
      apiFieldErrors?.tauxTVA
  );

  const todayFormatted = DATE_LONG_FORMATTER.format(new Date());
  const clientEmail = extractClientEmail(seance);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);
    setLocalFieldErrors({});

    const ht = parseDecimal(montantHt);
    if (!Number.isFinite(ht) || ht <= 0) {
      setLocalFieldErrors({ montantHt: true });
      setSubmitError(
        "Le montant HT est obligatoire et doit être strictement supérieur à 0 €."
      );
      return;
    }

    const tvaTrimmed = tauxTva.trim();
    let tvaRate: number | undefined;
    if (tvaTrimmed !== "") {
      const parsedRate = parseDecimal(tvaTrimmed);
      if (!Number.isFinite(parsedRate) || parsedRate < 0 || parsedRate > 100) {
        setLocalFieldErrors({ tauxTva: true });
        setSubmitError(
          "Le taux de TVA doit être compris entre 0 % et 100 %."
        );
        return;
      }
      tvaRate = parsedRate;
    }

    setIsSubmitting(true);
    try {
      const created = await apiGenererFacture(seance.id, {
        montantHt: ht,
        tauxTva: tvaRate,
      });

      const ttcLabel =
        typeof created.montantTtc === "number"
          ? ` (${formatCurrency(created.montantTtc)} TTC)`
          : "";
      success(
        "Facture générée",
        `La facture ${created.numero}${ttcLabel} a été émise pour la séance du ${formatSeanceDate(seance)} (${extractClientNom(seance)}).`
      );
      onSuccess(created, seance.id);
    } catch (err) {
      setSubmitError(err);
      showApiError(err, "Impossible de générer la facture");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="generer-facture-modal-title"
      className="fixed inset-0 z-50 flex justify-center overflow-y-auto bg-black/50 backdrop-blur-xs p-4"
      onClick={() => {
        if (!isSubmitting) onClose();
      }}
    >
      <div
        className="my-auto w-full max-w-2xl text-left"
        onClick={(e) => e.stopPropagation()}
      >
        <Card className="shadow-2xl border-border">
          <CardHeader className="border-b border-border pb-4">
            <div className="flex items-start justify-between gap-4">
              <div className="space-y-1 min-w-0">
                <CardTitle
                  id="generer-facture-modal-title"
                  className="text-lg flex items-center gap-2"
                >
                  <Receipt className="w-5 h-5 text-primary shrink-0" />
                  <span>Générer la facture</span>
                </CardTitle>
                <CardDescription>
                  Saisissez le montant HT et vérifiez le taux de TVA. Les
                  coordonnées du client et les détails de la prestation seront
                  figés à l'émission.
                </CardDescription>
              </div>

              <button
                type="button"
                onClick={onClose}
                disabled={isSubmitting}
                className="cursor-pointer shrink-0 p-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted disabled:opacity-50"
                aria-label="Fermer la modale de facturation"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </CardHeader>

          <form onSubmit={handleSubmit} noValidate>
            <CardContent className="pt-5 space-y-5">
              <ErrorAlert
                error={submitError}
                onDismiss={() => {
                  setSubmitError(null);
                  setLocalFieldErrors({});
                }}
              />

              {/* Récapitulatif figé de la séance */}
              <div className="p-4 rounded-xl border border-border bg-muted/40 space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                    <FileText className="w-3.5 h-3.5 text-primary shrink-0" />
                    <span>Prestation & client figés dans la facture</span>
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
                  <div className="space-y-1.5 min-w-0">
                    <div className="flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                      <Tag className="w-3.5 h-3.5 shrink-0" />
                      <span>Spécialité :</span>
                      <span className="min-w-0 break-words font-semibold text-foreground">
                        {extractSpecialite(seance)}
                      </span>
                    </div>
                    <div className="flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                      <Calendar className="w-3.5 h-3.5 shrink-0" />
                      <span>Date :</span>
                      <span className="min-w-0 break-words font-medium text-foreground capitalize">
                        {formatSeanceDate(seance)}
                      </span>
                    </div>
                    <div className="flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                      <Clock className="w-3.5 h-3.5 shrink-0" />
                      <span>Heure & durée :</span>
                      <span className="min-w-0 break-words font-medium text-foreground">
                        {formatSeanceHeure(seance)} · {formatSeanceDuree(seance)}
                      </span>
                    </div>
                    <div className="flex items-start gap-1.5 text-xs text-muted-foreground">
                      <MapPin className="w-3.5 h-3.5 shrink-0 mt-0.5" />
                      <span>Lieu :</span>
                      <span className="min-w-0 break-words font-medium text-foreground">
                        {seance.lieu || "Non renseigné"}
                      </span>
                    </div>
                  </div>

                  <div className="space-y-1.5 min-w-0">
                    <div className="flex items-start gap-1.5 text-xs text-muted-foreground">
                      <User className="w-3.5 h-3.5 shrink-0 mt-0.5" />
                      <div className="min-w-0">
                        <span>Client : </span>
                        <span className="min-w-0 break-words font-semibold text-foreground">
                          {extractClientNom(seance)}
                        </span>
                        {clientEmail && (
                          <div className="text-[11px] text-muted-foreground truncate">
                            {clientEmail}
                          </div>
                        )}
                      </div>
                    </div>
                    <div className="flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                      <Camera className="w-3.5 h-3.5 shrink-0" />
                      <span>Photographe :</span>
                      <span className="min-w-0 break-words font-semibold text-foreground">
                        {extractPhotographeNom(seance)}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Saisie Montant HT & Taux de TVA */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label
                    htmlFor="facture-montant-ht"
                    className="flex items-center gap-1.5"
                  >
                    <Euro className="w-3.5 h-3.5 text-muted-foreground" />
                    <span>Montant HT (€) *</span>
                  </Label>
                  <Input
                    id="facture-montant-ht"
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    placeholder="Ex. 450.00"
                    value={montantHt}
                    disabled={isSubmitting}
                    onChange={(e) => {
                      setMontantHt(e.target.value);
                      if (localFieldErrors.montantHt) {
                        setLocalFieldErrors((prev) => ({
                          ...prev,
                          montantHt: false,
                        }));
                      }
                    }}
                    aria-invalid={hasMontantHtError ? true : undefined}
                  />
                  <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
                    {MONTANT_HT_PRESETS.map((preset) => {
                      const isSelected =
                        montantHt.trim() !== "" &&
                        parseDecimal(montantHt) === preset.value;
                      return (
                        <button
                          key={preset.value}
                          type="button"
                          disabled={isSubmitting}
                          onClick={() => {
                            setMontantHt(String(preset.value));
                            setLocalFieldErrors((prev) => ({
                              ...prev,
                              montantHt: false,
                            }));
                          }}
                          className={`cursor-pointer h-9 sm:h-auto px-3 sm:px-2 py-0.5 rounded-md text-xs font-medium border transition-colors disabled:opacity-50 ${
                            isSelected
                              ? "bg-primary text-primary-foreground border-primary"
                              : "bg-background text-muted-foreground border-border hover:text-foreground"
                          }`}
                        >
                          {preset.label}
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div className="space-y-2">
                  <Label
                    htmlFor="facture-taux-tva"
                    className="flex items-center gap-1.5"
                  >
                    <Calculator className="w-3.5 h-3.5 text-muted-foreground" />
                    <span>Taux de TVA (%)</span>
                  </Label>
                  <Input
                    id="facture-taux-tva"
                    type="number"
                    step="0.01"
                    min="0"
                    max="100"
                    placeholder="20.00"
                    value={tauxTva}
                    disabled={isSubmitting}
                    onChange={(e) => {
                      setTauxTva(e.target.value);
                      if (localFieldErrors.tauxTva) {
                        setLocalFieldErrors((prev) => ({
                          ...prev,
                          tauxTva: false,
                        }));
                      }
                    }}
                    aria-invalid={hasTauxTvaError ? true : undefined}
                  />
                  <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
                    {TAUX_TVA_PRESETS.map((preset) => {
                      const isSelected =
                        !isTvaEmpty &&
                        parseDecimal(tauxTva) === preset.value;
                      return (
                        <button
                          key={preset.inputValue}
                          type="button"
                          disabled={isSubmitting}
                          onClick={() => {
                            setTauxTva(preset.inputValue);
                            setLocalFieldErrors((prev) => ({
                              ...prev,
                              tauxTva: false,
                            }));
                          }}
                          className={`cursor-pointer h-9 sm:h-auto px-3 sm:px-2 py-0.5 rounded-md text-xs font-medium border transition-colors disabled:opacity-50 ${
                            isSelected
                              ? "bg-primary text-primary-foreground border-primary"
                              : "bg-background text-muted-foreground border-border hover:text-foreground"
                          }`}
                        >
                          {preset.label}
                        </button>
                      );
                    })}
                  </div>
                  <p className="text-[11px] text-muted-foreground leading-snug">
                    <strong>20 %</strong> par défaut si laissé vide.
                  </p>
                </div>
              </div>

              {/* Aperçu du TTC en temps réel */}
              <div className="p-4 rounded-xl border border-primary/25 bg-primary/5 space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-xs font-semibold uppercase tracking-wider text-primary flex items-center gap-1.5">
                    <Calculator className="w-3.5 h-3.5" />
                    <span>Aperçu du TTC</span>
                  </span>
                  <div className="flex flex-wrap items-center gap-2 text-xs">
                    <span className="text-muted-foreground">
                      Émission :{" "}
                      <strong className="text-foreground">
                        {todayFormatted}
                      </strong>
                    </span>
                    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold border ${TONS.attention.badge}`}>
                      En attente
                    </span>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
                  <div className="rounded-lg bg-background/80 border border-border px-3 py-2">
                    <div className="text-[11px] text-muted-foreground">
                      Montant HT
                    </div>
                    <div className="text-sm font-semibold text-foreground tabular-nums">
                      {isPreviewValid ? formatCurrency(parsedHt) : "—"}
                    </div>
                  </div>

                  <div className="rounded-lg bg-background/80 border border-border px-3 py-2">
                    <div className="text-[11px] text-muted-foreground">
                      Montant TVA (
                      {Number.isFinite(parsedTva)
                        ? `${parsedTva.toLocaleString("fr-FR", {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })} %`
                        : "—"}
                      {isTvaEmpty ? ", par défaut" : ""})
                    </div>
                    <div className="text-sm font-semibold text-foreground tabular-nums">
                      {previewTvaAmount !== null
                        ? formatCurrency(previewTvaAmount)
                        : "—"}
                    </div>
                  </div>

                  <div className="rounded-lg bg-background border border-primary/40 px-3 py-2">
                    <div className="text-[11px] font-medium text-primary">
                      Montant TTC
                    </div>
                    <div className="text-base font-bold text-primary tabular-nums">
                      {previewTtc !== null ? formatCurrency(previewTtc) : "—"}
                    </div>
                  </div>
                </div>
              </div>

              <div className="flex flex-col-reverse gap-2 pt-3 border-t border-border sm:flex-row sm:justify-end">
                <Button
                  type="button"
                  variant="outline"
                  onClick={onClose}
                  disabled={isSubmitting}
                  className="cursor-pointer h-9 w-full sm:w-auto"
                >
                  Annuler
                </Button>
                <Button
                  type="submit"
                  disabled={isSubmitting}
                  className="cursor-pointer gap-1.5 h-9 w-full sm:w-auto"
                >
                  {isSubmitting ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <Receipt className="w-4 h-4" />
                  )}
                  <span>
                    {isSubmitting
                      ? "Génération en cours..."
                      : "Générer la facture"}
                  </span>
                </Button>
              </div>
            </CardContent>
          </form>
        </Card>
      </div>
    </div>
  );
}

export function GenererFactureModal({
  seance,
  onClose,
  onSuccess,
}: GenererFactureModalProps) {
  if (!seance) return null;

  return (
    <GenererFactureModalContent
      key={seance.id}
      seance={seance}
      onClose={onClose}
      onSuccess={onSuccess}
    />
  );
}
