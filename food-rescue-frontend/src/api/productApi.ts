import axiosClient from './axiosClient';

export interface ProductResponse {
  id: number;
  name: string;
  description: string;
  category: string;
  imageUrl?: string;
  allergens?: string;
  isPerishable?: boolean;
  merchantId?: number;
  createdAt?: string;
}

export interface ProductRequest {
  name: string;
  description: string;
  category: string;
  imageUrl?: string;
  allergens?: string;
  isPerishable?: boolean;
  merchantId?: number;
}

export const productApi = {
  getAllProducts: async (): Promise<ProductResponse[]> => {
    const { data } = await axiosClient.get<ProductResponse[]>('/api/products');
    return data;
  },

  getProductsByMerchant: async (merchantId: number): Promise<ProductResponse[]> => {
    const { data } = await axiosClient.get<ProductResponse[]>(`/api/products/merchant/${merchantId}`);
    return data;
  },

  getProductById: async (id: number): Promise<ProductResponse> => {
    const { data } = await axiosClient.get<ProductResponse>(`/api/products/${id}`);
    return data;
  },

  getProductsByCategory: async (category: string): Promise<ProductResponse[]> => {
    const { data } = await axiosClient.get<ProductResponse[]>(`/api/products/category/${category}`);
    return data;
  },

  createProduct: async (request: ProductRequest): Promise<ProductResponse> => {
    const { data } = await axiosClient.post<ProductResponse>('/api/products', request);
    return data;
  },

  updateProduct: async (id: number, request: ProductRequest): Promise<ProductResponse> => {
    const { data } = await axiosClient.put<ProductResponse>(`/api/products/${id}`, request);
    return data;
  },

  deleteProduct: async (id: number): Promise<void> => {
    await axiosClient.delete(`/api/products/${id}`);
  },
};