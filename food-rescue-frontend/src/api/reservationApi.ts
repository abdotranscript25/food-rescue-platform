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

  getReservationsByUser: async (consumerId: string): Promise<Reservation[]> => {
    const { data } = await axiosClient.get<Reservation[]>(`/api/reservations/user/${consumerId}`);
    return data;
  },

  getReservationsByMerchant: async (merchantId: string): Promise<Reservation[]> => {
    const { data } = await axiosClient.get<Reservation[]>(`/api/reservations/merchant/${merchantId}`);
    return data;
  },

  createReservation: async (reservation: ReservationRequest): Promise<Reservation> => {
    const { data } = await axiosClient.post<Reservation>('/api/reservations', {
      offerId: reservation.offerId,
      quantity: reservation.quantity,
      consumerId: reservation.consumerId
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