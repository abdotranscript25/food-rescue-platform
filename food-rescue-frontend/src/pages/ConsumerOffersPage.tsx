import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ShoppingBag, Search, Tag, Calendar } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { offerApi } from '@/api/offerApi';
import { reservationApi } from '@/api/reservationApi';
import { useAuthStore } from '@/store/authStore';

export default function ConsumerOffersPage() {
  const [search, setSearch] = useState('');
  const [quantities, setQuantities] = useState<{ [key: number]: number }>({});
  const { user } = useAuthStore();
  const queryClient = useQueryClient();

  // Extraction robuste et sécurisée de l'ID consommateur
  const rawId = user?.id ?? (user as any)?.sub;
  const consumerId = typeof rawId === 'string' ? parseInt(rawId, 10) : rawId;

  // Récupération via offerApi centralisé
  const { data: offers = [], isLoading } = useQuery({
    queryKey: ['available-offers'],
    queryFn: () => offerApi.getAvailableOffers(),
  });

  const reservationMutation = useMutation({
    mutationFn: ({ offerId, quantity }: { offerId: number; quantity: number }) => {
      if (!consumerId) {
        throw new Error("Utilisateur non authentifié.");
      }
      return reservationApi.createReservation({
        consumerId,
        offerId,
        quantity,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['available-offers'] });
      queryClient.invalidateQueries({ queryKey: ['my-reservations'] });
      alert('Panier sauvé avec succès ! 🎉 Retrouvez-le dans "Mes Réservations".');
    },
    onError: (error: any) => {
      alert(error?.response?.data?.message || "Erreur lors de la réservation de l'offre.");
    },
  });

  const handleQuantityChange = (offerId: number, qty: number) => {
    setQuantities((prev) => ({ ...prev, [offerId]: qty }));
  };

  const handleReserve = (offer: any) => {
    if (!user || !consumerId) {
      alert('Veuillez vous connecter en tant que consommateur pour réserver.');
      return;
    }
    const qtyToReserve = quantities[offer.id] || 1;
    if (qtyToReserve > (offer.remainingQuantity ?? 0)) {
      alert('La quantité demandée dépasse le stock restant.');
      return;
    }
    reservationMutation.mutate({ offerId: offer.id, quantity: qtyToReserve });
  };

  const filteredOffers = offers.filter(
    (offer: any) =>
      offer.title?.toLowerCase().includes(search.toLowerCase()) ||
      offer.description?.toLowerCase().includes(search.toLowerCase()) ||
      offer.product?.name?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl lg:text-3xl font-bold text-gray-900">Offres disponibles</h1>
        <p className="text-gray-500 mt-1">
          Découvrez les surplus alimentaires près de chez vous
        </p>
      </div>

      <div className="relative max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <Input
          placeholder="Rechercher un produit, une catégorie, un commerce..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-10 bg-white"
        />
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 3 }).map((_, i) => (
            <Card key={i} className="p-5 space-y-4">
              <Skeleton className="h-6 w-2/3" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-10 w-full" />
            </Card>
          ))}
        </div>
      )}

      {!isLoading && filteredOffers.length === 0 && (
        <Card className="p-16 text-center border-dashed bg-white shadow-sm">
          <ShoppingBag className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-gray-900 mb-1">Aucune offre disponible</h3>
          <p className="text-sm text-gray-500 max-w-md mx-auto">
            Il n'y a pas d'offres en ligne pour le moment. Revenez un peu plus tard !
          </p>
        </Card>
      )}

      {!isLoading && filteredOffers.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredOffers.map((offer: any) => {
            const currentQty = quantities[offer.id] || 1;
            const discountPercentage =
              offer.originalPrice && offer.originalPrice > 0
                ? Math.round(
                    ((offer.originalPrice - offer.discountedPrice) / offer.originalPrice) * 100
                  )
                : 0;

            const maxStock = offer.remainingQuantity ?? 0;

            return (
              <Card key={offer.id} className="p-5 flex flex-col justify-between bg-white shadow-sm hover:shadow-md transition-shadow">
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-green-50 text-green-700">
                      {offer.product?.category || 'Anti-gaspillage'}
                    </span>
                    {discountPercentage > 0 && (
                      <span className="text-xs font-bold px-2 py-0.5 rounded bg-red-100 text-red-600">
                        -{discountPercentage}%
                      </span>
                    )}
                  </div>

                  <h3 className="text-lg font-semibold text-gray-900">
                    {offer.product?.name || offer.title || 'Offre spéciale'}
                  </h3>
                  <p className="text-sm text-gray-500 line-clamp-2">
                    {offer.product?.description || offer.description}
                  </p>

                  <div className="flex items-center gap-2 pt-2">
                    <span className="text-xl font-bold text-green-600">
                      {offer.discountedPrice} MAD
                    </span>
                    {offer.originalPrice && (
                      <span className="text-sm text-gray-400 line-through">
                        {offer.originalPrice} MAD
                      </span>
                    )}
                  </div>

                  <div className="text-xs text-gray-500 space-y-1 pt-2 border-t border-gray-100">
                    <p className="flex items-center gap-1.5">
                      <Tag className="w-3.5 h-3.5 text-gray-400" />
                      <span>Stock restant : <strong>{maxStock} / {offer.quantity}</strong></span>
                    </p>
                    <p className="flex items-center gap-1.5">
                      <Calendar className="w-3.5 h-3.5 text-gray-400" />
                      <span>Expire le : {new Date(offer.expiresAt).toLocaleDateString('fr-FR')}</span>
                    </p>
                  </div>
                </div>

                {/* Section d'action de réservation */}
                <div className="mt-5 pt-3 border-t border-gray-100 flex items-center gap-3">
                  <input
                    type="number"
                    min={1}
                    max={maxStock || 1}
                    value={currentQty}
                    onChange={(e) => handleQuantityChange(offer.id, Number(e.target.value))}
                    className="w-16 p-2 text-sm border rounded-md text-center bg-gray-50 focus:bg-white"
                    disabled={maxStock <= 0}
                  />
                  <Button
                    onClick={() => handleReserve(offer)}
                    disabled={reservationMutation.isPending || maxStock <= 0}
                    className="flex-1 bg-green-600 hover:bg-green-700 gap-2 text-white"
                  >
                    <ShoppingBag className="w-4 h-4" />
                    {reservationMutation.isPending ? 'Réservation...' : maxStock > 0 ? 'Réserver' : 'Épuisé'}
                  </Button>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}