export interface Product {
  id: number;
  name: string;
  category: string | null;
  description: string | null;
  imageUrl: string | null;
  allergens: string | null;
  isPerishable: boolean;
  merchantId?: string; // 👈 Modifié en string (UUID)
  createdAt: string;
}