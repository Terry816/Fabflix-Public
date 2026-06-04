import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Compass, Home, LibraryBig, LogOut, Search, ShoppingCart, Trophy } from 'lucide-react';
import { api } from '../api/client';
import type { Suggestion } from '../types';
import { CART_UPDATED_EVENT, cartItemCount } from '../utils/cartEvents';

export default function NavBar() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [open, setOpen] = useState(false);
  const [cartCount, setCartCount] = useState(0);

  function loadCartCount() {
    api
      .cart()
      .then((data) => setCartCount(cartItemCount(data.cartItems)))
      .catch(() => setCartCount(0));
  }

  useEffect(() => {
    if (query.trim().length < 3) {
      setSuggestions([]);
      return;
    }

    const timer = window.setTimeout(() => {
      api
        .autocomplete(query.trim())
        .then((data) => {
          setSuggestions(data);
          setOpen(data.length > 0);
        })
        .catch(() => setSuggestions([]));
    }, 250);

    return () => window.clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    loadCartCount();
    window.addEventListener(CART_UPDATED_EVENT, loadCartCount);
    window.addEventListener('focus', loadCartCount);

    return () => {
      window.removeEventListener(CART_UPDATED_EVENT, loadCartCount);
      window.removeEventListener('focus', loadCartCount);
    };
  }, []);

  const navItems = useMemo(
    () => [
      { to: '/', label: 'Home', icon: Home },
      { to: '/discover', label: 'Discover', icon: Compass },
      { to: '/browse', label: 'Browse', icon: LibraryBig },
      { to: '/movies?top20=true&minYear=2015', label: 'Top 20', icon: Trophy }
    ],
    []
  );

  function submit(event: FormEvent) {
    event.preventDefault();
    const value = query.trim();
    if (value) {
      setOpen(false);
      navigate(`/movies?q=${encodeURIComponent(value)}&minYear=2015`);
    }
  }

  function selectSuggestion(suggestion: Suggestion) {
    setQuery('');
    setOpen(false);
    navigate(`/movies/${suggestion.data.movieId}`);
  }

  async function logout() {
    await api.logout().catch(() => undefined);
    navigate('/login', { replace: true });
  }

  return (
    <header className="topbar">
      <Link className="brand" to="/">
        <img src="/images/fabflix-logo.png" alt="" />
        <span>Fabflix</span>
      </Link>

      <form className="global-search" onSubmit={submit}>
        <label className="sr-only" htmlFor="global-search-input">
          Search movies
        </label>
        <Search size={18} aria-hidden="true" />
        <input
          id="global-search-input"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          onFocus={() => setOpen(suggestions.length > 0)}
          placeholder="Search movies"
          autoComplete="off"
        />
        {open && (
          <div className="suggestion-menu">
            {suggestions.map((suggestion) => (
              <button key={suggestion.data.movieId} type="button" onMouseDown={() => selectSuggestion(suggestion)}>
                {suggestion.value}
              </button>
            ))}
          </div>
        )}
      </form>

      <nav className="nav-links" aria-label="Primary navigation">
        {navItems.map((item) => (
          <NavLink key={item.to} to={item.to}>
            <item.icon size={17} />
            {item.label}
          </NavLink>
        ))}
        <NavLink className="icon-link" to="/cart" aria-label="Shopping cart">
          <span className="cart-nav-icon">
            <ShoppingCart size={19} />
            {cartCount > 0 && <span className="cart-count-badge">{cartCount > 99 ? '99+' : cartCount}</span>}
          </span>
          <span>Cart</span>
        </NavLink>
        <button className="icon-button" type="button" onClick={logout} aria-label="Logout">
          <LogOut size={18} />
          <span>Logout</span>
        </button>
      </nav>
    </header>
  );
}
