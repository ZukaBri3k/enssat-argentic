export const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
export const TOKEN_KEY = "argentic_token";

export type Role = "SECRETAIRE" | "GESTIONNAIRE";

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

export type StatutSeance = "PLANIFIEE" | "CONFIRMEE" | "TERMINEE" | "ANNULEE";

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

export class ApiError extends Error {
  status: number;
  problem?: ProblemDetail;
  fieldErrors?: Record<string, string>;

  constructor(status: number, message: string, problem?: ProblemDetail) {
    super(message);
    this.name = "ApiError";
    this.status = status;
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

  const response = await fetch(url, {
    ...restOptions,
    headers,
    body:
      body === undefined
        ? undefined
        : body instanceof FormData
          ? body
          : JSON.stringify(body),
  });

  if (!response.ok) {
    let problem: ProblemDetail | undefined;
    try {
      problem = await response.json();
    } catch {
      // Le corps n'est pas du JSON valide
    }

    // Déconnexion automatique sur 401 (sauf lors de la tentative de login elle-même)
    if (response.status === 401 && !path.includes("/auth/login")) {
      clearStoredToken();
      if (unauthorizedListener) {
        unauthorizedListener();
      }
    }

    const fieldErrorsMsg = problem?.errors
      ? Object.entries(problem.errors)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join(", ")
      : null;

    const message =
      problem?.detail ||
      fieldErrorsMsg ||
      problem?.title ||
      `Erreur HTTP ${response.status}`;

    throw new ApiError(response.status, message, problem);
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

// Endpoints Photographes & Clients (pour les listes déroulantes de filtre)
export async function apiGetPhotographes(): Promise<PhotographeSummary[]> {
  return api.get<PhotographeSummary[]>("/photographes");
}

export async function apiGetClients(): Promise<ClientSummary[]> {
  return api.get<ClientSummary[]>("/clients");
}
