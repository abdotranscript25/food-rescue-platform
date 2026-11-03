export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'EXPIRED';

export interface OfferResponse {
  id: number;
  productId?: number;
  merchantId?: string; // 👈 Modifié en string (UUID)
  title?: string;
  description?: string;
  imageUrl?: string;
  originalPrice?: number;
  discountedPrice?: number;
  quantity?: number;
  remainingQuantity?: number;
  status?: string;
  merchant?: {
    id: string; // 👈 Modifié en string (UUID)
    firstName?: string;
    lastName?: string;
  };
}

export interface Reservation {
  id: number;
  consumerId: string; // 👈 Modifié en string (UUID)
  offerId: number;
  quantity: number;
  totalPrice: number;
  status: ReservationStatus;
  paymentStatus?: 'SUCCESS' | 'PENDING' | 'FAILED'; // 👈 Ajouté pour le paiement
  reservationDate: string;
  cancelledAt?: string;
  offer?: OfferResponse;
  consumer?: {
    id: string; // 👈 Modifié en string (UUID)
    email?: string;
    firstName?: string;
    lastName?: string;
  };
}

export interface ReservationRequest {
  consumerId?: string; // 👈 Modifié en string (UUID)
  offerId: number;
  quantity: number;
}