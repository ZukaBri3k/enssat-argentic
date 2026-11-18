export const API_URL =
  (typeof import.meta !== "undefined" && import.meta.env && import.meta.env.VITE_API_URL) ||
  (typeof window !== "undefined" && window.location.hostname.includes("evan-saleck.com")
    ? "https://argentic-api.evan-saleck.com"
    : "http://localhost:8080");
export const TOKEN_KEY = "argentic_token";

export type Role = "SECRETAIRE" | "GESTIONNAIRE" | "PHOTOGRAPHE";

export const ROLE_LABELS: Record<Role, string> = {
  SECRETAIRE: "Secrétaire",
  GESTIONNAIRE: "Gestionnaire",
  PHOTOGRAPHE: "Photographe",
};

export const TYPE_CLIENT_LABELS: Record<TypeClient, string> = {
  PARTICULIER: "Particulier",
  ENTREPRISE: "Entreprise",
};

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: Record<string, string>;
}

export interface User {
  id: number;
  email: string;
  role: Role;
}

export interface LoginResponse {
  token: string;
}

export interface Specialite {
  id: number;
  libelle: string;
}

export interface Photographe {
  id: number;
  nom: string;
  email: string;
  actif: boolean;
  specialites: Specialite[];
}

export interface PhotographeRequest {
  nom: string;
  email: string;
  specialiteIds: number[];
  motDePasse?: string;
}

export type StatutSeance = "PLANIFIEE" | "CONFIRMEE" | "TERMINEE" | "ANNULEE";

// Séance qu'une désactivation du photographe annulerait
export interface SeanceAVenir {
  id: number;
  debut: string;
  fin: string;
  lieu: string;
  statut: StatutSeance;
  clientIdentite: string;
  specialite: Specialite;
}

export type StatutPaiement = "EN_ATTENTE" | "PAYEE" | "ANNULEE";

export type TypeClient = "PARTICULIER" | "ENTREPRISE";

export interface PhotographeSummary {
  id: number;
  nom: string;
  email?: string;
  actif?: boolean;
  specialites?: Specialite[];
}

export interface ClientSummary {
  id: number;
  identite?: string;
  nom?: string;
  prenom?: string;
  raisonSociale?: string;
  numeroSirenTva?: string;
  email?: string;
  adressePostale?: string;
  typeClient?: TypeClient;
  type?: TypeClient;
}

export interface FactureSummary {
  id: number;
  numero: string;
  dateEmission?: string;
  montantHt?: number;
  tauxTva?: number;
  montantTtc?: number;
  statutPaiement?: StatutPaiement;
  statut?: StatutPaiement;
  datePaiement?: string | null;
}

export interface SeanceResponse {
  id: number;
  date?: string;
  dateSeance?: string;
  heureDebut?: string;
  dureeMinutes?: number;
  duree?: number;
  debut?: string;
  fin?: string;
  heureFin?: string;
  dateFin?: string;
  lieu: string;
  statut: StatutSeance;
  photographe?: PhotographeSummary;
  photographeId?: number;
  photographeNom?: string;
  photographeEmail?: string;
  client?: ClientSummary;
  clientId?: number;
  clientIdentite?: string;
  clientNom?: string;
  clientEmail?: string;
  specialite?: Specialite;
  specialiteId?: number;
  specialiteLibelle?: string;
  typeSeance?: string;
  facture?: FactureSummary | null;
  factureId?: number | null;
  factureNumero?: string | null;
  factureStatut?: StatutPaiement | null;
}

export interface SeanceFilters {
  du?: string;
  au?: string;
  photographeId?: number | string;
  clientId?: number | string;
  statut?: StatutSeance | "";
}

export interface SeanceRequest {
  date: string;
  heureDebut: string;
  dureeMinutes: number;
  lieu: string;
  specialiteId: number;
  photographeId: number;
  clientId: number;
}

export interface Client {
  id: number;
  type: TypeClient;
  email: string;
  adressePostale: string;
  identite: string;
  // Spécifique Particulier
  nom?: string;
  prenom?: string;
  // Spécifique Entreprise
  raisonSociale?: string;
  numeroSirenTva?: string;
}

export interface ClientRequest {
  type: TypeClient;
  email: string;
  adressePostale: string;
  nom?: string;
  prenom?: string;
  raisonSociale?: string;
  numeroSirenTva?: string;
}

export interface TopPhotographeSeances {
  photographeId?: number;
  id?: number;
  nom: string;
  email?: string;
  nombreSeances: number;
}

export interface TopPhotographeDuree {
  photographeId?: number;
  id?: number;
  nom: string;
  email?: string;
  dureeTotaleMinutes?: number;
  dureeMinutes?: number;
  dureeCumulee?: number;
  duree?: number;
}

export interface ChiffreAffairesResponse {
  du?: string;
  au?: string;
  montantHt?: number;
  montantTtc?: number;
  chiffreAffairesHt?: number;
  chiffreAffairesTtc?: number;
  ht?: number;
  ttc?: number;
  nombreFactures?: number;
}

// Titre affiché à l'utilisateur selon le statut HTTP (status 0 = service injoignable)
function titreErreur(status: number): string {
  if (status === 0) return "Connexion impossible";
  if (status === 400) return "Données invalides";
  if (status === 401) return "Connexion refusée";
  if (status === 403) return "Accès refusé";
  if (status === 404) return "Élément introuvable";
  if (status === 409) return "Action impossible";
  return "Une erreur est survenue";
}

export class ApiError extends Error {
  status: number;
  title: string;
  problem?: ProblemDetail;
  fieldErrors?: Record<string, string>;

  constructor(status: number, message: string, problem?: ProblemDetail, title = titreErreur(status)) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.title = title;
    this.problem = problem;
    this.fieldErrors = problem?.errors;
  }
}

// Listener déclenché automatiquement en cas de 401 sur une route authentifiée
type UnauthorizedListener = () => void;
let unauthorizedListener: UnauthorizedListener | null = null;

export function setOnUnauthorizedListener(listener: UnauthorizedListener | null) {
  unauthorizedListener = listener;
}

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setStoredToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearStoredToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

interface ApiRequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  skipAuth?: boolean;
}

/**
 * Client API centralisé :
 * - Ajoute automatiquement le header Authorization: Bearer <token>
 * - Déconnecte automatiquement sur 401 (hors /auth/login)
 * - Extrait les messages et erreurs par champ des réponses ProblemDetail (RFC 9457)
 */
export async function apiFetch<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { body, skipAuth = false, headers: customHeaders, ...restOptions } = options;

  const headers = new Headers(customHeaders);
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json, application/problem+json");
  }
  if (body !== undefined && !(body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (!skipAuth) {
    const token = getStoredToken();
    if (token && !headers.has("Authorization")) {
      headers.set("Authorization", `Bearer ${token}`);
    }
  }

  const url = path.startsWith("http") ? path : `${API_URL}${path.startsWith("/") ? path : `/${path}`}`;

  let response: Response;
  try {
    response = await fetch(url, {
      ...restOptions,
      headers,
      body:
        body === undefined
          ? undefined
          : body instanceof FormData
            ? body
            : JSON.stringify(body),
    });
  } catch {
    // Erreur réseau : le message natif (« Failed to fetch »…) n'est pas destiné à l'utilisateur
    throw new ApiError(
      0,
      "Le service est momentanément indisponible. Vérifiez votre connexion puis réessayez."
    );
  }

  if (!response.ok) {
    let problem: ProblemDetail | undefined;
    try {
      problem = await response.json();
    } catch {
      // Le corps n'est pas du JSON valide
    }

    // Déconnexion automatique sur 401 (sauf lors de la tentative de login elle-même)
    const sessionExpiree = response.status === 401 && !path.includes("/auth/login");
    if (sessionExpiree) {
      clearStoredToken();
      if (unauthorizedListener) {
        unauthorizedListener();
      }
    }

    // Le détail d'une 401 de session, d'une 404 (contient l'identifiant technique) ou d'une 5xx
    // n'est pas destiné à l'utilisateur : message générique à la place.
    // Les erreurs par champ sont listées à part par ErrorAlert / les toasts.
    let message = problem?.detail || "Une erreur inattendue est survenue. Veuillez réessayer.";
    if (sessionExpiree) {
      message = "Votre session a expiré. Veuillez vous reconnecter.";
    } else if (response.status === 404) {
      message = "L'élément demandé n'existe pas ou a été supprimé.";
    } else if (response.status >= 500) {
      message = "Une erreur inattendue est survenue. Veuillez réessayer.";
    }

    throw new ApiError(
      response.status,
      message,
      problem,
      sessionExpiree ? "Session expirée" : undefined
    );
  }

  // Gestion des réponses 204 No Content ou corps vide
  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}

export const api = {
  get: <T>(path: string, options?: ApiRequestOptions) =>
    apiFetch<T>(path, { ...options, method: "GET" }),
  post: <T>(path: string, body?: unknown, options?: ApiRequestOptions) =>
    apiFetch<T>(path, { ...options, method: "POST", body }),
  put: <T>(path: string, body?: unknown, options?: ApiRequestOptions) =>
    apiFetch<T>(path, { ...options, method: "PUT", body }),
  patch: <T>(path: string, body?: unknown, options?: ApiRequestOptions) =>
    apiFetch<T>(path, { ...options, method: "PATCH", body }),
  delete: <T>(path: string, options?: ApiRequestOptions) =>
    apiFetch<T>(path, { ...options, method: "DELETE" }),
};

// Endpoints d'authentification & socle
export async function apiLogin(email: string, motDePasse: string): Promise<LoginResponse> {
  return api.post<LoginResponse>("/auth/login", { email, motDePasse }, { skipAuth: true });
}

export async function apiGetMe(token?: string): Promise<User> {
  const headers = token ? { Authorization: `Bearer ${token}` } : undefined;
  return api.get<User>("/auth/me", { headers });
}

export async function apiGetSpecialites(): Promise<Specialite[]> {
  return api.get<Specialite[]>("/specialites");
}

// Endpoints Photographes
export async function apiGetPhotographes(actif?: boolean): Promise<Photographe[]> {
  const query = actif === undefined ? "" : `?${new URLSearchParams({ actif: String(actif) })}`;
  return api.get<Photographe[]>(`/photographes${query}`);
}

export async function apiGetPhotographe(id: number): Promise<Photographe> {
  return api.get<Photographe>(`/photographes/${id}`);
}

export async function apiCreatePhotographe(request: PhotographeRequest): Promise<Photographe> {
  return api.post<Photographe>("/photographes", request);
}

export async function apiUpdatePhotographe(id: number, request: PhotographeRequest): Promise<Photographe> {
  return api.put<Photographe>(`/photographes/${id}`, request);
}

export async function apiDeletePhotographe(id: number): Promise<void> {
  return api.delete<void>(`/photographes/${id}`);
}

export async function apiGetSeancesAVenir(id: number): Promise<SeanceAVenir[]> {
  return api.get<SeanceAVenir[]>(`/photographes/${id}/seances-a-venir`);
}

export async function apiDesactiverPhotographe(id: number): Promise<Photographe> {
  return api.post<Photographe>(`/photographes/${id}/desactivation`);
}

export async function apiActiverPhotographe(id: number): Promise<Photographe> {
  return api.post<Photographe>(`/photographes/${id}/activation`);
}

// Endpoints Clients
export async function apiGetClients(): Promise<Client[]> {
  return api.get<Client[]>("/clients");
}

export async function apiGetClient(id: number): Promise<Client> {
  return api.get<Client>(`/clients/${id}`);
}

export async function apiCreateClient(request: ClientRequest): Promise<Client> {
  return api.post<Client>("/clients", request);
}

export async function apiUpdateClient(id: number, request: ClientRequest): Promise<Client> {
  return api.put<Client>(`/clients/${id}`, request);
}

export async function apiDeleteClient(id: number): Promise<void> {
  return api.delete<void>(`/clients/${id}`);
}

export async function apiGetStatsPing(): Promise<{ status: string }> {
  return api.get<{ status: string }>("/stats/ping");
}

// Endpoints Statistiques (rôle GESTIONNAIRE uniquement)
export async function apiGetTopPhotographesSeances(): Promise<TopPhotographeSeances[]> {
  return api.get<TopPhotographeSeances[]>("/stats/top-photographes/seances");
}

export async function apiGetTopPhotographesDuree(): Promise<TopPhotographeDuree[]> {
  return api.get<TopPhotographeDuree[]>("/stats/top-photographes/duree");
}

export async function apiGetChiffreAffaires(
  du: string,
  au: string
): Promise<ChiffreAffairesResponse> {
  const params = new URLSearchParams({ du, au });
  return api.get<ChiffreAffairesResponse>(`/stats/chiffre-affaires?${params.toString()}`);
}

// Endpoints Séances & Planning
export async function apiGetSeances(filters: SeanceFilters = {}): Promise<SeanceResponse[]> {
  const params = new URLSearchParams();
  if (filters.du) {
    params.set("du", filters.du);
  }
  if (filters.au) {
    params.set("au", filters.au);
  }
  if (filters.photographeId !== undefined && filters.photographeId !== "") {
    const idStr = String(filters.photographeId);
    params.set("idPhotographe", idStr);
    params.set("photographeId", idStr);
    params.set("photographe", idStr);
  }
  if (filters.clientId !== undefined && filters.clientId !== "") {
    const idStr = String(filters.clientId);
    params.set("idClient", idStr);
    params.set("clientId", idStr);
    params.set("client", idStr);
  }
  if (filters.statut) {
    params.set("statutSeance", filters.statut);
    params.set("statut", filters.statut);
  }
  const query = params.toString();
  return api.get<SeanceResponse[]>(query ? `/seances?${query}` : "/seances");
}

export async function apiGetSeanceById(id: number): Promise<SeanceResponse> {
  return api.get<SeanceResponse>(`/seances/${id}`);
}

export async function apiCreateSeance(request: SeanceRequest): Promise<SeanceResponse> {
  const payload = {
    ...request,
    dateSeance: request.date,
    duree: request.dureeMinutes,
    idSpecialite: request.specialiteId,
    idPhotographe: request.photographeId,
    idClient: request.clientId,
  };
  return api.post<SeanceResponse>("/seances", payload);
}

export async function apiUpdateSeance(
  id: number,
  request: SeanceRequest
): Promise<SeanceResponse> {
  const payload = {
    ...request,
    dateSeance: request.date,
    duree: request.dureeMinutes,
    idSpecialite: request.specialiteId,
    idPhotographe: request.photographeId,
    idClient: request.clientId,
  };
  return api.put<SeanceResponse>(`/seances/${id}`, payload);
}

export async function apiChangerStatutSeance(
  id: number,
  statut: StatutSeance
): Promise<SeanceResponse> {
  return api.patch<SeanceResponse>(`/seances/${id}/statut`, { statut });
}

export async function apiDeleteSeance(id: number): Promise<void> {
  return api.delete<void>(`/seances/${id}`);
}

// Endpoints Factures
export interface CoordonneesFacturationDto {
  identite: string;
  adressePostale: string;
  email: string;
  numeroSirenTva?: string | null;
}

export interface PrestationFactureeDto {
  date: string;
  heureDebut: string;
  dureeMinutes: number;
  typeSeance: string;
  nomPhotographe: string;
}

export interface FactureResponse {
  id: number;
  numero: string;
  seanceId?: number;
  seance?: { id: number };
  dateEmission: string;
  montantHt: number;
  tauxTva: number;
  montantTtc: number;
  statutPaiement?: StatutPaiement;
  statut?: StatutPaiement;
  datePaiement?: string | null;
  coordonneesClient?: CoordonneesFacturationDto;
  prestation?: PrestationFactureeDto;
  clientIdentite?: string;
  clientAdressePostale?: string;
  clientEmail?: string;
  clientNumeroSirenTva?: string | null;
  prestationDate?: string;
  prestationHeureDebut?: string;
  prestationDureeMinutes?: number;
  prestationType?: string;
  typeSeance?: string;
  photographeNom?: string;
  nomPhotographe?: string;
}

export interface FactureRequest {
  montantHt: number;
  tauxTva?: number | null;
}

export interface FactureFilters {
  statut?: StatutPaiement | "";
  du?: string;
  au?: string;
}

export async function apiGenererFacture(
  seanceId: number,
  request: FactureRequest
): Promise<FactureResponse> {
  const payload: Record<string, number> = { montantHt: request.montantHt };
  if (request.tauxTva !== undefined && request.tauxTva !== null) {
    payload.tauxTva = request.tauxTva;
  }
  return api.post<FactureResponse>(`/seances/${seanceId}/facture`, payload);
}

export async function apiGetFactures(
  filters: FactureFilters = {}
): Promise<FactureResponse[]> {
  const params = new URLSearchParams();
  if (filters.statut) {
    params.set("statut", filters.statut);
    params.set("statutPaiement", filters.statut);
  }
  if (filters.du) {
    params.set("du", filters.du);
  }
  if (filters.au) {
    params.set("au", filters.au);
  }
  const query = params.toString();
  return api.get<FactureResponse[]>(query ? `/factures?${query}` : "/factures");
}

export async function apiGetFactureById(id: number): Promise<FactureResponse> {
  return api.get<FactureResponse>(`/factures/${id}`);
}

export async function apiEnregistrerPaiementFacture(
  id: number,
  datePaiement: string
): Promise<FactureResponse> {
  return api.post<FactureResponse>(`/factures/${id}/paiement`, { datePaiement });
}

export async function apiAnnulerFacture(id: number): Promise<FactureResponse> {
  return api.post<FactureResponse>(`/factures/${id}/annulation`);
}

/**
 * Extrait le nom du fichier depuis l'en-tête HTTP Content-Disposition (RFC 6266 / RFC 5987)
 */
export function extractFilenameFromContentDisposition(
  contentDisposition: string | null,
  fallback: string
): string {
  if (!contentDisposition) return fallback;

  // Format RFC 5987: filename*=UTF-8''nom_fichier.pdf
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match && utf8Match[1]) {
    try {
      const decoded = decodeURIComponent(utf8Match[1].trim());
      if (decoded) return decoded;
    } catch {
      // Échec de décodage, continuer vers format standard
    }
  }

  // Format standard: filename="nom_fichier.pdf" ou filename=nom_fichier.pdf
  const standardMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  if (standardMatch && standardMatch[1]) {
    const candidate = standardMatch[1].trim();
    if (candidate) return candidate;
  }

  return fallback;
}

/**
 * Déclenche le téléchargement d'un Blob dans le navigateur
 */
export function triggerBlobDownload(blob: Blob, filename: string): void {
  const blobUrl = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(blobUrl);
}

export interface FacturePdfDownloadResult {
  blob: Blob;
  filename: string;
}

/**
 * Télécharge le PDF d'une facture via une requête authentifiée (GET /factures/{id}/pdf)
 * et déclenche automatiquement l'enregistrement du fichier (Blob) côté client.
 * En cas d'erreur backend (404, 401, 403, 500...), l'erreur est renvoyée en ApiError avec ProblemDetail.
 */
export async function apiDownloadFacturePdf(
  id: number,
  fallbackNumero?: string
): Promise<FacturePdfDownloadResult> {
  const token = getStoredToken();
  const headers = new Headers();
  headers.set("Accept", "application/pdf, application/problem+json");
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const path = `/factures/${id}/pdf`;
  const url = path.startsWith("http")
    ? path
    : `${API_URL}${path.startsWith("/") ? path : `/${path}`}`;

  let response: Response;
  try {
    response = await fetch(url, {
      method: "GET",
      headers,
    });
  } catch {
    throw new ApiError(
      0,
      "Le service est momentanément indisponible. Vérifiez votre connexion puis réessayez."
    );
  }

  if (!response.ok) {
    let problem: ProblemDetail | undefined;
    try {
      problem = (await response.json()) as ProblemDetail;
    } catch {
      // Corps non-JSON
    }

    const sessionExpiree = response.status === 401;
    if (sessionExpiree) {
      clearStoredToken();
      if (unauthorizedListener) {
        unauthorizedListener();
      }
    }

    let message = problem?.detail || "Impossible de télécharger la facture.";
    if (sessionExpiree) {
      message = "Votre session a expiré. Veuillez vous reconnecter.";
    } else if (response.status === 404) {
      message = problem?.detail || "La facture demandée n'existe pas ou a été supprimée.";
    } else if (response.status === 403) {
      message = problem?.detail || "Vous n'avez pas l'autorisation de télécharger cette facture.";
    } else if (response.status >= 500) {
      message = "Une erreur inattendue est survenue lors de la génération du PDF. Veuillez réessayer.";
    }

    throw new ApiError(
      response.status,
      message,
      problem,
      sessionExpiree ? "Session expirée" : titreErreur(response.status)
    );
  }

  const blob = await response.blob();
  const rawNumero = fallbackNumero ? fallbackNumero.trim() : "";
  const defaultFilename = rawNumero
    ? (rawNumero.toLowerCase().endsWith(".pdf") ? rawNumero : `${rawNumero}.pdf`)
    : `facture-${id}.pdf`;

  const contentDisposition = response.headers.get("Content-Disposition");
  const extractedFilename = extractFilenameFromContentDisposition(contentDisposition, defaultFilename);
  const finalFilename = extractedFilename.toLowerCase().endsWith(".pdf")
    ? extractedFilename
    : `${extractedFilename}.pdf`;

  triggerBlobDownload(blob, finalFilename);

  return { blob, filename: finalFilename };
}
