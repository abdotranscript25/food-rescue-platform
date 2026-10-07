import axiosClient from './axiosClient';
import type { Reservation, ReservationRequest } from '@/types/reservation';

export const reservationApi = {
  getAllReservations: async (): Promise<Reservation[]> => {
    const { data } = await axiosClient.get<Reservation[]>('/api/reservations');
    return data;
  },

  getReservationById: async (id: number): Promise<Reservation> => {
    const { data } = await axiosClient.get<Reservation>(`/api/reservations/${id}`);
    return data;
  },

  getReservationsByUser: async (consumerId: number): Promise<Reservation[]> => {
    const { data } = await axiosClient.get<Reservation[]>(`/api/reservations/user/${consumerId}`);
    return data;
  },

  createReservation: async (reservation: ReservationRequest): Promise<Reservation> => {
    // On envoie l'offre et la quantité, le backend récupère l'utilisateur via le token Bearer
    const { data } = await axiosClient.post<Reservation>('/api/reservations', {
      offerId: reservation.offerId,
      quantity: reservation.quantity,
      consumerId: reservation.consumerId // Optionnel si géré par le token
    });
    return data;
  },

  cancelReservation: async (id: number): Promise<Reservation> => {
    const { data } = await axiosClient.put<Reservation>(`/api/reservations/${id}/cancel`);
    return data;
  },

  completeReservation: async (id: number): Promise<Reservation> => {
    const { data } = await axiosClient.put<Reservation>(`/api/reservations/${id}/complete`);
    return data;
  },
};