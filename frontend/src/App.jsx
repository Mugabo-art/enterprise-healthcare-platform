import { Routes, Route, Navigate } from 'react-router-dom';
import LandingPage from './components/Landing/LandingPage.jsx';
import LoginPage from './components/Login/LoginPage.jsx';
import RegisterPage from './components/Register/RegisterPage.jsx';
import DashboardPage from './components/Dashboard/DashboardPage.jsx';
import PatientDetailPage from './components/Patient/PatientDetailPage.jsx';
import { useAuth } from './context/AuthContext.jsx';

function ProtectedRoute({ children }) {
  const { accessToken, loading } = useAuth();
  if (loading) return null;
  return accessToken ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/dashboard/patients/:id"
        element={
          <ProtectedRoute>
            <PatientDetailPage />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}
