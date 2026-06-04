import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { CreditCard, Minus, Plus, ShoppingBag, Trash2 } from 'lucide-react';
import { ApiError, api } from '../api/client';
import type { CartItem, Movie } from '../types';
import LoadState from '../components/LoadState';
import MoviePoster from '../components/MoviePoster';
import { announceCartUpdated } from '../utils/cartEvents';

function cartItemMovie(item: CartItem): Movie {
  return {
    movie_id: item.movieId,
    title: item.title,
    year: item.year,
    director: item.director,
    rating: item.rating,
    genres: null,
    stars: null,
    star_ids: null
  };
}

export default function CartPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const total = useMemo(
    () => items.reduce((sum, item) => sum + (item.rating ?? 0) * item.quantity, 0),
    [items]
  );

  function loadCart() {
    setLoading(true);
    setError('');
    api
      .cart()
      .then((data) => setItems(data.cartItems))
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          navigate('/login', { replace: true });
          return;
        }
        setError(err instanceof Error ? err.message : 'Failed to load cart.');
      })
      .finally(() => setLoading(false));
  }

  useEffect(loadCart, [navigate]);

  async function updateQuantity(movieId: string, quantity: number) {
    setNotice('');
    try {
      await api.updateCart(movieId, quantity);
      announceCartUpdated();
      loadCart();
    } catch (err) {
      setNotice(err instanceof Error ? err.message : 'Failed to update cart.');
    }
  }

  return (
    <main className="page-shell">
      <section className="page-heading cart-heading">
        <div>
          <p className="eyebrow">Checkout</p>
          <h1>Shopping Cart</h1>
          <p className="muted">Review your queue before payment. Poster-backed titles stay easy to scan.</p>
        </div>
        <div className="total-box">
          <ShoppingBag size={18} />
          Total ${total.toFixed(2)}
        </div>
      </section>

      {notice && <div className="form-message error">{notice}</div>}

      <LoadState loading={loading} error={error}>
        {items.length === 0 ? (
          <div className="state-box">Your shopping cart is empty.</div>
        ) : (
          <section className="cart-layout">
            <div className="cart-list" role="table" aria-label="Shopping cart items">
              <div className="cart-list-head" role="row">
                <span>Title</span>
                <span>Quantity</span>
                <span>Price</span>
                <span>Subtotal</span>
              </div>
              {items.map((item) => {
                const subtotal = (item.rating ?? 0) * item.quantity;
                return (
                  <article className="cart-item-card" key={item.movieId} role="row">
                    <Link className="cart-poster-link" to={`/movies/${item.movieId}`} aria-label={`View ${item.title}`}>
                      <MoviePoster movie={cartItemMovie(item)} />
                    </Link>
                    <div className="cart-item-main">
                      <Link className="cart-title" to={`/movies/${item.movieId}`}>
                        {item.title}
                      </Link>
                      <p>
                        {item.year} · {item.director}
                      </p>
                    </div>
                    <div className="quantity-stepper" aria-label={`${item.title} quantity`}>
                      <button
                        className="icon-button"
                        type="button"
                        onClick={() => updateQuantity(item.movieId, Math.max(0, item.quantity - 1))}
                        aria-label={`Decrease ${item.title} quantity`}
                      >
                        <Minus size={16} />
                      </button>
                      <input
                        className="quantity-input"
                        type="number"
                        min={0}
                        value={item.quantity}
                        onChange={(event) => updateQuantity(item.movieId, Number(event.target.value))}
                      />
                      <button
                        className="icon-button"
                        type="button"
                        onClick={() => updateQuantity(item.movieId, item.quantity + 1)}
                        aria-label={`Increase ${item.title} quantity`}
                      >
                        <Plus size={16} />
                      </button>
                    </div>
                    <div className="cart-price">${(item.rating ?? 0).toFixed(2)}</div>
                    <div className="cart-subtotal">${subtotal.toFixed(2)}</div>
                    <button className="small-button danger" type="button" onClick={() => updateQuantity(item.movieId, 0)}>
                      <Trash2 size={16} />
                      Remove
                    </button>
                  </article>
                );
              })}
            </div>

            <aside className="cart-summary">
              <p className="eyebrow">Order Summary</p>
              <div>
                <span>Items</span>
                <strong>{items.reduce((sum, item) => sum + item.quantity, 0)}</strong>
              </div>
              <div>
                <span>Estimated total</span>
                <strong>${total.toFixed(2)}</strong>
              </div>
              <button className="primary-button" type="button" onClick={() => navigate('/payment')}>
                <CreditCard size={19} />
                Proceed to Payment
              </button>
            </aside>
          </section>
        )}
      </LoadState>
    </main>
  );
}
