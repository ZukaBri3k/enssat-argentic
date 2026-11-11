export const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

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
  role: "SECRETAIRE" | "GESTIONNAIRE";
}

export interface LoginResponse {
  token: string;
}

export class ApiError extends Error {
  status: number;
  problem?: ProblemDetail;

  constructor(status: number, message: string, problem?: ProblemDetail) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.problem = problem;
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let problem: ProblemDetail | undefined;
    try {
      problem = await response.json();
    } catch {
      // response might not be JSON
    }

    const message =
      problem?.detail ||
      problem?.title ||
      (problem?.errors ? Object.values(problem.errors).join(", ") : null) ||
      `Erreur HTTP ${response.status}`;

    throw new ApiError(response.status, message, problem);
  }

  return response.json() as Promise<T>;
}

export async function apiLogin(email: string, motDePasse: string): Promise<LoginResponse> {
  const response = await fetch(`${API_URL}/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({ email, motDePasse }),
  });

  return handleResponse<LoginResponse>(response);
}

export async function apiGetMe(token: string): Promise<User> {
  const response = await fetch(`${API_URL}/auth/me`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
    },
  });

  return handleResponse<User>(response);
}

export interface Specialite {
  id: number;
  libelle: string;
}

export async function apiGetSpecialites(token: string): Promise<Specialite[]> {
  const response = await fetch(`${API_URL}/specialites`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
    },
  });

  return handleResponse<Specialite[]>(response);
}

export async function apiGetStatsPing(token: string): Promise<{ status: string }> {
  const response = await fetch(`${API_URL}/stats/ping`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
    },
  });

  return handleResponse<{ status: string }>(response);
}

