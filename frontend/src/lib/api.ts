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

export type TypeClient = "PARTICULIER" | "ENTREPRISE";

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
