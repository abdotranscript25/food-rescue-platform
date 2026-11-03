import axiosClient from './axiosClient';
import type { UserProfile } from '@/types/user';
import type { Offer } from '@/types/offer';
import type { Review } from '@/types/review';

export const merchantApi = {
  // 💡 Récupérer uniquement les marchands via le nouvel endpoint back-end
  getAllProfiles: async (): Promise<UserProfile[]> => {
    const response = await axiosClient.get<UserProfile[]>('/api/users/profiles/merchants');
    return response.data;
  },

  // 2. Récupérer un profil spécifique par son ID
  getProfileById: async (id: string): Promise<UserProfile> => {
    const response = await axiosClient.get<UserProfile>(`/api/users/profiles/${id}`);
    return response.data;
  },

  // 3. Récupérer les offres/produits d'un marchand spécifique
  getOffersByMerchant: async (merchantId: string): Promise<Offer[]> => {
    const response = await axiosClient.get<Offer[]>(`/api/offers/merchant/${merchantId}`);
    return response.data;
  },

  // 4. Récupérer les avis d'un marchand
  getReviewsByMerchant: async (merchantId: string): Promise<Review[]> => {
    const response = await axiosClient.get<Review[]>(`/api/reviews/merchant/${merchantId}`);
    return response.data;
  },

  // 5. Récupérer la note moyenne d'un marchand
  getAverageRating: async (merchantId: string): Promise<number> => {
    const response = await axiosClient.get<number>(`/api/reviews/merchant/${merchantId}/average`);
    return response.data;
  },
};