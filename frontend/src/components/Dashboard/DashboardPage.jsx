import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listPatients } from '../../services/patientService.js';
import { useAuth } from '../../context/AuthContext.jsx';

export default function DashboardPage() {
  const [patients, setPatients] = useState([]);
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');
  const { logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/');
  }

  useEffect(() => {
    listPatients(search)
      .then((page) => setPatients(page.content ?? []))
      .catch((err) => setError(err.response?.data?.message || 'Failed to load patients'));
  }, [search]);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>Patients</h2>
        <button onClick={handleLogout}>Log out</button>
      </div>
      <input
        placeholder="Search by last name"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        style={{ marginBottom: '1rem', width: '100%' }}
      />
      {error && <p className="error">{error}</p>}
      <ul>
        {patients.map((p) => (
          <li key={p.id}>
            {p.firstName} {p.lastName} — {p.dateOfBirth}
          </li>
        ))}
        {patients.length === 0 && !error && <li>No patients found.</li>}
      </ul>
    </div>
  );
}
