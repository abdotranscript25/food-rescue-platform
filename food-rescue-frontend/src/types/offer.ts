export type OfferStatus =
  | 'DRAFT'
  | 'PUBLISHED'
  | 'AVAILABLE'
  | 'SOLD_OUT'
  | 'EXPIRED'
  | 'CANCELLED';

export interface Offer {
  id: number;
  productId: number | null;
  merchantId: string; // 👈 Modifié en string (UUID)
  title: string | null;
  description: string | null;
  originalPrice: number;
  discountedPrice: number;
  quantity: number;
  remainingQuantity: number;
  availableFrom: string;
  expiresAt: string;
  status: OfferStatus;
}

export interface OfferWithDetails extends Offer {
  product?: {
    id: number;
    name: string;
    category: string | null;
    description: string | null;
    imageUrl: string | null;
  };
  merchant?: {
    id: string; // 👈 Modifié en string (UUID)
    firstName: string;
    lastName: string;
    city: string | null;
  };
}