import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { LoginPage } from './LoginPage';
import { api } from '../api/client';

jest.mock('../api/client', () => ({
  ApiError: class ApiError extends Error {
    status = 401;
  },
  api: {
    login: jest.fn(),
    employeeLogin: jest.fn()
  }
}));

describe('LoginPage', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('submits credentials and navigates to the home route', async () => {
    jest.mocked(api.login).mockResolvedValue({ status: 'success', message: 'Login successful.' });

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<h1>Home Route</h1>} />
        </Routes>
      </MemoryRouter>
    );

    await userEvent.type(screen.getByLabelText(/email/i), 'terry@test.com');
    await userEvent.type(screen.getByLabelText(/^password$/i, { selector: 'input' }), 'password');
    await userEvent.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(api.login).toHaveBeenCalledWith('terry@test.com', 'password');
    });
    expect(await screen.findByRole('heading', { name: /home route/i })).toBeInTheDocument();
  });

  it('shows login errors without leaving the page', async () => {
    jest.mocked(api.login).mockRejectedValue(new Error('Incorrect password.'));

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    await userEvent.type(screen.getByLabelText(/email/i), 'terry@test.com');
    await userEvent.type(screen.getByLabelText(/^password$/i, { selector: 'input' }), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: /login/i }));

    expect(await screen.findByText('Login failed.')).toBeInTheDocument();
  });
});
