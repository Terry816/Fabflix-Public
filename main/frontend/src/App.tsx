import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import NavBar from './components/NavBar';
import AdminNav from './components/AdminNav';
import HomePage from './pages/HomePage';
import BrowsePage from './pages/BrowsePage';
import DiscoverPage from './pages/DiscoverPage';
import MovieListPage from './pages/MovieListPage';
import MovieDetailPage from './pages/MovieDetailPage';
import StarDetailPage from './pages/StarDetailPage';
import CartPage from './pages/CartPage';
import PaymentPage from './pages/PaymentPage';
import ConfirmationPage from './pages/ConfirmationPage';
import { EmployeeLoginPage, LoginPage } from './pages/LoginPage';
import { AddGenrePage, AddMoviePage, AddStarPage, DashboardPage } from './pages/AdminPages';

function CustomerLayout() {
  return (
    <>
      <NavBar />
      <Outlet />
    </>
  );
}

function AdminLayout() {
  return (
    <>
      <AdminNav />
      <Outlet />
    </>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/employee/login" element={<EmployeeLoginPage />} />

      <Route element={<CustomerLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/browse" element={<BrowsePage />} />
        <Route path="/discover" element={<DiscoverPage />} />
        <Route path="/movies" element={<MovieListPage />} />
        <Route path="/movies/:movieId" element={<MovieDetailPage />} />
        <Route path="/stars/:starId" element={<StarDetailPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/payment" element={<PaymentPage />} />
        <Route path="/confirmation" element={<ConfirmationPage />} />
      </Route>

      <Route path="/employee" element={<AdminLayout />}>
        <Route index element={<Navigate to="/employee/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="add-movie" element={<AddMoviePage />} />
        <Route path="add-star" element={<AddStarPage />} />
        <Route path="add-genre" element={<AddGenrePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
