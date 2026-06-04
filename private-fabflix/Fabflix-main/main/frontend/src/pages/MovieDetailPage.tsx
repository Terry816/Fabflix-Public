import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Calendar, Clapperboard, DollarSign, Plus, Star, UserRound } from 'lucide-react';
import { ApiError, api } from '../api/client';
import type { Movie } from '../types';
import LoadState from '../components/LoadState';
import MoviePoster from '../components/MoviePoster';
import StarAvatar from '../components/StarAvatar';
import TrailerBackdrop from '../components/TrailerBackdrop';
import { announceCartUpdated } from '../utils/cartEvents';
import { splitList } from '../utils/movieArtwork';
import { buildGenreLinks } from './MovieListPage';

export default function MovieDetailPage() {
  const { movieId = '' } = useParams();
  const navigate = useNavigate();
  const [movie, setMovie] = useState<Movie | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    setLoading(true);
    api
      .movie(movieId)
      .then((data) => setMovie(data[0] ?? null))
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          navigate('/login', { replace: true });
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to load movie.');
      })
      .finally(() => setLoading(false));
  }, [movieId, navigate]);

  async function addToCart() {
    if (!movie) return;
    try {
      const response = await api.addToCart(movie.movie_id);
      announceCartUpdated();
      setNotice(response.message);
    } catch (err) {
      setNotice(err instanceof Error ? err.message : 'Failed to add movie to cart.');
    }
  }

  const starNames = splitList(movie?.stars ?? null);
  const starIds = splitList(movie?.star_ids ?? null);
  const price = (movie?.rating ?? 0).toFixed(2);

  return (
    <main className="page-shell">
      <button className="secondary-button back-button" type="button" onClick={() => navigate(-1)}>
        <ArrowLeft size={17} />
        Back
      </button>

      <LoadState loading={loading} error={error}>
        {movie ? (
          <section className="detail-layout">
            <aside className="detail-poster-panel">
              <MoviePoster movie={movie} size="detail" />
              <div className="detail-poster-copy">
                <p className="eyebrow">Now showing</p>
                <h1>{movie.title}</h1>
                <div className="detail-poster-meta">
                  <span>{movie.year}</span>
                  <span>{movie.rating?.toFixed(1) ?? 'N/A'}</span>
                </div>
                <div className="price-card" aria-label={`Price ${price} dollars`}>
                  <DollarSign size={19} />
                  <span>Price</span>
                  <strong>${price}</strong>
                </div>
                <button className="primary-button" type="button" onClick={addToCart}>
                  <Plus size={18} />
                  Add to Cart
                </button>
                {notice && <div className="form-message">{notice}</div>}
              </div>
            </aside>

            <div className="detail-content-stack">
              <div className="detail-main">
                <TrailerBackdrop movie={movie} showPosterBase={false} />
              </div>

              <section className="panel detail-info-panel">
                <div className="form-title">
                  <Clapperboard size={20} />
                  <h2>Movie Details</h2>
                </div>

                <dl className="metadata-list">
                  <div>
                    <Calendar size={18} />
                    <dt>Year</dt>
                    <dd>{movie.year}</dd>
                  </div>
                  <div>
                    <UserRound size={18} />
                    <dt>Director</dt>
                    <dd>{movie.director}</dd>
                  </div>
                  <div>
                    <Star size={18} fill="currentColor" />
                    <dt>Rating</dt>
                    <dd>{movie.rating?.toFixed(1) ?? 'N/A'}</dd>
                  </div>
                </dl>
              </section>

              <div className="panel">
                <div className="form-title">
                  <Clapperboard size={20} />
                  <h2>Genres</h2>
                </div>
                <div className="chip-grid">{buildGenreLinks(movie.genres)}</div>
              </div>

              <div className="panel">
                <div className="form-title">
                  <UserRound size={20} />
                  <h2>Stars</h2>
                </div>
                <div className="cast-grid">
                  {starNames.map((name, index) =>
                    starIds[index] ? (
                      <Link className="cast-card" key={starIds[index]} to={`/stars/${starIds[index]}`}>
                        <StarAvatar starId={starIds[index]} name={name} />
                        <span>{name}</span>
                      </Link>
                    ) : (
                      <span className="cast-card" key={`${name}-${index}`}>
                        <StarAvatar starId="" name={name} />
                        <span>{name}</span>
                      </span>
                    )
                  )}
                </div>
              </div>
            </div>
          </section>
        ) : (
          <div className="state-box">
            Movie not found. <Link to="/movies?top20=true&minYear=2015">Browse top movies</Link>
          </div>
        )}
      </LoadState>
    </main>
  );
}
