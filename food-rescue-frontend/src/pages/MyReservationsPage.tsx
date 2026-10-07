import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ShoppingBag, Search, Ban, CheckCircle, Clock } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { reservationApi } from '@/api/reservationApi';
import { useAuthStore } from '@/store/authStore';

export default function MyReservationsPage() {
  const [search, setSearch] = useState('');
  const { user } = useAuthStore();
  const queryClient = useQueryClient();

  // Extraction robuste de l'ID consommateur
  const rawId = user?.id ?? (user as any)?.sub ?? 1;
  const consumerId = typeof rawId === 'string' ? parseInt(rawId, 10) : rawId;

  // Récupération des réservations de l'utilisateur connecté
  const { data: reservations = [], isLoading } = useQuery({
    queryKey: ['my-reservations', consumerId],
    queryFn: async () => {
      const response = await reservationApi.getReservationsByUser(consumerId);
      // S'adapte si l'API retourne { data: [...] } ou directement le tableau [...]
      return response.data || response;
    },
    enabled: !!consumerId,
  });

  // Mutation pour annuler une réservation
  const cancelMutation = useMutation({
    mutationFn: (id: number) => reservationApi.cancelReservation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-reservations', consumerId] });
      queryClient.invalidateQueries({ queryKey: ['available-offers'] });
      alert('Réservation annulée avec succès.');
    },
    onError: (error: any) => {
      alert(error?.response?.data?.message || "Erreur lors de l'annulation de la réservation.");
    },
  });

  // Filtrage intelligent
  const filtered = Array.isArray(reservations) ? reservations.filter((res) => {
    const statusMatch = res.status?.toLowerCase().includes(search.toLowerCase());
    const idMatch = res.id?.toString().includes(search);
    return statusMatch || idMatch;
  }) : [];

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-blue-50 text-blue-700">
            <Clock className="w-3 h-3" /> Confirmée
          </span>
        );
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-green-50 text-green-700">
            <CheckCircle className="w-3 h-3" /> Retirée / Complétée
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
            <Card key={reservation.id} className="p-5 flex flex-col justify-between bg-white shadow-sm hover:shadow-md transition-shadow">
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-gray-500">RÉSERVATION #{reservation.id}</span>
                  {getStatusBadge(reservation.status)}
                </div>

                <div className="text-sm space-y-1 text-gray-600">
                  <p><strong>Offre ID :</strong> {reservation.offerId}</p>
                  <p><strong>Quantité :</strong> {reservation.quantity} unité(s)</p>
                  <p className="text-base font-semibold text-green-600">
                    Total : {reservation.totalPrice} MAD
                  </p>
                  <p className="text-xs text-gray-400">
                    Réservé le : {reservation.reservationDate ? new Date(reservation.reservationDate).toLocaleString() : 'N/A'}
                  </p>
                </div>
              </div>

              {reservation.status === 'CONFIRMED' && (
                <div className="mt-5 pt-3 border-t border-gray-100 flex gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    className="w-full text-red-600 border-red-200 hover:bg-red-50 gap-1 text-xs"
                    onClick={() => {
                      if (window.confirm("Voulez-vous vraiment annuler cette réservation ?")) {
                        cancelMutation.mutate(reservation.id);
                      }
                    }}
                    disabled={cancelMutation.isPending}
                  >
                    <Ban className="w-3.5 h-3.5" />
                    Annuler la réservation
                  </Button>
                </div>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}