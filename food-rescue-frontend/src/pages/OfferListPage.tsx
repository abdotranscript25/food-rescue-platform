import { useState, useMemo } from 'react';
import { Search, PackageOpen } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { Card } from '@/components/ui/card';
import OfferCard from '@/components/offers/OfferCard';
import { offerApi } from '@/api/offerApi';
import { productApi } from '@/api/productApi';
import { userApi } from '@/api/userApi';
import type { OfferWithDetails } from '@/types/offer';
import { useQuery } from '@tanstack/react-query';

export default function OfferListPage() {
  const [search, setSearch] = useState('');

  const { data: offers = [], isLoading, error } = useQuery({
    queryKey: ['offers', 'available'],
    queryFn: async (): Promise<OfferWithDetails[]> => {
      const offers = await offerApi.getAvailableOffers();

      const enriched = await Promise.all(
        offers.map(async (offer): Promise<OfferWithDetails> => {
          const [product, merchant] = await Promise.all([
            offer.productId
              ? productApi.getProductById(offer.productId).catch(() => undefined)
              : Promise.resolve(undefined),
            userApi
              .getProfileById(offer.merchantId)
              .catch(() => undefined),
          ]);
          return { ...offer, product, merchant };
        })
      );

      return enriched;
    },
    staleTime: 60_000,
  });

  const filtered = useMemo(() => {
    if (!search.trim()) return offers;
    const q = search.toLowerCase();
    return offers.filter(
      (o) =>
        o.product?.name?.toLowerCase().includes(q) ||
        o.product?.category?.toLowerCase().includes(q) ||
        o.merchant?.firstName?.toLowerCase().includes(q) ||
        o.merchant?.lastName?.toLowerCase().includes(q) ||
        o.title?.toLowerCase().includes(q)
    );
  }, [offers, search]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl lg:text-3xl font-bold text-gray-900">
            Offres disponibles
          </h1>
          <p className="text-gray-500 mt-1">
            Découvrez les surplus alimentaires près de chez vous
          </p>
        </div>
      </div>

      <div className="relative max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <Input
          placeholder="Rechercher un produit, une catégorie, un commerce..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-10 h-11 bg-white"
        />
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <Card key={i} className="overflow-hidden">
              <Skeleton className="aspect-video w-full" />
              <div className="p-4 space-y-3">
                <Skeleton className="h-3 w-1/3" />
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-1/2" />
                <Skeleton className="h-6 w-1/2" />
              </div>
            </Card>
          ))}
        </div>
      )}

      {error && !isLoading && (
        <Card className="p-12 text-center">
          <p className="text-red-500 font-medium">
            Erreur lors du chargement des offres.
          </p>
          <p className="text-sm text-gray-500 mt-2">
            Vérifiez que le backend est bien démarré.
          </p>
        </Card>
      )}

      {!isLoading && !error && filtered.length === 0 && (
        <Card className="p-16 text-center border-dashed bg-white">
          <PackageOpen className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-gray-900 mb-1">
            {search ? 'Aucun résultat' : 'Aucune offre disponible'}
          </h3>
          <p className="text-sm text-gray-500">
            {search
              ? 'Essayez avec d\'autres mots-clés.'
              : 'Revenez plus tard, de nouvelles offres arrivent bientôt.'}
          </p>
        </Card>
      )}

      {!isLoading && !error && filtered.length > 0 && (
        <>
          <p className="text-sm text-gray-500">
            {filtered.length} offre{filtered.length > 1 ? 's' : ''} disponible{filtered.length > 1 ? 's' : ''}
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {filtered.map((offer) => (
              <OfferCard key={offer.id} offer={offer} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}