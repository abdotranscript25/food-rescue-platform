export interface UserProfile {
  id: string; // 👈 Modifié en string (UUID)
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  address: string | null;
  city: string | null;
  createdAt: string;
  updatedAt: string;
}