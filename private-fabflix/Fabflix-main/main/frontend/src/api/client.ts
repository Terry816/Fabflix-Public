import type {
  ApiMessage,
  CartResponse,
  DatabaseMetadata,
  Genre,
  Movie,
  OrderSummaryResponse,
  StarMovie,
  Suggestion
} from '../types';

export class ApiError extends Error {
  status: number;
  body: unknown;

  constructor(message: string, status: number, body: unknown) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

type QueryValue = string | number | boolean | null | undefined;
type FormValue = string | number | null | undefined;

function queryString(params: Record<string, QueryValue>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      search.set(key, String(value));
    }
  });
  const value = search.toString();
  return value ? `?${value}` : '';
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    credentials: 'include',
    ...init,
    headers: {
      ...(init.headers ?? {})
    }
  });

  const contentType = response.headers.get('content-type') ?? '';
  const body = contentType.includes('application/json') ? await response.json() : await response.text();

  if (!response.ok) {
    const message =
      typeof body === 'object' && body !== null && 'message' in body
        ? String((body as { message: unknown }).message)
        : response.statusText;
    throw new ApiError(message, response.status, body);
  }

  return body as T;
}

function formBody(data: Record<string, FormValue>): URLSearchParams {
  const body = new URLSearchParams();
  Object.entries(data).forEach(([key, value]) => {
    if (value !== null && value !== undefined) {
      body.set(key, String(value));
    }
  });
  return body;
}

function postForm<T>(path: string, data: Record<string, FormValue>): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: formBody(data)
  });
}

export const api = {
  login: (email: string, password: string) => postForm<ApiMessage>('/api/login', { username: email, password }),
  employeeLogin: (email: string, password: string) => postForm<ApiMessage>('/api/employee-login', { email, password }),
  logout: () => postForm<ApiMessage>('/api/logout', {}),
  index: () => request<{ firstName?: string }>('/api/index'),
  genres: () => request<Genre[]>('/api/genres'),
  autocomplete: (query: string) => request<Suggestion[]>(`/api/autocomplete${queryString({ query })}`),
  movies: (params: Record<string, QueryValue>) => request<Movie[]>(`/api/search${queryString(params)}`),
  fullTextMovies: (params: Record<string, QueryValue>) => request<Movie[]>(`/api/fulltext-search${queryString(params)}`),
  movie: (id: string) => request<Movie[]>(`/api/single-movie${queryString({ id })}`),
  star: (id: string) => request<StarMovie[]>(`/api/single-star${queryString({ id })}`),
  addToCart: (movieId: string) => postForm<ApiMessage>('/api/add-to-cart', { movieId }),
  cart: () => request<CartResponse>('/api/shopping-cart'),
  updateCart: (movieId: string, quantity: number) => postForm<ApiMessage>('/api/update-cart', { movieId, quantity }),
  placeOrder: (data: { firstName: string; lastName: string; cardNumber: string; expiration: string }) =>
    postForm<ApiMessage>('/api/place-order', data),
  orderSummary: () => request<OrderSummaryResponse>('/api/order-summary'),
  metadata: () => request<DatabaseMetadata>('/api/database-metadata'),
  insertGenre: (genreName: string) => postForm<ApiMessage>('/api/insert-genre', { genre_name: genreName }),
  insertStar: (starName: string, birthYear: string) =>
    postForm<ApiMessage>('/api/insert-star', { star_name: starName, birth_year: birthYear }),
  insertMovie: (data: {
    movie_title: string;
    movie_year: string;
    movie_director: string;
    star_name: string;
    genre_name: string;
  }) => postForm<ApiMessage>('/api/insert-movie', data)
};
