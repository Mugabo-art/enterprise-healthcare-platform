import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import RegisterPage from '../RegisterPage.jsx';

const mockUseAuth = vi.fn();
vi.mock('../../../context/AuthContext.jsx', () => ({ useAuth: () => mockUseAuth() }));

const register = vi.fn();
vi.mock('../../../services/authService.js', () => ({ register: (...args) => register(...args) }));

function renderPage() {
  return render(
    <MemoryRouter>
      <RegisterPage />
    </MemoryRouter>
  );
}

function fill(password = 'Password123!', confirm = password) {
  fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'new@hospital.test' } });
  fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: password } });
  fireEvent.change(screen.getByLabelText(/confirm password/i), { target: { value: confirm } });
}

describe('RegisterPage', () => {
  beforeEach(() => {
    register.mockReset().mockResolvedValue({});
  });

  it('does not offer a role picker to anonymous visitors and registers them as PATIENT', async () => {
    mockUseAuth.mockReturnValue({ user: null });
    renderPage();
    expect(screen.queryByLabelText(/^role$/i)).not.toBeInTheDocument();

    fill();
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => expect(register).toHaveBeenCalledWith('new@hospital.test', 'Password123!', 'PATIENT'));
  });

  it('never offers a privileged role to signed-in non-admin staff', async () => {
    mockUseAuth.mockReturnValue({ user: { role: 'DOCTOR' } });
    renderPage();
    expect(screen.queryByLabelText(/^role$/i)).not.toBeInTheDocument();

    fill();
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => expect(register).toHaveBeenCalledWith('new@hospital.test', 'Password123!', 'PATIENT'));
  });

  it('lets an administrator choose a staff role', async () => {
    mockUseAuth.mockReturnValue({ user: { role: 'ADMIN' } });
    renderPage();

    fireEvent.change(screen.getByLabelText(/^role$/i), { target: { value: 'DOCTOR' } });
    fill();
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => expect(register).toHaveBeenCalledWith('new@hospital.test', 'Password123!', 'DOCTOR'));
  });

  it('rejects mismatched passwords without calling the API', async () => {
    mockUseAuth.mockReturnValue({ user: null });
    renderPage();

    fill('Password123!', 'Different123!');
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument();
    expect(register).not.toHaveBeenCalled();
  });

  it('shows the server error message when registration fails', async () => {
    mockUseAuth.mockReturnValue({ user: null });
    register.mockRejectedValue({ response: { data: { message: 'Email already registered' } } });
    renderPage();

    fill();
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText(/email already registered/i)).toBeInTheDocument();
  });
});
