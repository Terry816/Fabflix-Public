import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Plus } from 'lucide-react';
import { ApiError, api } from '../api/client';
import type { StarMovie } from '../types';
import LoadState from '../components/LoadState';
import StarAvatar from '../components/StarAvatar';
import { announceCartUpdated } from '../utils/cartEvents';

export default function StarDetailPage() {
  const { starId = '' } = useParams();
  const navigate = useNavigate();
  const [movies, setMovies] = useState<StarMovie[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    setLoading(true);
    api
      .star(starId)
      .then(setMovies)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          navigate('/login', { replace: true });
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to load star.');
      })
      .finally(() => setLoading(false));
  }, [navigate, starId]);

  const starName = movies[0]?.star_name ?? 'Star';
  const birthYear = useMemo(() => movies.find((movie) => movie.birthYear !== null)?.birthYear ?? null, [movies]);

  async function addMovie(movieId: string) {
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
      <button className="secondary-button back-button" type="button" onClick={() => navigate(-1)}>
        <ArrowLeft size={17} />
        Back
      </button>

      <LoadState loading={loading} error={error}>
        <section className="page-heading star-heading">
          <StarAvatar starId={starId} name={starName} size="hero" />
          <div>
            <p className="eyebrow">Star</p>
            <h1>{starName}</h1>
            <p className="muted">{birthYear ? `Born ${birthYear}` : 'Birth year unavailable'}</p>
          </div>
        </section>

        {notice && <div className="form-message">{notice}</div>}

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Movie Title</th>
                <th>Release Year</th>
                <th>Director</th>
                <th>Cart</th>
              </tr>
            </thead>
            <tbody>
              {movies.map((movie) => (
                <tr key={movie.movie_id}>
                  <td>
                    <Link to={`/movies/${movie.movie_id}`}>{movie.movie_title}</Link>
                  </td>
                  <td>{movie.movie_year}</td>
                  <td>{movie.director}</td>
                  <td>
                    <button className="small-button" type="button" onClick={() => addMovie(movie.movie_id)}>
                      <Plus size={16} />
                      Add
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {movies.length === 0 && <div className="state-box">No movies found for this star.</div>}
      </LoadState>
    </main>
  );
}
