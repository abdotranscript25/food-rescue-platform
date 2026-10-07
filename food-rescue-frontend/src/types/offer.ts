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
  merchantId: number;
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

// Vue enrichie (Offer + Product + Merchant)
export interface OfferWithDetails extends Offer {
  product?: {
    id: number;
    name: string;
    category: string | null;
    description: string | null;
    imageUrl: string | null;
  };
  merchant?: {
    id: number;
    firstName: string;
    lastName: string;
    city: string | null;
  };
}