import { useState, useEffect } from 'react';
import {
  CheckCircle2,
  ShoppingBag,
  User,
  Loader2,
  AlertCircle,
  Clock,
  CalendarCheck
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useAuthStore } from '@/store/authStore';
import { reservationApi } from '@/api/reservationApi';

interface ReservationItem {
  id: number;
  quantity: number;
  status: 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED';
  reservationDate: string;
  consumerId: string; // UUID String
  consumer?: {
    firstName: string;
    lastName: string;
    email: string;
  };
  offer?: {
    title: string;
    discountedPrice: number;
  };
}

export default function MerchantReservationsPage() {
  const merchantIdFromStore = useAuthStore((state: any) => state.id || state.merchantId);
  const email = useAuthStore((state: any) => state.email);

  // Utilisation directe du GUID sous forme de String
  const merchantId = merchantIdFromStore ? String(merchantIdFromStore) : null;

  const [reservations, setReservations] = useState<ReservationItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchMerchantReservations = async () => {
    if (!merchantId) {
      setError("Identifiant du commerçant introuvable dans la session.");
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);

      const data = await reservationApi.getReservationsByMerchant(merchantId);
      setReservations(Array.isArray(data) ? (data as unknown as ReservationItem[]) : []);
    } catch (err: any) {
      console.error("Erreur lors de la récupération des réservations commerçant", err);
      if (err.response?.status === 404) {
        setReservations([]);
      } else if (err.response?.status === 403) {
        setError("Accès refusé (403). Vérifiez que vous êtes bien connecté en tant que commerçant avec les bons droits.");
      } else {
        setError("Impossible de charger les réservations depuis le serveur.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchMerchantReservations();
  }, [merchantId]);

  const handleComplete = async (id: number) => {
    try {
      await reservationApi.completeReservation(id);
      fetchMerchantReservations();
    } catch (err) {
      console.error("Erreur lors de la validation du retrait", err);
    }
  };

  return (
    <div className="space-y-6 pb-12">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-extrabold tracking-tight text-gray-900 flex items-center gap-2">
          <CalendarCheck className="w-7 h-7 text-emerald-600" />
          Suivi des Réservations Clients
        </h1>
        <p className="text-sm text-gray-500">
          Visualisez en temps réel les paniers réservés sur vos offres et validez les retraits en boutique.
        </p>
      </div>

      {isLoading ? (
        <div className="flex flex-col justify-center items-center py-20 space-y-3">
          <Loader2 className="w-8 h-8 animate-spin text-emerald-600" />
          <p className="text-xs text-gray-400">Chargement des réservations en cours...</p>
        </div>
      ) : error ? (
        <div className="p-4 bg-red-50 text-red-700 rounded-xl flex items-center gap-2 text-sm border border-red-200">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      ) : reservations.length === 0 ? (
        <Card className="border-dashed border-2 border-gray-200 bg-white shadow-none">
          <CardContent className="py-16 text-center space-y-3">
            <div className="w-12 h-12 bg-gray-50 rounded-full flex items-center justify-center mx-auto text-gray-400">
              <ShoppingBag className="w-6 h-6" />
            </div>
            <p className="text-gray-700 font-semibold text-base">Aucune réservation active pour le moment.</p>
            <p className="text-xs text-gray-400 max-w-sm mx-auto">
              Dès qu'un client réservera l'un de vos paniers, sa commande s'affichera ici instantanément.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {reservations.map((res) => (
            <Card key={res.id} className="border border-gray-100 shadow-sm hover:shadow-md transition-shadow bg-white">
              <CardContent className="p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-gray-900">Réservation #{res.id}</span>
                    <Badge className={
                      res.status === 'PENDING' ? 'bg-amber-100 text-amber-800 border-0' :
                      res.status === 'CONFIRMED' ? 'bg-blue-100 text-blue-800 border-0' :
                      res.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800 border-0' :
                      'bg-gray-100 text-gray-800 border-0'
                    }>
                      {res.status}
                    </Badge>
                  </div>
                  <h4 className="font-semibold text-emerald-700 text-base">
                    {res.offer?.title || 'Panier Anti-Gaspi'} <span className="text-gray-500 font-normal text-sm">(Quantité : {res.quantity})</span>
                  </h4>
                  <div className="flex flex-wrap items-center gap-4 text-xs text-gray-500 pt-1">
                    <span className="flex items-center gap-1">
                      <User className="w-3.5 h-3.5 text-gray-400" /> Client : <strong className="text-gray-700">{res.consumer ? `${res.consumer.firstName} ${res.consumer.lastName}` : res.consumerId}</strong>
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5 text-gray-400" /> Réservé le : {res.reservationDate ? new Date(res.reservationDate).toLocaleDateString() : 'N/A'}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2 w-full md:w-auto justify-end">
                  {res.status !== 'COMPLETED' && res.status !== 'CANCELLED' && (
                    <Button
                      onClick={() => handleComplete(res.id)}
                      size="sm"
                      className="bg-emerald-600 hover:bg-emerald-700 text-white font-medium shadow-sm"
                    >
                      <CheckCircle2 className="w-4 h-4 mr-1.5" /> Valider le retrait
                    </Button>
                  )}
                  {res.status === 'COMPLETED' && (
                    <span className="text-xs font-semibold text-emerald-600 flex items-center gap-1 bg-emerald-50 px-3 py-1.5 rounded-lg">
                      <CheckCircle2 className="w-4 h-4" /> Retiré avec succès
                    </span>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}