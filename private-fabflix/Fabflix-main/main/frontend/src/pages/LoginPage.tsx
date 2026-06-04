import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Film } from 'lucide-react';
import { ApiError, api } from '../api/client';
import MoviePoster from '../components/MoviePoster';
import type { Movie } from '../types';

const authPosterMovies: Movie[] = [
  {
    movie_id: 'tt15239678',
    title: 'Dune: Part Two',
    year: 2024,
    director: 'Denis Villeneuve',
    rating: 8.4,
    genres: 'Action, Adventure, Drama',
    stars: null,
    star_ids: null
  },
  {
    movie_id: 'tt15398776',
    title: 'Oppenheimer',
    year: 2023,
    director: 'Christopher Nolan',
    rating: 8.2,
    genres: 'Biography, Drama, History',
    stars: null,
    star_ids: null
  },
  {
    movie_id: 'tt1745960',
    title: 'Top Gun: Maverick',
    year: 2022,
    director: 'Joseph Kosinski',
    rating: 8.2,
    genres: 'Action, Drama',
    stars: null,
    star_ids: null
  },
  {
    movie_id: 'tt10872600',
    title: 'Spider-Man: No Way Home',
    year: 2021,
    director: 'Jon Watts',
    rating: 8.1,
    genres: 'Action, Adventure, Fantasy',
    stars: null,
    star_ids: null
  },
  {
    movie_id: 'tt1877830',
    title: 'The Batman',
    year: 2022,
    director: 'Matt Reeves',
    rating: 7.8,
    genres: 'Action, Crime, Drama',
    stars: null,
    star_ids: null
  },
  {
    movie_id: 'tt9362722',
    title: 'Spider-Man: Across the Spider-Verse',
    year: 2023,
    director: 'Joaquim Dos Santos',
    rating: 8.6,
    genres: 'Animation, Action, Adventure',
    stars: null,
    star_ids: null
  },
  {
    movie_id: 'tt6710474',
    title: 'Everything Everywhere All at Once',
    year: 2022,
    director: 'Daniel Kwan',
    rating: 7.8,
    genres: 'Action, Adventure, Comedy',
    stars: null,
    star_ids: null
  },
  {
    movie_id: 'tt2380307',
    title: 'Coco',
    year: 2017,
    director: 'Lee Unkrich',
    rating: 8.4,
    genres: 'Animation, Adventure, Drama',
    stars: null,
    star_ids: null
  },
  {
    movie_id: 'tt2948356',
    title: 'Zootopia',
    year: 2016,
    director: 'Byron Howard',
    rating: 8.0,
    genres: 'Animation, Adventure, Comedy',
    stars: null,
    star_ids: null
  },
  {
    movie_id: 'tt4154796',
    title: 'Avengers: Endgame',
    year: 2019,
    director: 'Anthony Russo',
    rating: 8.4,
    genres: 'Action, Adventure, Drama',
    stars: null,
    star_ids: null
  },
  {
    movie_id: 'tt5052448',
    title: 'Get Out',
    year: 2017,
    director: 'Jordan Peele',
    rating: 7.8,
    genres: 'Horror, Mystery, Thriller',
    stars: null,
    star_ids: null
  },
  {
    movie_id: 'tt6751668',
    title: 'Parasite',
    year: 2019,
    director: 'Bong Joon Ho',
    rating: 8.5,
    genres: 'Drama, Thriller',
    stars: null,
    star_ids: null
  }
];

export function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await api.login(email, password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Login failed.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-screen auth-poster-screen">
      <div className="auth-poster-wall" aria-hidden="true">
        {authPosterMovies.map((movie) => (
          <div className="auth-wall-poster" key={movie.movie_id}>
            <MoviePoster movie={movie} />
          </div>
        ))}
      </div>

      <section className="auth-panel auth-login-panel">
        <div>
          <img className="auth-logo" src="/images/fabflix-logo.png" alt="" />
          <h2>Login to Fabflix</h2>
        </div>
        <form onSubmit={submit} className="stacked-form">
          <label>
            Email
            <input value={email} onChange={(event) => setEmail(event.target.value)} type="email" required />
          </label>
          <label>
            Password
            <span className="password-control">
              <input
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                type={showPassword ? 'text' : 'password'}
                required
              />
              <button type="button" onClick={() => setShowPassword((value) => !value)} aria-label="Toggle password">
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </span>
          </label>
          {error && <div className="form-message error">{error}</div>}
          <button className="primary-button" type="submit" disabled={submitting}>
            <Film size={18} />
            {submitting ? 'Signing in...' : 'Login'}
          </button>
        </form>
        <Link className="subtle-link" to="/employee/login">
          Employee dashboard
        </Link>
      </section>
    </main>
  );
}

export function EmployeeLoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await api.employeeLogin(email, password);
      navigate('/employee/dashboard', { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Employee login failed.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-screen">
      <section className="auth-panel">
        <img className="auth-logo" src="/images/fabflix-logo.png" alt="" />
        <h1>Employee Login</h1>
        <form onSubmit={submit} className="stacked-form">
          <label>
            Email
            <input value={email} onChange={(event) => setEmail(event.target.value)} type="email" required />
          </label>
          <label>
            Password
            <input value={password} onChange={(event) => setPassword(event.target.value)} type="password" required />
          </label>
          {error && <div className="form-message error">{error}</div>}
          <button className="primary-button" type="submit" disabled={submitting}>
            {submitting ? 'Signing in...' : 'Login'}
          </button>
        </form>
        <Link className="subtle-link" to="/login">
          Customer login
        </Link>
      </section>
    </main>
  );
}
