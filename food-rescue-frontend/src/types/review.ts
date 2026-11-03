export interface Review {
  id: number;
  merchantId: string; // UUID du marchand noté
  consumerId: string; // UUID du consommateur qui note
  rating: number;     // Note de 1 à 5
  comment: string;
  createdAt?: string;
}