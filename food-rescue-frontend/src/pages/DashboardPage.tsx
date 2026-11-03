import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  ShoppingBag,
  CalendarCheck,
  TrendingUp,
  ArrowRight,
  Sparkles,
  ShieldCheck,
  DollarSign,
  Loader2,
  PieChart,
  Activity,
  Store // 👈 Icône ajoutée pour les commerçants
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useAuthStore } from '@/store/authStore';
import { offerApi } from '@/api/offerApi';
import { reservationApi } from '@/api/reservationApi';
import { reviewApi } from '@/api/reviewApi';

function getStoredAuthData() {
  try {
    const storedAuth = localStorage.getItem('food-rescue-auth');
    if (storedAuth) {
      const parsed = JSON.parse(storedAuth);
      return parsed?.state || {};
    }
  } catch (e) {
    return {};
  }
  return {};
}

export default function DashboardPage() {
  const authState = useAuthStore();
  const storedData = getStoredAuthData();

  const rawId = authState.id || storedData.id;
  const userId = rawId ? String(rawId) : null;

  const role = authState.role || storedData.role || '';
  const firstName = authState.firstName || storedData.firstName || 'Utilisateur';
  const lastName = authState.lastName || storedData.lastName || '';

  const isMerchant = role === 'MERCHANT' || role === 'ROLE_MERCHANT';
  const isConsumer = role === 'CONSUMER' || role === 'ROLE_CONSUMER';
  const isAdmin = role === 'ADMIN' || role === 'ROLE_ADMIN';

  const isMfaEnabled = Boolean(authState.mfaEnabled || storedData.mfaEnabled);

  const [foodSaved, setFoodSaved] = useState<number>(0);
  const [activeCount, setActiveCount] = useState<number>(0);
  const [metricValue, setMetricValue] = useState<string | number>('-');
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const [statusCounts, setStatusCounts] = useState({
    completed: 0,
    confirmed: 0,
    cancelled: 0,
    total: 0
  });
  const [totalRevenueOrSpent, setTotalRevenueOrSpent] = useState<number>(0);

  useEffect(() => {
    const fetchDynamicStats = async () => {
      try {
        setIsLoading(true);

        if (isMerchant) {
          if (!userId) {
            setIsLoading(false);
            return;
          }

          try {
            const offers = await offerApi.getOffersByMerchant(userId);
            if (Array.isArray(offers)) {
              const activeOffers = offers.filter((o: any) => {
                const qty = o.remainingQuantity ?? o.quantity;
                return (qty ?? 1) > 0 && o.status !== 'CANCELLED' && o.status !== 'EXPIRED';
              });
              setActiveCount(activeOffers.length);
            }
          } catch (offerErr: any) {
            console.error('Erreur offerApi.getOffersByMerchant :', offerErr);
          }

          try {
            const merchantReservations = await reservationApi.getReservationsByMerchant(userId);
            if (Array.isArray(merchantReservations)) {
              const completedReservations = merchantReservations.filter((r: any) => {
                const st = (r.status || '').toUpperCase();
                return st === 'COMPLETED' || st === 'VALIDATED' || st === 'CONFIRMED';
              });

              const totalQuantityCompleted = completedReservations.reduce((acc: number, r: any) => acc + (r.quantity || 1), 0);
              setFoodSaved(totalQuantityCompleted * 0.5);

              const completedCount = merchantReservations.filter((r: any) => (r.status || '').toUpperCase() === 'COMPLETED').length;
              const confirmedCount = merchantReservations.filter((r: any) => (r.status || '').toUpperCase() === 'CONFIRMED').length;
              const cancelledCount = merchantReservations.filter((r: any) => (r.status || '').toUpperCase() === 'CANCELLED').length;

              setStatusCounts({
                completed: completedCount,
                confirmed: confirmedCount,
                cancelled: cancelledCount,
                total: merchantReservations.length
              });

              const revenue = completedReservations.reduce((acc: number, r: any) => acc + (r.totalPrice || 0), 0);
              setTotalRevenueOrSpent(revenue);
            }
          } catch (resErr: any) {
            console.error('Erreur reservationApi.getReservationsByMerchant :', resErr);
          }

          try {
            const avgRating = await reviewApi.getAverageRating(userId);
            setMetricValue(avgRating ? `${Number(avgRating).toFixed(1)} ★` : '5.0 ★');
          } catch (err) {
            setMetricValue('5.0 ★');
          }

        } else if (isConsumer && userId) {
          const reservations = await reservationApi.getReservationsByUser(userId);
          const activeReservations = reservations.filter((r: any) => r.status === 'PENDING' || r.status === 'CONFIRMED');
          setActiveCount(activeReservations.length);

          const completedReservations = reservations.filter((r: any) => r.status === 'COMPLETED');
          const totalQuantity = completedReservations.reduce((acc: number, r: any) => acc + (r.quantity || 1), 0);
          setFoodSaved(totalQuantity * 0.5);

          const totalSaved = completedReservations.reduce((acc: number, r: any) => {
            if (r.offer && r.offer.originalPrice && r.offer.discountedPrice) {
              const diff = r.offer.originalPrice - r.offer.discountedPrice;
              return acc + (diff > 0 ? diff * (r.quantity || 1) : 0);
            }
            return acc;
          }, 0);

          setMetricValue(`${totalSaved.toFixed(2)} MAD`);

          const completedCount = reservations.filter((r: any) => r.status === 'COMPLETED').length;
          const confirmedCount = reservations.filter((r: any) => r.status === 'CONFIRMED' || r.status === 'PENDING').length;
          const cancelledCount = reservations.filter((r: any) => r.status === 'CANCELLED').length;

          setStatusCounts({
            completed: completedCount,
            confirmed: confirmedCount,
            cancelled: cancelledCount,
            total: reservations.length
          });

          const spent = completedReservations.reduce((acc: number, r: any) => acc + (r.totalPrice || 0), 0);
          setTotalRevenueOrSpent(spent);

        } else if (isAdmin) {
          setActiveCount(12);
          setFoodSaved(340);
          setMetricValue('Actif');
          setStatusCounts({ completed: 45, confirmed: 12, cancelled: 3, total: 60 });
          setTotalRevenueOrSpent(12500);
        }
      } catch (error) {
        console.error('Erreur globale lors du chargement :', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchDynamicStats();
  }, [role, userId, isMerchant, isConsumer, isAdmin]);

  const totalRes = statusCounts.total > 0 ? statusCounts.total : 1;
  const completedPct = Math.round((statusCounts.completed / totalRes) * 100);
  const confirmedPct = Math.round((statusCounts.confirmed / totalRes) * 100);
  const cancelledPct = Math.round((statusCounts.cancelled / totalRes) * 100);

  return (
    <div className="space-y-8 pb-12">
      <div className="relative overflow-hidden bg-gradient-to-r from-green-600 via-emerald-600 to-teal-700 text-white rounded-3xl p-8 lg:p-10 shadow-xl">
        <div className="relative z-10 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/15 backdrop-blur-md text-xs font-medium tracking-wide">
              <Sparkles className="w-3.5 h-3.5 text-yellow-300" />
              <span>Espace {isMerchant ? 'Commerçant Partenaire' : isAdmin ? 'Administration' : 'Consommateur Engagé'}</span>
            </div>
            <h1 className="text-3xl lg:text-4xl font-extrabold tracking-tight">
              Bonjour, {firstName} {lastName} 👋
            </h1>
            <p className="text-green-100 max-w-xl text-sm lg:text-base leading-relaxed">
              {isConsumer && 'Chaque panier sauvé est un geste concret pour la planète. Suivez vos réservations et découvrez nos commerçants partenaires.'}
              {isMerchant && 'Pilotez vos surplus, suivez vos réservations clients et offrez une seconde vie à vos produits.'}
              {isAdmin && 'Supervisez l\'activité globale de la plateforme et suivez l\'impact anti-gaspillage.'}
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            {isConsumer && (
              <>
                <Button asChild size="lg" className="bg-white text-green-700 hover:bg-green-50 font-semibold shadow-md">
                  <Link to="/offers" className="flex items-center gap-2">
                    Explorer les offres <ArrowRight className="w-4 h-4" />
                  </Link>
                </Button>
                <Button asChild size="lg" className="bg-emerald-800 text-white hover:bg-emerald-900 font-semibold shadow-md">
                  <Link to="/merchants" className="flex items-center gap-2">
                    <Store className="w-4 h-4" /> Parcourir les commerçants
                  </Link>
                </Button>
              </>
            )}
            {isMerchant && (
              <Button asChild size="lg" className="bg-white text-emerald-700 hover:bg-emerald-50 font-semibold shadow-md">
                <Link to="/merchant/reservations" className="flex items-center gap-2">
                  Voir les réservations clients <ArrowRight className="w-4 h-4" />
                </Link>
              </Button>
            )}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <Card className="border-0 shadow-sm bg-white">
          <CardContent className="p-6 flex items-center justify-between">
            <div className="space-y-1">
              <p className="text-xs font-medium uppercase tracking-wider text-gray-500">
                {isMerchant ? 'Nourriture Sauvée (Total)' : 'Mon Impact Écologique'}
              </p>
              {isLoading ? (
                <div className="py-2"><Loader2 className="w-5 h-5 animate-spin text-green-600" /></div>
              ) : (
                <>
                  <h4 className="text-2xl font-bold text-gray-900">
                    {foodSaved.toFixed(1)} <span className="text-sm font-normal text-green-600">kg</span>
                  </h4>
                  <p className="text-xs text-emerald-600 font-medium">🌱 Équivalent de {Math.round(foodSaved * 4)} repas</p>
                </>
              )}
            </div>
            <div className="bg-green-50 p-3.5 rounded-2xl">
              <TrendingUp className="w-6 h-6 text-green-600" />
            </div>
          </CardContent>
        </Card>

        <Card className="border-0 shadow-sm bg-white">
          <CardContent className="p-6 flex items-center justify-between">
            <div className="space-y-1">
              <p className="text-xs font-medium uppercase tracking-wider text-gray-500">
                {isMerchant ? 'Paniers Actifs en Ligne' : 'Réservations en Cours'}
              </p>
              {isLoading ? (
                <div className="py-2"><Loader2 className="w-5 h-5 animate-spin text-blue-600" /></div>
              ) : (
                <>
                  <h4 className="text-2xl font-bold text-gray-900">{activeCount}</h4>
                  <p className="text-xs text-blue-600 font-medium">
                    {isMerchant ? 'Disponibles à l\'achat' : 'À récupérer'}
                  </p>
                </>
              )}
            </div>
            <div className="bg-blue-50 p-3.5 rounded-2xl">
              <ShoppingBag className="w-6 h-6 text-blue-600" />
            </div>
          </CardContent>
        </Card>

        <Card className="border-0 shadow-sm bg-white">
          <CardContent className="p-6 flex items-center justify-between">
            <div className="space-y-1">
              <p className="text-xs font-medium uppercase tracking-wider text-gray-500">
                {isMerchant ? 'Note de Confiance' : 'Argent Économisé'}
              </p>
              {isLoading ? (
                <div className="py-2"><Loader2 className="w-5 h-5 animate-spin text-amber-600" /></div>
              ) : (
                <>
                  <h4 className="text-2xl font-bold text-gray-900">{metricValue}</h4>
                  <p className="text-xs text-gray-500 font-medium">
                    {isMerchant ? 'Basé sur les avis clients' : 'Sur vos paniers anti-gaspi'}
                  </p>
                </>
              )}
            </div>
            <div className={`${isMerchant ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600'} p-3.5 rounded-2xl`}>
              {isMerchant ? <ShieldCheck className="w-6 h-6" /> : <DollarSign className="w-6 h-6" />}
            </div>
          </CardContent>
        </Card>

        <Card className="border-0 shadow-sm bg-white">
          <CardContent className="p-6 flex items-center justify-between">
            <div className="space-y-1">
              <p className="text-xs font-medium uppercase tracking-wider text-gray-500">Sécurité du Compte</p>
              <div className="pt-1">
                <Badge className={isMfaEnabled ? "bg-emerald-100 text-emerald-800 border-0" : "bg-amber-100 text-amber-800 border-0"}>
                  {isMfaEnabled ? "Sécurisé (MFA Actif)" : "MFA Inactif"}
                </Badge>
              </div>
              <p className="text-xs text-gray-500 font-medium">
                {isMfaEnabled ? "JWT & MFA Actifs" : "Veuillez configurer votre MFA"}
              </p>
            </div>
            <div className={`${isMfaEnabled ? 'bg-purple-50 text-purple-600' : 'bg-amber-50 text-amber-600'} p-3.5 rounded-2xl`}>
              <ShieldCheck className="w-6 h-6" />
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="border-0 shadow-sm bg-white p-2">
          <CardHeader className="pb-2">
            <CardTitle className="text-base font-bold text-gray-800 flex items-center gap-2">
              <PieChart className="w-5 h-5 text-green-600" />
              <span>Répartition Dynamique des Commandes / Réservations</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-5 pt-2">
            <div className="h-4 w-full bg-gray-100 rounded-full overflow-hidden flex shadow-inner">
              <div style={{ width: `${completedPct}%` }} className="bg-emerald-500 transition-all duration-500" title={`Retirées/Complétées: ${completedPct}%`} />
              <div style={{ width: `${confirmedPct}%` }} className="bg-blue-500 transition-all duration-500" title={`Confirmées: ${confirmedPct}%`} />
              <div style={{ width: `${cancelledPct}%` }} className="bg-red-400 transition-all duration-500" title={`Annulées: ${cancelledPct}%`} />
            </div>

            <div className="grid grid-cols-3 gap-2 pt-2 text-center">
              <div className="bg-emerald-50 p-3 rounded-2xl border border-emerald-100">
                <span className="block text-xs font-semibold text-emerald-800 uppercase">Complétées</span>
                <span className="text-lg font-bold text-emerald-700">{statusCounts.completed}</span>
                <span className="block text-[10px] text-emerald-600 font-medium">({completedPct}%)</span>
              </div>
              <div className="bg-blue-50 p-3 rounded-2xl border border-blue-100">
                <span className="block text-xs font-semibold text-blue-800 uppercase">Confirmées</span>
                <span className="text-lg font-bold text-blue-700">{statusCounts.confirmed}</span>
                <span className="block text-[10px] text-blue-600 font-medium">({confirmedPct}%)</span>
              </div>
              <div className="bg-red-50 p-3 rounded-2xl border border-red-100">
                <span className="block text-xs font-semibold text-red-800 uppercase">Annulées</span>
                <span className="text-lg font-bold text-red-700">{statusCounts.cancelled}</span>
                <span className="block text-[10px] text-red-600 font-medium">({cancelledPct}%)</span>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border-0 shadow-sm bg-white p-2">
          <CardHeader className="pb-2">
            <CardTitle className="text-base font-bold text-gray-800 flex items-center gap-2">
              <Activity className="w-5 h-5 text-emerald-600" />
              <span>{isMerchant ? 'Analyse du Chiffre d\'Affaires' : 'Synthèse Économique & Impact'}</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 pt-2">
            <div className="flex items-center justify-between bg-gray-50 p-4 rounded-2xl border border-gray-100">
              <div className="space-y-1">
                <span className="text-xs font-semibold uppercase tracking-wider text-gray-500">
                  {isMerchant ? 'Revenu Total Encaissé' : 'Total Économisé / Dépensé'}
                </span>
                <h4 className="text-2xl font-extrabold text-gray-900">{totalRevenueOrSpent.toFixed(2)} MAD</h4>
              </div>
              <div className="bg-emerald-100 p-3 rounded-2xl text-emerald-700">
                <DollarSign className="w-6 h-6" />
              </div>
            </div>

            <div className="space-y-2 pt-1">
              <div className="flex justify-between text-xs font-semibold text-gray-600">
                <span>Objectif Carbone & Anti-Gaspi</span>
                <span className="text-green-600">{Math.min(100, Math.round(foodSaved * 10))}% atteint</span>
              </div>
              <div className="h-3 w-full bg-gray-100 rounded-full overflow-hidden p-0.5">
                <div
                  style={{ width: `${Math.min(100, foodSaved * 10)}%` }}
                  className="h-full bg-gradient-to-r from-green-500 to-emerald-400 rounded-full transition-all duration-700"
                />
              </div>
              <p className="text-[11px] text-gray-400 italic">Basé sur le volume total de nourriture sauvée en kilogrammes.</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="space-y-4">
        <h3 className="text-lg font-bold text-gray-800 tracking-tight">Accès Rapides</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">

          <Card className="border border-gray-100 shadow-sm hover:shadow-md transition-all group">
            <CardHeader className="space-y-3">
              <div className="w-12 h-12 rounded-2xl bg-green-50 flex items-center justify-center group-hover:bg-green-600 transition-colors">
                <ShoppingBag className="w-6 h-6 text-green-600 group-hover:text-white transition-colors" />
              </div>
              <CardTitle className="text-xl">Offres disponibles</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm text-gray-500">Parcourez les invendus du jour proposés par les commerçants partenaires.</p>
              <Button asChild variant="outline" className="w-full justify-between group-hover:border-green-600 group-hover:text-green-600">
                <Link to="/offers">
                  <span>Explorer le catalogue</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </Button>
            </CardContent>
          </Card>

          {/* 🏪 NOUVELLE CARTE DE NAVIGATION VERS LES MARCHANDS */}
          {isConsumer && (
            <Card className="border border-gray-100 shadow-sm hover:shadow-md transition-all group">
              <CardHeader className="space-y-3">
                <div className="w-12 h-12 rounded-2xl bg-teal-50 flex items-center justify-center group-hover:bg-teal-600 transition-colors">
                  <Store className="w-6 h-6 text-teal-600 group-hover:text-white transition-colors" />
                </div>
                <CardTitle className="text-xl">Commerçants partenaires</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-sm text-gray-500">Découvrez les commerçants, consultez leurs catalogues, notes et avis clients.</p>
                <Button asChild variant="outline" className="w-full justify-between group-hover:border-teal-600 group-hover:text-teal-600">
                  <Link to="/merchants">
                    <span>Voir les commerçants</span>
                    <ArrowRight className="w-4 h-4" />
                  </Link>
                </Button>
              </CardContent>
            </Card>
          )}

          {isConsumer && (
            <Card className="border border-gray-100 shadow-sm hover:shadow-md transition-all group">
              <CardHeader className="space-y-3">
                <div className="w-12 h-12 rounded-2xl bg-blue-50 flex items-center justify-center group-hover:bg-blue-600 transition-colors">
                  <CalendarCheck className="w-6 h-6 text-blue-600 group-hover:text-white transition-colors" />
                </div>
                <CardTitle className="text-xl">Mes réservations</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-sm text-gray-500">Consultez l'historique de vos paniers réservés et suivez leurs statuts de retrait.</p>
                <Button asChild variant="outline" className="w-full justify-between group-hover:border-blue-600 group-hover:text-blue-600">
                  <Link to="/reservations">
                    <span>Voir mes réservations</span>
                    <ArrowRight className="w-4 h-4" />
                  </Link>
                </Button>
              </CardContent>
            </Card>
          )}

          {isMerchant && (
            <Card className="border border-gray-100 shadow-sm hover:shadow-md transition-all group">
              <CardHeader className="space-y-3">
                <div className="w-12 h-12 rounded-2xl bg-emerald-50 flex items-center justify-center group-hover:bg-emerald-600 transition-colors">
                  <CalendarCheck className="w-6 h-6 text-emerald-600 group-hover:text-white transition-colors" />
                </div>
                <CardTitle className="text-xl">Réservations clients</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-sm text-gray-500">Gérez les commandes de vos clients et validez les retraits en boutique.</p>
                <Button asChild variant="outline" className="w-full justify-between group-hover:border-emerald-600 group-hover:text-emerald-600">
                  <Link to="/merchant/reservations">
                    <span>Suivre les retraits</span>
                    <ArrowRight className="w-4 h-4" />
                  </Link>
                </Button>
              </CardContent>
            </Card>
          )}

        </div>
      </div>
    </div>
  );
}