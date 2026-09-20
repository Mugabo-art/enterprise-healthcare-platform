import { Routes, Route, Navigate } from 'react-router-dom';
import LandingPage from './components/Landing/LandingPage.jsx';
import LoginPage from './components/Login/LoginPage.jsx';
import RegisterPage from './components/Register/RegisterPage.jsx';
import DashboardPage from './components/Dashboard/DashboardPage.jsx';
import { useAuth } from './context/AuthContext.jsx';

function ProtectedRoute({ children }) {
  const { accessToken } = useAuth();
  return accessToken ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route
        path="/login"
        element={
          <div className="app-shell">
            <LoginPage />
          </div>
        }
      />
      <Route
        path="/register"
        element={
          <div className="app-shell">
            <RegisterPage />
          </div>
        }
      />
      <Route
        path="/dashboard"
        element={
          <div className="app-shell">
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          </div>
        }
      />
    </Routes>
  );
}
