import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Clapperboard, Compass, Play, Search } from 'lucide-react';
import { ApiError, api } from '../api/client';
import type { Movie } from '../types';
import MovieCard from '../components/MovieCard';
import MoviePoster from '../components/MoviePoster';
import TrailerBackdrop from '../components/TrailerBackdrop';
import LoadState from '../components/LoadState';

interface MovieRow {
  title: string;
  subtitle: string;
  to: string;
  movies: Movie[];
}

function MovieRail({ row }: { row: MovieRow }) {
  if (row.movies.length === 0) {
    return null;
  }

  return (
    <section className="movie-row" aria-labelledby={`row-${row.title.replace(/\W+/g, '-').toLowerCase()}`}>
      <div className="section-heading">
        <div>
          <p className="eyebrow">{row.subtitle}</p>
          <h2 id={`row-${row.title.replace(/\W+/g, '-').toLowerCase()}`}>{row.title}</h2>
        </div>
        <Link className="text-button" to={row.to}>
          See all
        </Link>
      </div>
      <div className="movie-rail">
        {row.movies.slice(0, 12).map((movie) => (
          <MovieCard key={movie.movie_id} movie={movie} variant="rail" />
        ))}
      </div>
    </section>
  );
}

export default function HomePage() {
  const navigate = useNavigate();
  const [firstName, setFirstName] = useState('');
  const [recentMovies, setRecentMovies] = useState<Movie[]>([]);
  const [topMovies, setTopMovies] = useState<Movie[]>([]);
  const [actionMovies, setActionMovies] = useState<Movie[]>([]);
  const [dramaMovies, setDramaMovies] = useState<Movie[]>([]);
  const [comedyMovies, setComedyMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([
      api.index(),
      api.movies({ minYear: 2020, sort1: 'relevance', dir1: 'desc', sort2: 'year', dir2: 'desc', pageSize: 12 }),
      api.movies({ top20: true, minYear: 2015 }),
      api.movies({ genre: 'Action', minYear: 2015, sort1: 'relevance', dir1: 'desc', sort2: 'year', dir2: 'desc', pageSize: 12 }),
      api.movies({ genre: 'Drama', minYear: 2015, sort1: 'relevance', dir1: 'desc', sort2: 'year', dir2: 'desc', pageSize: 12 }),
      api.movies({ genre: 'Comedy', minYear: 2015, sort1: 'relevance', dir1: 'desc', sort2: 'year', dir2: 'desc', pageSize: 12 })
    ])
      .then(([indexData, recent, top, action, drama, comedy]) => {
        setFirstName(indexData.firstName ?? '');
        setRecentMovies(recent);
        setTopMovies(top);
        setActionMovies(action);
        setDramaMovies(drama);
        setComedyMovies(comedy);
      })
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          navigate('/login', { replace: true });
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to load home page.');
      })
      .finally(() => setLoading(false));
  }, [navigate]);

  const featured = recentMovies[0] ?? topMovies[0] ?? actionMovies[0] ?? dramaMovies[0] ?? comedyMovies[0] ?? null;

  const rows = useMemo<MovieRow[]>(
    () => [
      {
        title: 'New and notable',
        subtitle: 'New releases',
        to: '/movies?minYear=2020&sort1=relevance&dir1=desc&sort2=year&dir2=desc',
        movies: recentMovies
      },
      {
        title: 'Modern top 20',
        subtitle: 'Audience favorites since 2015',
        to: '/movies?top20=true&minYear=2015',
        movies: topMovies
      },
      {
        title: 'Action momentum',
        subtitle: 'High energy picks',
        to: '/movies?genre=Action&minYear=2015',
        movies: actionMovies
      },
      {
        title: 'Critically strong drama',
        subtitle: 'Character driven',
        to: '/movies?genre=Drama&minYear=2015',
        movies: dramaMovies
      },
      {
        title: 'Comedy picks',
        subtitle: 'Light and funny',
        to: '/movies?genre=Comedy&minYear=2015',
        movies: comedyMovies
      }
    ],
    [actionMovies, comedyMovies, dramaMovies, recentMovies, topMovies]
  );

  return (
    <main>
      <LoadState loading={loading} error={error}>
        <section className="stream-hero">
          {featured && <TrailerBackdrop movie={featured} />}
          <div className="stream-hero-copy">
            <h1>{firstName ? `Welcome back, ${firstName}` : 'Welcome to Fabflix'}</h1>
            <div className="hero-actions">
              <Link className="primary-button" to={featured ? `/movies/${featured.movie_id}` : '/movies?top20=true&minYear=2015'}>
                <Play size={18} fill="currentColor" />
                Open featured
              </Link>
              <Link className="secondary-button" to="/discover">
                <Compass size={18} />
                Discover
              </Link>
            </div>
          </div>

          {featured && (
            <div className="hero-feature">
              <MoviePoster movie={featured} size="hero" />
              <div>
                <p className="eyebrow">Featured title</p>
                <h2>{featured.title}</h2>
                <p>
                  {featured.year} · {featured.director} · {featured.rating?.toFixed(1) ?? 'N/A'}
                </p>
              </div>
            </div>
          )}
        </section>

        <section className="quick-actions" aria-label="Primary browsing actions">
          <Link to="/movies?top20=true&minYear=2015">
            <Clapperboard size={19} />
            Top 20 Movies
          </Link>
          <Link to="/discover">
            <Compass size={19} />
            Discover by Genre
          </Link>
          <Link to="/browse">
            <Search size={19} />
            Advanced Search
          </Link>
        </section>

        <div className="stream-content">
          {rows.map((row) => (
            <MovieRail key={row.title} row={row} />
          ))}
        </div>
      </LoadState>
    </main>
  );
}
