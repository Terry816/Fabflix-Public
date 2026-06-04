import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Search, SlidersHorizontal } from 'lucide-react';
import { ApiError, api } from '../api/client';
import type { Genre } from '../types';
import LoadState from '../components/LoadState';

export default function BrowsePage() {
  const navigate = useNavigate();
  const [genres, setGenres] = useState<Genre[]>([]);
  const [form, setForm] = useState({ title: '', year: '', director: '', star: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const titleChars = useMemo(() => [...'0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ', '*'], []);
  const displayGenres = useMemo(() => genres.filter((genre) => !genre.name.startsWith('Playwright-')), [genres]);

  useEffect(() => {
    api
      .genres()
      .then(setGenres)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          navigate('/login', { replace: true });
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to load browse options.');
      })
      .finally(() => setLoading(false));
  }, [navigate]);

  function submit(event: FormEvent) {
    event.preventDefault();
    const params = new URLSearchParams();
    Object.entries(form).forEach(([key, value]) => {
      if (value.trim()) {
        params.set(key, value.trim());
      }
    });
    if (!params.has('year')) {
      params.set('minYear', '2015');
    }
    navigate(`/movies?${params.toString()}`);
  }

  return (
    <main className="page-shell">
      <section className="page-heading browse-heading">
        <div>
          <p className="eyebrow">Browse library</p>
          <h1>Find exactly what fits tonight</h1>
          <p className="muted">Search by title, director, year, star, genre, or first character.</p>
        </div>
      </section>

      <LoadState loading={loading} error={error}>
        <section className="browse-grid">
          <div className="search-panel">
            <div className="form-title">
              <SlidersHorizontal size={22} />
              <h2>Advanced Search</h2>
            </div>
            <form className="stacked-form compact" onSubmit={submit}>
              <label>
                Title
                <input
                  value={form.title}
                  onChange={(event) => setForm((value) => ({ ...value, title: event.target.value }))}
                  placeholder="Spider-Man"
                />
              </label>
              <label>
                Year
                <input
                  value={form.year}
                  onChange={(event) => setForm((value) => ({ ...value, year: event.target.value }))}
                  placeholder="2004"
                  inputMode="numeric"
                />
              </label>
              <label>
                Director
                <input
                  value={form.director}
                  onChange={(event) => setForm((value) => ({ ...value, director: event.target.value }))}
                  placeholder="Sam Raimi"
                />
              </label>
              <label>
                Star
                <input
                  value={form.star}
                  onChange={(event) => setForm((value) => ({ ...value, star: event.target.value }))}
                  placeholder="Tobey Maguire"
                />
              </label>
              <button className="primary-button" type="submit">
                <Search size={18} />
                Search
              </button>
            </form>
          </div>

          <section className="browse-panel" aria-labelledby="browse-genres">
            <h2 id="browse-genres">Browse by Genre</h2>
            <div className="chip-grid">
              {displayGenres.map((genre) => (
                <Link key={genre.name} to={`/movies?genre=${encodeURIComponent(genre.name)}&minYear=2015`}>
                  {genre.name}
                </Link>
              ))}
            </div>
          </section>

          <section className="browse-panel browse-letters" aria-labelledby="browse-title">
            <h2 id="browse-title">Browse by Title</h2>
            <div className="letter-grid">
              {titleChars.map((char) => (
                <Link key={char} to={`/movies?startsWith=${encodeURIComponent(char)}&minYear=2015`}>
                  {char}
                </Link>
              ))}
            </div>
          </section>
        </section>
      </LoadState>
    </main>
  );
}
