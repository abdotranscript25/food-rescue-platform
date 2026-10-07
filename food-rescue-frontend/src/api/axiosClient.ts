import axios from 'axios';
import { useAuthStore } from '@/store/authStore';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // ⬅️ TRÈS IMPORTANT : Permet d'envoyer les cookies HttpOnly automatiquement
});

// ❌ On supprime l'intercepteur de requête (plus besoin d'ajouter le Bearer token manuellement)

// Intercepteur réponse : gère l'expiration / déconnexion si 401
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default axiosClient;