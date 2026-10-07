import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Clock, ShoppingBag, Store, Plus, Minus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { offerApi } from '@/api/offerApi';
import { reservationApi } from '@/api/reservationApi';
import { useAuthStore } from '@/store/authStore';

export default function OfferDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, role } = useAuthStore();
  const queryClient = useQueryClient();

  const [offer, setOffer] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [quantity, setQuantity] = useState(1);

  useEffect(() => {
    if (!id) return;

    offerApi.getOfferById(Number(id))
      .then((data) => {
        setOffer(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Erreur chargement offre:", err);
        setError(true);
        setLoading(false);
      });
  }, [id]);

  // Mutation pour créer la réservation
  const reservationMutation = useMutation({
    mutationFn: async () => {
      if (!isAuthenticated) {
        throw new Error("Veuillez vous connecter en tant que consommateur pour réserver.");
      }
      return await reservationApi.createReservation({
        offerId: Number(id),
        quantity: quantity,
      } as any);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-reservations'] });
      queryClient.invalidateQueries({ queryKey: ['available-offers'] });

      alert("🎉 Panier sauvé avec succès ! Un e-mail de confirmation vous a été envoyé.");
      navigate('/consumer/reservations');
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err.message || "Erreur lors de la réservation.";
      alert(`Erreur : ${msg}`);
    },
  });

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-6 space-y-4">
        <Skeleton className="h-8 w-32" />
        <Skeleton className="h-96 w-full rounded-2xl" />
      </div>
    );
  }

  if (error || !offer) {
    return (
      <div className="max-w-4xl mx-auto p-12 text-center space-y-4">
        <h2 className="text-xl font-bold text-gray-900">Offre introuvable</h2>
        <p className="text-gray-500">Cette offre n'existe plus ou l'ID est invalide.</p>
        <Button onClick={() => navigate(-1)} variant="outline">
          Retour
        </Button>
      </div>
    );
  }

  const discount =
    offer.originalPrice > 0
      ? Math.round(
          ((offer.originalPrice - offer.discountedPrice) / offer.originalPrice) * 100
        )
      : 0;

  const maxStock = offer.remainingQuantity ?? 0;

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      {/* Bouton de retour */}
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors font-medium"
      >
        <ArrowLeft className="w-5 h-5" />
        <span>Retour</span>
      </button>

      <Card className="overflow-hidden border border-gray-100 shadow-sm grid grid-cols-1 md:grid-cols-2 gap-6 p-6 bg-white">
        {/* Image du produit */}
        <div className="relative h-72 md:h-full min-h-[300px] bg-gray-100 rounded-xl overflow-hidden flex items-center justify-center">
          {offer.product?.imageUrl ? (
            <img
              src={offer.product.imageUrl}
              alt={offer.product.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <span className="text-6xl">🍽️</span>
          )}

          {discount > 0 && (
            <span className="absolute top-4 left-4 bg-red-500 text-white font-bold px-3 py-1 rounded-full text-sm shadow">
              -{discount}%
            </span>
          )}
        </div>

        {/* Détails */}
        <div className="flex flex-col justify-between space-y-4">
          <div className="space-y-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-green-600 bg-green-50 px-2.5 py-1 rounded-md">
              {offer.product?.category || 'Anti-gaspillage'}
            </span>
            <h1 className="text-2xl font-bold text-gray-900">
              {offer.product?.name || offer.title || 'Offre spéciale'}
            </h1>
            <p className="text-gray-600 text-sm leading-relaxed">
              {offer.product?.description || offer.description || 'Profitez de cette offre anti-gaspillage de qualité.'}
            </p>
          </div>

          {/* Prix */}
          <div className="flex items-baseline gap-3">
            <span className="text-3xl font-extrabold text-green-600">
              {(offer.discountedPrice * quantity).toFixed(2)} MAD
            </span>
            {quantity === 1 && (
              <span className="text-lg text-gray-400 line-through">
                {offer.originalPrice?.toFixed(2)} MAD
              </span>
            )}
            <span className="text-xs text-gray-500">({offer.discountedPrice?.toFixed(2)} MAD / unité)</span>
          </div>

          {/* Infos pratiques */}
          <div className="space-y-2 text-sm text-gray-600 border-t border-gray-100 pt-4">
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-orange-500" />
              <span>Expire le : <strong>{new Date(offer.expiresAt).toLocaleString('fr-FR')}</strong></span>
            </div>
            <div className="flex items-center gap-2">
              <ShoppingBag className="w-4 h-4 text-blue-500" />
              <span>Quantité restante : <strong>{maxStock} unités</strong></span>
            </div>
            {offer.merchant && (
              <div className="flex items-center gap-2">
                <Store className="w-4 h-4 text-green-600" />
                <span>Commerçant : <strong>{offer.merchant.firstName} {offer.merchant.lastName}</strong></span>
              </div>
            )}
          </div>

          {/* Section Réservation : Uniquement visible pour les consommateurs (CONSUMER) */}
          {role === 'CONSUMER' && (
            maxStock > 0 ? (
              <div className="space-y-3 pt-4 border-t border-gray-100">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-700">Quantité :</span>
                  <div className="flex items-center gap-3 border border-gray-200 rounded-lg p-1 bg-gray-50">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8"
                      onClick={() => setQuantity(Math.max(1, quantity - 1))}
                      disabled={quantity <= 1}
                    >
                      <Minus className="w-3.5 h-3.5" />
                    </Button>
                    <span className="font-semibold text-gray-900 w-6 text-center">{quantity}</span>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8"
                      onClick={() => setQuantity(Math.min(maxStock, quantity + 1))}
                      disabled={quantity >= maxStock}
                    >
                      <Plus className="w-3.5 h-3.5" />
                    </Button>
                  </div>
                </div>

                <Button
                  className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-3 rounded-xl shadow-sm transition-all"
                  onClick={() => reservationMutation.mutate()}
                  disabled={reservationMutation.isPending}
                >
                  {reservationMutation.isPending ? "Réservation en cours..." : "Réserver ce panier 🛒"}
                </Button>
              </div>
            ) : (
              <div className="p-3 bg-red-50 border border-red-100 text-red-600 text-center font-medium rounded-xl text-sm">
                Épuisé - Toutes les portions ont été sauvées !
              </div>
            )
          )}
        </div>
      </Card>
    </div>
  );
}