import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BadgeCheck, CreditCard } from 'lucide-react';
import { ApiError, api } from '../api/client';
import type { CartItem } from '../types';
import LoadState from '../components/LoadState';
import { announceCartUpdated } from '../utils/cartEvents';

export default function PaymentPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<CartItem[]>([]);
  const [form, setForm] = useState({ firstName: '', lastName: '', cardNumber: '', expiration: '' });
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const total = useMemo(
    () => items.reduce((sum, item) => sum + (item.rating ?? 0) * item.quantity, 0),
    [items]
  );

  useEffect(() => {
    api
      .cart()
      .then((data) => setItems(data.cartItems))
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          navigate('/login', { replace: true });
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to load payment page.');
      })
      .finally(() => setLoading(false));
  }, [navigate]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await api.placeOrder(form);
      announceCartUpdated();
      navigate('/confirmation', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error processing payment.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-shell">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Payment</p>
          <h1>Complete Order</h1>
        </div>
        <div className="total-box">Total ${total.toFixed(2)}</div>
      </section>

      <LoadState loading={loading} error={null}>
        <section className="panel narrow">
          <form className="stacked-form" onSubmit={submit}>
            <label>
              First Name on Card
              <input
                value={form.firstName}
                onChange={(event) => setForm((value) => ({ ...value, firstName: event.target.value }))}
                required
              />
            </label>
            <label>
              Last Name on Card
              <input
                value={form.lastName}
                onChange={(event) => setForm((value) => ({ ...value, lastName: event.target.value }))}
                required
              />
            </label>
            <label>
              Credit Card Number
              <span className="input-icon">
                <CreditCard size={18} />
                <input
                  value={form.cardNumber}
                  onChange={(event) => setForm((value) => ({ ...value, cardNumber: event.target.value }))}
                  required
                />
              </span>
            </label>
            <label>
              Expiration Date
              <input
                value={form.expiration}
                onChange={(event) => setForm((value) => ({ ...value, expiration: event.target.value }))}
                type="date"
                required
              />
            </label>
            {error && <div className="form-message error">{error}</div>}
            <button className="primary-button" type="submit" disabled={submitting || items.length === 0}>
              <BadgeCheck size={19} />
              {submitting ? 'Placing Order...' : 'Place Order'}
            </button>
          </form>
        </section>
      </LoadState>
    </main>
  );
}
