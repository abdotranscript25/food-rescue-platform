import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ShoppingBag, Search, Ban, CheckCircle, Clock, Star, Store } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { reservationApi } from '@/api/reservationApi';
import { offerApi } from '@/api/offerApi';
import { useAuthStore } from '@/store/authStore';
import { ReviewSection } from '@/components/ui/ReviewSection';

function ReservationCard({ reservation, consumerId, onCancel }: { reservation: any; consumerId: string; onCancel: (id: number) => void }) {
  const [showReview, setShowReview] = useState(false);

  const { data: offerDetails } = useQuery({
    queryKey: ['offer-details-forced', reservation.offerId],
    queryFn: async () => {
      if (!reservation.offerId) return null;
      try {
        const res = await offerApi.getOfferById(reservation.offerId);
        return res.data || res;
      } catch (e) {
        return null;
      }
    },
    enabled: !!reservation.offerId,
  });

  const offerData = offerDetails || reservation.offer;

  const offerTitle =
    offerData?.product?.name ||
    offerData?.title ||
    reservation.offer?.title ||
    `Panier #${reservation.offerId}`;

  let rawImage =
    offerData?.imageUrl ||
    offerData?.photoUrl ||
    offerData?.photo ||
    offerData?.picture ||
    offerData?.product?.imageUrl ||
    offerData?.image;

  const offerImage = rawImage
    ? (rawImage.startsWith('http') ? rawImage : `http://localhost:8080${rawImage.startsWith('/') ? '' : '/'}${rawImage}`)
    : null;

  const merchantName =
    offerData?.merchant?.firstName ? `${offerData.merchant.firstName} ${offerData.merchant.lastName}` :
    offerData?.merchantName ||
    null;

  // L'ID du marchand est désormais une chaîne (UUID)
  const merchantId = offerData?.merchant?.id ?? offerData?.merchantId;

  const currentStatus = reservation.status?.toUpperCase();
  const isCompleted = currentStatus === 'COMPLETED' || currentStatus === 'DELIVERED' || currentStatus === 'VALIDATED';

  const getStatusBadge = (status: string) => {
    const s = status?.toUpperCase();
    switch (s) {
      case 'CONFIRMED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-blue-50 text-blue-700">
            <Clock className="w-3 h-3" /> Confirmée
          </span>
        );
      case 'COMPLETED':
      case 'DELIVERED':
      case 'VALIDATED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-green-50 text-green-700">
            <CheckCircle className="w-3 h-3" /> Retirée
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-red-50 text-red-700">
            <Ban className="w-3 h-3" /> Annulée
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-gray-50 text-gray-700">
            {status}
          </span>
        );
    }
  };

  return (
    <Card className="overflow-hidden bg-white shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between">
      <div>
        <div className="relative h-44 w-full bg-gray-100 overflow-hidden">
          {offerImage ? (
            <img
              src={offerImage}
              alt={offerTitle}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-400 bg-gray-50">
              <ShoppingBag className="w-10 h-10" />
            </div>
          )}
          <div className="absolute top-3 right-3">
            {getStatusBadge(reservation.status)}
          </div>
        </div>

        <div className="p-5 space-y-3">
          <span className="text-xs font-bold text-gray-400">RÉSERVATION #{reservation.id}</span>

          <div className="space-y-1">
            <h4 className="font-bold text-gray-900 text-base">{offerTitle}</h4>
            {merchantName && (
              <p className="text-xs text-green-600 font-medium flex items-center gap-1">
                <Store className="w-3.5 h-3.5" /> {merchantName}
              </p>
            )}
          </div>

          <div className="text-sm space-y-1 text-gray-600 border-t border-gray-50 pt-2">
            <p><strong>Quantité :</strong> {reservation.quantity} unité(s)</p>
            <p className="text-base font-semibold text-green-600">
              Total : {reservation.totalPrice} MAD
            </p>
            <p className="text-xs text-gray-400">
              Réservé le : {reservation.reservationDate ? new Date(reservation.reservationDate).toLocaleString('fr-FR') : 'N/A'}
            </p>
          </div>
        </div>
      </div>

      <div className="p-5 pt-0 mt-auto space-y-2">
        {currentStatus === 'CONFIRMED' && (
          <Button
            size="sm"
            variant="outline"
            className="w-full text-red-600 border-red-200 hover:bg-red-50 gap-1 text-xs"
            onClick={() => {
              if (window.confirm("Voulez-vous vraiment annuler cette réservation ?")) {
                onCancel(reservation.id);
              }
            }}
          >
            <Ban className="w-3.5 h-3.5" />
            Annuler la réservation
          </Button>
        )}

        {isCompleted && merchantId && (
          <>
            <Button
              size="sm"
              className="w-full bg-yellow-500 hover:bg-yellow-600 text-white gap-1 text-xs font-medium"
              onClick={() => setShowReview(!showReview)}
            >
              <Star className="w-3.5 h-3.5 fill-current" />
              {showReview ? "Fermer l'avis" : "Noter mon expérience"}
            </Button>

            {showReview && (
              <div className="mt-3 pt-3 border-t border-dashed bg-gray-50 p-3 rounded-lg">
                <ReviewSection
                  merchantId={merchantId}
                  clientId={consumerId}
                  canReview={true}
                />
              </div>
            )}
          </>
        )}
      </div>
    </Card>
  );
}

export default function MyReservationsPage() {
  const [search, setSearch] = useState('');

  const authId = useAuthStore((state) => state.id);
  const queryClient = useQueryClient();

  // 💡 Récupération directe de l'UUID sous forme de string
  const consumerId = authId ? String(authId) : '';

  const { data: reservations = [], isLoading } = useQuery({
    queryKey: ['my-reservations', consumerId],
    queryFn: async () => {
      const response = await reservationApi.getReservationsByUser(consumerId);
      return response.data || response;
    },
    enabled: !!consumerId,
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => reservationApi.cancelReservation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-reservations', consumerId] });
      alert('Réservation annulée avec succès.');
    },
    onError: (error: any) => {
      alert(error?.response?.data?.message || "Erreur lors de l'annulation de la réservation.");
    },
  });

  const filtered = Array.isArray(reservations) ? reservations.filter((res) => {
    const statusMatch = res.status?.toLowerCase().includes(search.toLowerCase());
    const idMatch = res.id?.toString().includes(search);
    return statusMatch || idMatch;
  }) : [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl lg:text-3xl font-bold text-gray-900">Mes Réservations</h1>
        <p className="text-gray-500 mt-1">
          Suivez l'état de vos paniers sauvés et gérez vos retraits
        </p>
      </div>

      <div className="relative max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <Input
          placeholder="Rechercher par statut (confirmée, annulée...)..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-10 bg-white"
        />
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 3 }).map((_, i) => (
            <Card key={i} className="p-5 space-y-4">
              <Skeleton className="h-36 w-full rounded-lg" />
              <Skeleton className="h-6 w-1/3" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-10 w-full" />
            </Card>
          ))}
        </div>
      )}

      {!isLoading && filtered.length === 0 && (
        <Card className="p-16 text-center border-dashed bg-white shadow-sm">
          <ShoppingBag className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-gray-900 mb-1">Aucune réservation pour le moment</h3>
          <p className="text-sm text-gray-500 mb-6 max-w-md mx-auto">
            Vous n'avez pas encore sauvé de paniers. Parcourez les offres disponibles pour commencer !
          </p>
        </Card>
      )}

      {!isLoading && filtered.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filtered.map((reservation) => (
            <ReservationCard
              key={reservation.id}
              reservation={reservation}
              consumerId={consumerId}
              onCancel={(id) => cancelMutation.mutate(id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}