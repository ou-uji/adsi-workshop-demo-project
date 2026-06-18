export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface ApiErrorResponse {
  status: number;
  title: string;
  detail: string;
  errors?: Record<string, string>;
}
