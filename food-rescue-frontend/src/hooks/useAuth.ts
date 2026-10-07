import { useAuthStore } from '@/store/authStore';
import { useNavigate } from 'react-router-dom';

export const useAuth = () => {
  const navigate = useNavigate();
  const { token, email, role, firstName, lastName, isAuthenticated, setAuth, logout } =
    useAuthStore();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return {
    token,
    email,
    role,
    firstName,
    lastName,
    isAuthenticated,
    setAuth,
    logout: handleLogout,
  };
};