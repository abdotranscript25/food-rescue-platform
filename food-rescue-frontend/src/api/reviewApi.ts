import axiosClient from './axiosClient';

export interface Review {
  id?: number;
  rating: number;
  comment: string;
  clientId: string;   // 👈 Modifié en string (UUID)
  merchantId: string; // 👈 Modifié en string (UUID)
  createdAt?: string;
}

export const reviewApi = {
  async createReview(review: Review): Promise<Review> {
    const response = await axiosClient.post('/api/reviews', review);
    return response.data;
  },

  async getReviewsByMerchant(merchantId: string): Promise<Review[]> {
    const response = await axiosClient.get(`/api/reviews/merchant/${merchantId}`);
    return response.data;
  },

  async getAverageRating(merchantId: string): Promise<number> {
    const response = await axiosClient.get(`/api/reviews/merchant/${merchantId}/average`);
    return response.data;
  }
};