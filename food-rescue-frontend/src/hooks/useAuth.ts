import { useAuthStore } from '@/store/authStore';
import { useNavigate } from 'react-router-dom';

export const useAuth = () => {
  const navigate = useNavigate();
  const {
    id,
    email,
    role,
    firstName,
    lastName,
    mfaEnabled,
    isAuthenticated,
    setAuth,
    logout
  } = useAuthStore();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return {
    id,
    merchantId: id, // Alias pratique pour les pages marchandes
    email,
    role,
    firstName,
    lastName,
    mfaEnabled,
    isAuthenticated,
    setAuth,
    logout: handleLogout,
  };
};