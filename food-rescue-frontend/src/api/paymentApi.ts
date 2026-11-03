import axiosClient from './axiosClient';

export interface PaymentRequest {
  reservationId: number;
  clientId: number;
  amount: number;
  paymentMethod: string;
}

export interface PaymentResponse {
  id: number;
  reservationId: number;
  clientId: number;
  amount: number;
  currency: string;
  paymentMethod: string;
  status: 'SUCCESS' | 'PENDING' | 'FAILED';
  createdAt: string;
}

export const paymentApi = {
  processPayment: async (data: PaymentRequest): Promise<PaymentResponse> => {
    const response = await axiosClient.post<PaymentResponse>('/api/payments', data);
    return response.data;
  },

  getPaymentByReservation: async (reservationId: number): Promise<PaymentResponse> => {
    const response = await axiosClient.get<PaymentResponse>(`/api/payments/reservation/${reservationId}`);
    return response.data;
  },
};