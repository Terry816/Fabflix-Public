import { FormEvent, ReactNode, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Database, Film, Plus, UserRound } from 'lucide-react';
import { ApiError, api } from '../api/client';
import type { DatabaseMetadata } from '../types';
import LoadState from '../components/LoadState';

export function DashboardPage() {
  const navigate = useNavigate();
  const [metadata, setMetadata] = useState<DatabaseMetadata>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .metadata()
      .then(setMetadata)
      .catch((err) => {
        if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
          navigate('/employee/login', { replace: true });
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to load database metadata.');
      })
      .finally(() => setLoading(false));
  }, [navigate]);

  return (
    <main className="page-shell">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Employee</p>
          <h1>Database Metadata</h1>
        </div>
        <Database size={38} className="success-icon" />
      </section>

      <LoadState loading={loading} error={error}>
        <div className="metadata-grid">
          {Object.entries(metadata).map(([table, columns]) => (
            <section className="panel" key={table}>
              <h2>{table}</h2>
              <div className="table-wrap compact-table">
                <table>
                  <thead>
                    <tr>
                      <th>Column</th>
                      <th>Type</th>
                    </tr>
                  </thead>
                  <tbody>
                    {columns.map((column) => (
                      <tr key={`${table}-${column.column_name}`}>
                        <td>{column.column_name}</td>
                        <td>{column.column_type}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          ))}
        </div>
      </LoadState>
    </main>
  );
}

export function AddGenrePage() {
  const [genreName, setGenreName] = useState('');
  const [message, setMessage] = useState('');

  async function submit(event: FormEvent) {
    event.preventDefault();
    setMessage('');
    try {
      const response = await api.insertGenre(genreName);
      setMessage(response.message);
      setGenreName('');
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Failed to add genre.');
    }
  }

  return (
    <AdminForm title="Add New Genre" icon={<Plus size={26} />} onSubmit={submit} message={message}>
      <label>
        Genre Name
        <input value={genreName} onChange={(event) => setGenreName(event.target.value)} required />
      </label>
    </AdminForm>
  );
}

export function AddStarPage() {
  const [starName, setStarName] = useState('');
  const [birthYear, setBirthYear] = useState('');
  const [message, setMessage] = useState('');

  async function submit(event: FormEvent) {
    event.preventDefault();
    setMessage('');
    try {
      const response = await api.insertStar(starName, birthYear);
      setMessage(`${response.message}${response.star_id ? ` (${String(response.star_id)})` : ''}`);
      setStarName('');
      setBirthYear('');
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Failed to add star.');
    }
  }

  return (
    <AdminForm title="Add New Star" icon={<UserRound size={26} />} onSubmit={submit} message={message}>
      <label>
        Star Name
        <input value={starName} onChange={(event) => setStarName(event.target.value)} required />
      </label>
      <label>
        Birth Year
        <input value={birthYear} onChange={(event) => setBirthYear(event.target.value)} inputMode="numeric" />
      </label>
    </AdminForm>
  );
}

export function AddMoviePage() {
  const [form, setForm] = useState({
    movie_title: '',
    movie_year: '',
    movie_director: '',
    star_name: '',
    genre_name: ''
  });
  const [message, setMessage] = useState('');

  async function submit(event: FormEvent) {
    event.preventDefault();
    setMessage('');
    try {
      const response = await api.insertMovie(form);
      setMessage(`${response.message}${response.movie_id ? ` (${String(response.movie_id)})` : ''}`);
      setForm({ movie_title: '', movie_year: '', movie_director: '', star_name: '', genre_name: '' });
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Failed to add movie.');
    }
  }

  function updateField(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  return (
    <AdminForm title="Add New Movie" icon={<Film size={26} />} onSubmit={submit} message={message}>
      <label>
        Movie Title
        <input value={form.movie_title} onChange={(event) => updateField('movie_title', event.target.value)} required />
      </label>
      <label>
        Release Year
        <input
          value={form.movie_year}
          onChange={(event) => updateField('movie_year', event.target.value)}
          inputMode="numeric"
          required
        />
      </label>
      <label>
        Director
        <input
          value={form.movie_director}
          onChange={(event) => updateField('movie_director', event.target.value)}
          required
        />
      </label>
      <label>
        Star Name
        <input value={form.star_name} onChange={(event) => updateField('star_name', event.target.value)} required />
      </label>
      <label>
        Genre Name
        <input value={form.genre_name} onChange={(event) => updateField('genre_name', event.target.value)} required />
      </label>
    </AdminForm>
  );
}

interface AdminFormProps {
  title: string;
  icon: ReactNode;
  message: string;
  onSubmit: (event: FormEvent) => void;
  children: ReactNode;
}

function AdminForm({ title, icon, message, onSubmit, children }: AdminFormProps) {
  return (
    <main className="page-shell">
      <section className="panel narrow">
        <div className="form-title">
          {icon}
          <h1>{title}</h1>
        </div>
        <form className="stacked-form" onSubmit={onSubmit}>
          {children}
          {message && <div className="form-message">{message}</div>}
          <button className="primary-button" type="submit">
            Save
          </button>
        </form>
      </section>
    </main>
  );
}
