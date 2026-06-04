import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import NavBar from './NavBar';
import { api } from '../api/client';

jest.mock('../api/client', () => ({
  api: {
    autocomplete: jest.fn(),
    cart: jest.fn(),
    logout: jest.fn()
  }
}));

function LocationDisplay() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname}</div>;
}

describe('NavBar', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    jest.mocked(api.cart).mockResolvedValue({
      status: 'success',
      message: 'Cart loaded.',
      cartItems: []
    });
  });

  it('navigates to the selected autocomplete movie', async () => {
    jest.mocked(api.autocomplete).mockResolvedValue([
      {
        value: 'Spider-Man',
        data: { movieId: 'tt0145487' }
      }
    ]);

    render(
      <MemoryRouter initialEntries={['/']}>
        <NavBar />
        <Routes>
          <Route path="*" element={<LocationDisplay />} />
        </Routes>
      </MemoryRouter>
    );

    await userEvent.type(screen.getByLabelText(/search movies/i), 'Spider');
    const suggestion = await screen.findByRole('button', { name: 'Spider-Man' });
    await userEvent.click(suggestion);

    expect(screen.getByTestId('location')).toHaveTextContent('/movies/tt0145487');
  });

  it('submits a normal search when no suggestion is selected', async () => {
    jest.mocked(api.autocomplete).mockResolvedValue([]);

    render(
      <MemoryRouter initialEntries={['/']}>
        <NavBar />
        <Routes>
          <Route path="*" element={<LocationDisplay />} />
        </Routes>
      </MemoryRouter>
    );

    await userEvent.type(screen.getByLabelText(/search movies/i), 'Matrix');
    await userEvent.keyboard('{Enter}');

    await waitFor(() => {
      expect(screen.getByTestId('location')).toHaveTextContent('/movies');
    });
  });

  it('shows the current cart item count', async () => {
    jest.mocked(api.autocomplete).mockResolvedValue([]);
    jest.mocked(api.cart).mockResolvedValue({
      status: 'success',
      message: 'Cart loaded.',
      cartItems: [
        {
          movieId: 'tt15239678',
          title: 'Dune: Part Two',
          year: 2024,
          director: 'Denis Villeneuve',
          rating: 8.4,
          quantity: 2
        },
        {
          movieId: 'tt15398776',
          title: 'Oppenheimer',
          year: 2023,
          director: 'Christopher Nolan',
          rating: 8.2,
          quantity: 1
        }
      ]
    });

    render(
      <MemoryRouter initialEntries={['/']}>
        <NavBar />
      </MemoryRouter>
    );

    expect(await screen.findByText('3')).toBeInTheDocument();
  });
});
