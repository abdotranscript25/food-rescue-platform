export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'EXPIRED';

export interface Reservation {
  id: number;
  consumerId: number;
  offerId: number;
  quantity: number;
  totalPrice: number;
  status: ReservationStatus;
  reservationDate: string;
  cancelledAt?: string;
}

export interface ReservationRequest {
  consumerId: number;
  offerId: number;
  quantity: number;
}