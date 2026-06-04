import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { CheckCircle2 } from 'lucide-react';
import { ApiError, api } from '../api/client';
import type { OrderItem } from '../types';
import LoadState from '../components/LoadState';

export default function ConfirmationPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<OrderItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .orderSummary()
      .then((data) => setItems(data.cartItems))
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          navigate('/login', { replace: true });
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to load order summary.');
      })
      .finally(() => setLoading(false));
  }, [navigate]);

  return (
    <main className="page-shell">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Confirmation</p>
          <h1>Order Placed</h1>
        </div>
        <CheckCircle2 className="success-icon" size={40} />
      </section>

      <LoadState loading={loading} error={error}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Movie Title</th>
                <th>Quantity</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.movieId}>
                  <td>
                    <Link to={`/movies/${item.movieId}`}>{item.title}</Link>
                  </td>
                  <td>{item.quantity}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="checkout-row">
          <Link className="primary-button" to="/">
            Continue Browsing
          </Link>
        </div>
      </LoadState>
    </main>
  );
}
