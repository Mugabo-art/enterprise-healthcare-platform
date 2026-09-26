import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import SchedulePage, { filterByView } from '../SchedulePage.jsx';
import { getSchedule } from '../../../services/doctorService.js';
import { updateVisit } from '../../../services/visitService.js';

const mockAuth = { user: { id: 'doc-1', email: 'doc@hospital.test', role: 'DOCTOR' }, logout: vi.fn() };
vi.mock('../../../context/AuthContext.jsx', () => ({
  useAuth: () => mockAuth,
}));
vi.mock('../../../services/doctorService.js', () => ({ getSchedule: vi.fn() }));
vi.mock('../../../services/patientService.js', () => ({
  getPatient: vi.fn(() => Promise.resolve({ id: 'p1', firstName: 'Ada', lastName: 'Lovelace' })),
}));
vi.mock('../../../services/visitService.js', () => ({ updateVisit: vi.fn() }));

const NOW = new Date(2026, 8, 26, 12, 0);

function visit(id, date, status = 'SCHEDULED') {
  return { id, patientId: 'p1', visitDate: date.toISOString(), visitType: 'OUTPATIENT', reason: `Reason ${id}`, status };
}

describe('filterByView', () => {
  const past = visit('past', new Date(2026, 8, 25, 9, 0));
  const laterToday = visit('later', new Date(2026, 8, 26, 16, 0));
  const earlierToday = visit('earlier', new Date(2026, 8, 26, 8, 0));
  const nextWeek = visit('next', new Date(2026, 9, 2, 10, 0));
  const tomorrow = visit('tomorrow', new Date(2026, 8, 27, 10, 0));
  const all = [nextWeek, tomorrow, laterToday, earlierToday, past];

  it('keeps only today, earliest first', () => {
    expect(filterByView(all, 'today', NOW).map((v) => v.id)).toEqual(['earlier', 'later']);
  });

  it('lists future days soonest first', () => {
    expect(filterByView(all, 'upcoming', NOW).map((v) => v.id)).toEqual(['tomorrow', 'next']);
  });

  it('puts anything before today in past', () => {
    expect(filterByView(all, 'past', NOW).map((v) => v.id)).toEqual(['past']);
  });
});

describe('SchedulePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("loads the signed-in doctor's schedule and resolves patient names", async () => {
    getSchedule.mockResolvedValue([visit('v1', new Date())]);
    render(<MemoryRouter><SchedulePage /></MemoryRouter>);

    expect(await screen.findByText('Ada Lovelace')).toBeInTheDocument();
    expect(getSchedule).toHaveBeenCalledWith('doc-1');
  });

  it('starts a scheduled visit and reflects the new status', async () => {
    const v = visit('v1', new Date());
    getSchedule.mockResolvedValue([v]);
    updateVisit.mockResolvedValue({ ...v, status: 'IN_PROGRESS' });
    render(<MemoryRouter><SchedulePage /></MemoryRouter>);

    fireEvent.click(await screen.findByRole('button', { name: 'Start' }));

    await waitFor(() => expect(screen.getByRole('button', { name: 'Complete' })).toBeInTheDocument());
    expect(updateVisit).toHaveBeenCalledWith('p1', 'v1', { status: 'IN_PROGRESS' });
  });
});
