export type ApiStatus = 'success' | 'fail';

export interface ApiMessage {
  status: ApiStatus;
  message: string;
  [key: string]: unknown;
}

export interface Movie {
  movie_id: string;
  title: string;
  year: number;
  director: string;
  rating: number | null;
  genres: string | null;
  stars: string | null;
  star_ids: string | null;
}

export interface Genre {
  name: string;
}

export interface Suggestion {
  value: string;
  data: {
    movieId: string;
  };
}

export interface StarMovie {
  star_name: string;
  birthYear: number | null;
  movie_id: string;
  movie_title: string;
  movie_year: number;
  director: string;
}

export interface CartItem {
  movieId: string;
  title: string;
  year: number;
  director: string;
  rating: number | null;
  quantity: number;
}

export interface CartResponse extends ApiMessage {
  cartItems: CartItem[];
}

export interface OrderItem {
  movieId: string;
  title: string;
  quantity: number;
}

export interface OrderSummaryResponse extends ApiMessage {
  cartItems: OrderItem[];
}

export interface MetadataColumn {
  column_name: string;
  column_type: string;
}

export type DatabaseMetadata = Record<string, MetadataColumn[]>;
