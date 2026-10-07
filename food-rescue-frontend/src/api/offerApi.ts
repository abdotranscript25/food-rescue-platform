import axiosClient from './axiosClient';
import type { Offer, OfferWithDetails } from '@/types/offer';

export const offerApi = {
  getAvailableOffers: async (): Promise<OfferWithDetails[]> => {
    const { data } = await axiosClient.get<OfferWithDetails[]>('/api/offers/available');
    return data;
  },

  getOfferById: async (id: number): Promise<OfferWithDetails> => {
    const { data } = await axiosClient.get<OfferWithDetails>(`/api/offers/${id}`);
    return data;
  },

  getOffersByMerchant: async (merchantId: number): Promise<OfferWithDetails[]> => {
    const { data } = await axiosClient.get<OfferWithDetails[]>(`/api/offers/merchant/${merchantId}`);
    return data;
  },

  createOffer: async (offer: Partial<Offer>): Promise<OfferWithDetails> => {
    const { data } = await axiosClient.post<OfferWithDetails>('/api/offers', offer);
    return data;
  },

  // Ajout de la fonction de mise à jour d'une offre existante
  updateOffer: async (id: number, offer: Partial<Offer>): Promise<OfferWithDetails> => {
    const { data } = await axiosClient.put<OfferWithDetails>(`/api/offers/${id}`, offer);
    return data;
  },

  publishOffer: async (id: number): Promise<OfferWithDetails> => {
    const { data } = await axiosClient.put<OfferWithDetails>(`/api/offers/${id}/publish`);
    return data;
  },

  cancelOffer: async (id: number): Promise<OfferWithDetails> => {
    const { data } = await axiosClient.put<OfferWithDetails>(`/api/offers/${id}/cancel`);
    return data;
  },

  deleteOffer: async (id: number): Promise<void> => {
    await axiosClient.delete(`/api/offers/${id}`);
  },
};