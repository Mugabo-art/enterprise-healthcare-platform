import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import LoginPage from '../LoginPage.jsx';
import { AuthProvider } from '../../../context/AuthContext.jsx';

vi.mock('../../../services/authService.js', () => ({
  login: vi.fn(() => Promise.resolve({ accessToken: 'a', refreshToken: 'r', expiresIn: 900 })),
  verifyMfa: vi.fn(),
  logout: vi.fn(),
}));

function renderLogin() {
  return render(
    <BrowserRouter>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </BrowserRouter>
  );
}

describe('LoginPage', () => {
  it('renders email and password fields', () => {
    renderLogin();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('submits credentials and does not show an error on success', async () => {
    renderLogin();
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'doctor@hospital.test' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'Password123!' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.queryByText(/login failed/i)).not.toBeInTheDocument();
    });
  });
});
