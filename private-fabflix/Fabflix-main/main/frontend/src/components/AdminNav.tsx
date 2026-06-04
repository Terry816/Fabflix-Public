import { NavLink, useNavigate } from 'react-router-dom';
import { LogOut } from 'lucide-react';
import { api } from '../api/client';

export default function AdminNav() {
  const navigate = useNavigate();

  async function logout() {
    await api.logout().catch(() => undefined);
    navigate('/employee/login', { replace: true });
  }

  return (
    <header className="topbar admin-topbar">
      <NavLink className="brand" to="/employee/dashboard">
        <img src="/images/fabflix-logo.png" alt="" />
        <span>Fabflix Employee</span>
      </NavLink>
      <nav className="nav-links" aria-label="Employee navigation">
        <NavLink to="/employee/dashboard">Metadata</NavLink>
        <NavLink to="/employee/add-movie">Add Movie</NavLink>
        <NavLink to="/employee/add-star">Add Star</NavLink>
        <NavLink to="/employee/add-genre">Add Genre</NavLink>
        <button className="icon-button" type="button" onClick={logout}>
          <LogOut size={18} />
          <span>Logout</span>
        </button>
      </nav>
    </header>
  );
}
