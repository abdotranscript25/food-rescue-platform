export interface Payment {
  id: number;
  reservationId: number;
  clientId: number;
  amount: number;
  currency: string;
  paymentMethod: string;
  status: 'SUCCESS' | 'PENDING' | 'FAILED';
  createdAt?: string;
  checkoutUrl?: string; // ⬅️ AJOUTÉ
}

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
  checkoutUrl?: string; // ⬅️ AJOUTÉ
}