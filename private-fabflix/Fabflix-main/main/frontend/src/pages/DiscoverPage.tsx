import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Flame, Grid3X3, Star } from 'lucide-react';
import { ApiError, api } from '../api/client';
import type { Movie } from '../types';
import LoadState from '../components/LoadState';
import MovieCard from '../components/MovieCard';

const genreTabs = ['Action', 'Drama', 'Comedy', 'Adventure', 'Sci-Fi', 'Crime'];

export default function DiscoverPage() {
  const navigate = useNavigate();
  const [activeGenre, setActiveGenre] = useState('Action');
  const [movies, setMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');
    api
      .movies({
        genre: activeGenre,
        minYear: 2015,
        sort1: 'relevance',
        dir1: 'desc',
        sort2: 'year',
        dir2: 'desc',
        pageSize: 24
      })
      .then(setMovies)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          navigate('/login', { replace: true });
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to load discovery results.');
      })
      .finally(() => setLoading(false));
  }, [activeGenre, navigate]);

  return (
    <main className="page-shell">
      <section className="page-heading discover-heading">
        <div>
          <p className="eyebrow">Discover</p>
          <h1>Browse like a streaming app</h1>
          <p className="muted">Pick a genre and start browsing.</p>
        </div>
        <Link className="secondary-button" to="/movies?top20=true&minYear=2015">
          <Star size={18} />
          Top 20
        </Link>
      </section>

      <div className="genre-tabs" role="tablist" aria-label="Discover genres">
        {genreTabs.map((genre) => (
          <button
            key={genre}
            className={genre === activeGenre ? 'active' : ''}
            type="button"
            role="tab"
            aria-selected={genre === activeGenre}
            onClick={() => setActiveGenre(genre)}
          >
            {genre === activeGenre ? <Flame size={16} /> : <Grid3X3 size={16} />}
            {genre}
          </button>
        ))}
      </div>

      <LoadState loading={loading} error={error}>
        <section className="catalog-grid" aria-label={`${activeGenre} movies`}>
          {movies.map((movie) => (
            <MovieCard key={movie.movie_id} movie={movie} />
          ))}
        </section>
        {movies.length === 0 && <div className="state-box">No movies found for this genre.</div>}
      </LoadState>
    </main>
  );
}
