import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { StepBack, StepForward } from 'lucide-react';
import { ApiError, api } from '../api/client';
import type { Movie } from '../types';
import LoadState from '../components/LoadState';
import MovieCard from '../components/MovieCard';
import { announceCartUpdated } from '../utils/cartEvents';

type SortOption =
  | 'relevance-desc'
  | 'year-desc-rating-desc'
  | 'title-asc'
  | 'title-desc'
  | 'title-asc-rating-desc'
  | 'title-desc-rating-asc'
  | 'rating-asc'
  | 'rating-desc'
  | 'rating-asc-title-desc'
  | 'rating-desc-title-asc';

const sortOptions: Array<{ value: SortOption; label: string }> = [
  { value: 'relevance-desc', label: 'Most relevant recent' },
  { value: 'year-desc-rating-desc', label: 'Newest first' },
  { value: 'title-asc', label: 'Title ascending, rating ascending' },
  { value: 'title-desc', label: 'Title descending, rating descending' },
  { value: 'title-asc-rating-desc', label: 'Title ascending, rating descending' },
  { value: 'title-desc-rating-asc', label: 'Title descending, rating ascending' },
  { value: 'rating-asc', label: 'Rating ascending, title ascending' },
  { value: 'rating-desc', label: 'Rating descending, title descending' },
  { value: 'rating-asc-title-desc', label: 'Rating ascending, title descending' },
  { value: 'rating-desc-title-asc', label: 'Rating descending, title ascending' }
];

function getSortParams(value: SortOption) {
  let sort1 = 'relevance';
  let dir1 = 'desc';
  let sort2 = 'year';
  let dir2 = 'desc';

  switch (value) {
    case 'relevance-desc':
      break;
    case 'year-desc-rating-desc':
      sort1 = 'year';
      sort2 = 'rating';
      break;
    case 'title-asc':
      sort1 = 'title';
      dir1 = 'asc';
      sort2 = 'rating';
      dir2 = 'asc';
      break;
    case 'title-desc':
      sort1 = 'title';
      dir1 = 'desc';
      sort2 = 'rating';
      dir2 = 'desc';
      break;
    case 'title-asc-rating-desc':
      sort1 = 'title';
      dir1 = 'asc';
      sort2 = 'rating';
      dir2 = 'desc';
      break;
    case 'title-desc-rating-asc':
      sort1 = 'title';
      dir1 = 'desc';
      sort2 = 'rating';
      dir2 = 'asc';
      break;
    case 'rating-asc':
      sort1 = 'rating';
      dir1 = 'asc';
      sort2 = 'title';
      dir2 = 'asc';
      break;
    case 'rating-desc':
      sort1 = 'rating';
      dir1 = 'desc';
      sort2 = 'title';
      dir2 = 'desc';
      break;
    case 'rating-asc-title-desc':
      sort1 = 'rating';
      dir1 = 'asc';
      sort2 = 'title';
      dir2 = 'desc';
      break;
    case 'rating-desc-title-asc':
      sort1 = 'rating';
      dir1 = 'desc';
      sort2 = 'title';
      dir2 = 'asc';
      break;
  }

  return { sort1, dir1, sort2, dir2 };
}

function splitCsv(value: string | null) {
  return value ? value.split(/\s*,\s*/).filter(Boolean) : [];
}

export function buildGenreLinks(genres: string | null, limit = Number.MAX_SAFE_INTEGER) {
  return splitCsv(genres)
    .slice(0, limit)
    .map((genre) => (
      <Link key={genre} to={`/movies?genre=${encodeURIComponent(genre)}&minYear=2015`}>
        {genre}
      </Link>
    ));
}

export function buildStarLinks(stars: string | null, starIds: string | null, limit = Number.MAX_SAFE_INTEGER) {
  const names = splitCsv(stars);
  const ids = splitCsv(starIds);
  return names
    .slice(0, limit)
    .map((name, index) =>
      ids[index] ? (
        <Link key={`${ids[index]}-${index}`} to={`/stars/${ids[index]}`}>
          {name}
        </Link>
      ) : (
        <span key={`${name}-${index}`}>{name}</span>
      )
    );
}

export default function MovieListPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const queryKey = searchParams.toString();
  const [movies, setMovies] = useState<Movie[]>([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [sort, setSort] = useState<SortOption>('relevance-desc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const isTop20 = searchParams.get('top20') === 'true';
  const fullTextQuery = searchParams.get('q') ?? '';

  const title = useMemo(() => {
    if (isTop20) return 'Top 20 Movies';
    if (fullTextQuery) return `Search results for "${fullTextQuery}"`;
    if (searchParams.get('genre')) return `${searchParams.get('genre')} Movies`;
    if (searchParams.get('startsWith')) return `Titles starting with ${searchParams.get('startsWith')}`;
    return 'Movie Results';
  }, [fullTextQuery, isTop20, searchParams]);

  useEffect(() => {
    setPage(1);
  }, [queryKey]);

  useEffect(() => {
    setLoading(true);
    setError('');
    setNotice('');
    const sortParams = getSortParams(sort);
    const minYear = searchParams.get('minYear') ?? (searchParams.get('year') ? null : '2015');
    const shared = { ...sortParams, minYear, page, pageSize };
    const request = fullTextQuery
      ? api.fullTextMovies({ q: fullTextQuery, ...shared })
      : api.movies({
          top20: isTop20,
          title: searchParams.get('title'),
          year: searchParams.get('year'),
          director: searchParams.get('director'),
          star: searchParams.get('star'),
          genre: searchParams.get('genre'),
          startsWith: searchParams.get('startsWith'),
          ...shared
        });

    request
      .then(setMovies)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          navigate('/login', { replace: true });
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to load movies.');
      })
      .finally(() => setLoading(false));
  }, [fullTextQuery, isTop20, navigate, page, pageSize, queryKey, searchParams, sort]);

  async function addMovie(movieId: string) {
    setNotice('');
    try {
      const response = await api.addToCart(movieId);
      announceCartUpdated();
      setNotice(response.message);
    } catch (err) {
      setNotice(err instanceof Error ? err.message : 'Failed to add movie to cart.');
    }
  }

  return (
    <main className="page-shell">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Catalog</p>
          <h1>{title}</h1>
          <p className="muted">Poster-first browsing with quick add and focused metadata.</p>
        </div>
        <div className="toolbar">
          <label>
            Sort
            <select value={sort} onChange={(event) => setSort(event.target.value as SortOption)}>
              {sortOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Page size
            <select
              value={pageSize}
              disabled={isTop20}
              onChange={(event) => {
                setPageSize(Number(event.target.value));
                setPage(1);
              }}
            >
              {[10, 25, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>
        </div>
      </section>

      {notice && <div className="form-message">{notice}</div>}

      <LoadState loading={loading} error={error}>
        <section className="catalog-grid" aria-label="Movie results">
          {movies.map((movie) => (
            <MovieCard key={movie.movie_id} movie={movie} onAdd={addMovie} />
          ))}
        </section>

        {movies.length === 0 && <div className="state-box">No movies found.</div>}

        <div className="pagination">
          <button className="secondary-button" type="button" disabled={page === 1 || isTop20} onClick={() => setPage((value) => Math.max(1, value - 1))}>
            <StepBack size={17} />
            Previous
          </button>
          <span>Page {page}</span>
          <button className="secondary-button" type="button" disabled={movies.length < pageSize || isTop20} onClick={() => setPage((value) => value + 1)}>
            Next
            <StepForward size={17} />
          </button>
        </div>
      </LoadState>
    </main>
  );
}
