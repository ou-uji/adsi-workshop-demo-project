const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

interface ApiError {
  status: number;
  title: string;
  detail: string;
  errors?: Record<string, string>;
}

export class ApiClientError extends Error {
  constructor(
    public readonly status: number,
    public readonly title: string,
    public readonly detail: string,
    public readonly errors?: Record<string, string>,
  ) {
    super(detail);
    this.name = "ApiClientError";
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = (await response.json()) as ApiError;
    throw new ApiClientError(response.status, body.title, body.detail, body.errors);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export const apiClient = {
  get<T>(path: string): Promise<T> {
    return fetch(`${API_BASE_URL}${path}`, {
      credentials: "include",
      headers: { Accept: "application/json" },
    }).then(handleResponse<T>);
  },

  post<T>(path: string, body: unknown): Promise<T> {
    return fetch(`${API_BASE_URL}${path}`, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(body),
    }).then(handleResponse<T>);
  },

  put<T>(path: string, body: unknown): Promise<T> {
    return fetch(`${API_BASE_URL}${path}`, {
      method: "PUT",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(body),
    }).then(handleResponse<T>);
  },

  delete<T>(path: string): Promise<T> {
    return fetch(`${API_BASE_URL}${path}`, {
      method: "DELETE",
      credentials: "include",
      headers: { Accept: "application/json" },
    }).then(handleResponse<T>);
  },
};
