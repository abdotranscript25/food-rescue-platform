import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from '@/pages/LoginPage';
import RegisterPage from '@/pages/RegisterPage';
import MfaPage from '@/pages/MfaPage';
import MfaSecurityPage from '@/pages/MfaSecurityPage';
import DashboardPage from '@/pages/DashboardPage';
import OfferListPage from '@/pages/OfferListPage';
import OfferDetailPage from '@/pages/OfferDetailPage';
import MyProductsPage from '@/pages/MyProductsPage';
import MyOffersPage from '@/pages/MyOffersPage';
import ConsumerOffersPage from '@/pages/ConsumerOffersPage';
import MyReservationsPage from '@/pages/MyReservationsPage';
import MerchantReservationsPage from '@/pages/MerchantReservationsPage';
import ProfilePage from '@/pages/ProfilePage';
import { MerchantsPage } from '@/pages/MerchantsPage'; // 👈 Import de la page Marchands
import MainLayout from '@/components/layout/MainLayout';
import ProtectedRoute from './ProtectedRoute';

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        {/* ============================================ */}
        {/* Routes publiques                             */}
        {/* ============================================ */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/mfa" element={<MfaPage />} />

        {/* ============================================ */}
        {/* Routes protégées globalement                 */}
        {/* ============================================ */}
        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            {/* Dashboard accessible à tous les connectés */}
            <Route path="/dashboard" element={<DashboardPage />} />

            {/* Sécurité et MFA */}
            <Route path="/security" element={<MfaSecurityPage />} />

            {/* Profil utilisateur */}
            <Route path="/profile" element={<ProfilePage />} />

            {/* Offres générales */}
            <Route path="/offers" element={<OfferListPage />} />
            <Route path="/offers/:id" element={<OfferDetailPage />} />

            {/* ============================================ */}
            {/* Routes - Réservé aux CONSOMMATEURS           */}
            {/* ============================================ */}
            <Route element={<ProtectedRoute allowedRoles={['ROLE_CONSUMER', 'CONSUMER', 'ADMIN', 'ROLE_ADMIN']} />}>
              <Route path="/consumer/offers" element={<ConsumerOffersPage />} />
              <Route path="/reservations" element={<MyReservationsPage />} />
              <Route path="/merchants" element={<MerchantsPage />} /> {/* 👈 NOUVELLE ROUTE MARCHANDS */}
            </Route>

            {/* ============================================ */}
            {/* Réservations Clients - Réservé aux MARCHANDS */}
            {/* ============================================ */}
            <Route element={<ProtectedRoute allowedRoles={['ROLE_MERCHANT', 'MERCHANT', 'ADMIN', 'ROLE_ADMIN']} />}>
              <Route path="/merchant/reservations" element={<MerchantReservationsPage />} />
              <Route path="/my-products" element={<MyProductsPage />} />
              <Route path="/my-offers" element={<MyOffersPage />} />
            </Route>
          </Route>
        </Route>

        {/* ============================================ */}
        {/* Redirections automatiques                    */}
        {/* ============================================ */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}