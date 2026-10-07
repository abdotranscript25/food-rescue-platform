import axiosClient from './axiosClient';
import type { UserProfile } from '@/types/user';

export const userApi = {
  getProfileById: async (id: number): Promise<UserProfile> => {
    const { data } = await axiosClient.get<UserProfile>(`/api/users/profiles/${id}`);
    return data;
  },

  getProfileByEmail: async (email: string): Promise<UserProfile> => {
    const { data } = await axiosClient.get<UserProfile>(`/api/users/profiles/email/${email}`);
    return data;
  },
};