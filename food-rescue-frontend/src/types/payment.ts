export interface Payment {
  id: number;
  reservationId: number;
  clientId: number;
  amount: number;
  currency: string;
  paymentMethod: string;
  status: 'SUCCESS' | 'PENDING' | 'FAILED';
  createdAt?: string;
}