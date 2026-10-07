import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Tag, Plus, Search, CheckCircle2, Ban, Trash2, Pencil } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import OfferCard from '@/components/offers/OfferCard';
import OfferFormDialog from '@/components/offers/OfferFormDialog';
import { offerApi } from '@/api/offerApi';
import { useAuthStore } from '@/store/authStore';
import type { OfferWithDetails } from '@/types/offer';

export default function MyOffersPage() {
  const [search, setSearch] = useState('');
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [selectedOffer, setSelectedOffer] = useState<OfferWithDetails | null>(null);

  const { user } = useAuthStore();
  const queryClient = useQueryClient();

  const merchantId = user?.id ?? 1;

  // Récupération des offres du marchand
  const { data: offers = [], isLoading } = useQuery({
    queryKey: ['merchant-offers', merchantId],
    queryFn: () => offerApi.getOffersByMerchant(merchantId),
  });

  // Mutation pour publier une offre (DRAFT -> AVAILABLE)
  const publishMutation = useMutation({
    mutationFn: (id: number) => offerApi.publishOffer(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['merchant-offers', merchantId] });
    },
  });

  // Mutation pour annuler une offre
  const cancelMutation = useMutation({
    mutationFn: (id: number) => offerApi.cancelOffer(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['merchant-offers', merchantId] });
    },
  });

  // Mutation pour supprimer définitivement une offre
  const deleteMutation = useMutation({
    mutationFn: (id: number) => offerApi.deleteOffer(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['merchant-offers', merchantId] });
    },
  });

  // Filtrage intelligent (recherche sur le nom du produit ou la description)
  const filtered = offers.filter((o) => {
    const nameMatch = o.product?.name?.toLowerCase().includes(search.toLowerCase()) ?? false;
    const titleMatch = o.title?.toLowerCase().includes(search.toLowerCase()) ?? false;
    const descMatch =
      (o.product?.description?.toLowerCase().includes(search.toLowerCase()) ?? false) ||
      (o.description?.toLowerCase().includes(search.toLowerCase()) ?? false);
    return nameMatch || titleMatch || descMatch;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl lg:text-3xl font-bold text-gray-900">
            Mes offres anti-gaspillage
          </h1>
          <p className="text-gray-500 mt-1">
            Gérez vos offres, suivez les stocks et publiez vos réductions
          </p>
        </div>

        <Button
          onClick={() => {
            setSelectedOffer(null); // Mode création
            setIsDialogOpen(true);
          }}
          className="bg-green-600 hover:bg-green-700 gap-2 shadow-sm"
        >
          <Plus className="w-4 h-4" />
          Nouvelle offre
        </Button>
      </div>

      {/* Barre de recherche */}
      <div className="relative max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <Input
          placeholder="Rechercher par nom de produit..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-10 bg-white"
        />
      </div>

      {/* Chargement par Skeletons */}
      {isLoading && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <Card key={i} className="p-4 space-y-3">
              <Skeleton className="h-40 w-full rounded-xl" />
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-3 w-1/2" />
            </Card>
          ))}
        </div>
      )}

      {/* État vide */}
      {!isLoading && offers.length === 0 && (
        <Card className="p-16 text-center border-dashed bg-white shadow-sm">
          <Tag className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-gray-900 mb-1">
            Aucune offre pour le moment
          </h3>
          <p className="text-sm text-gray-500 mb-6 max-w-md mx-auto">
            Commencez par créer votre première offre à partir de votre catalogue de produits enregistrés.
          </p>
          <Button
            onClick={() => {
              setSelectedOffer(null);
              setIsDialogOpen(true);
            }}
            className="bg-green-600 hover:bg-green-700 gap-2"
          >
            <Plus className="w-4 h-4" />
            Créer ma première offre
          </Button>
        </Card>
      )}

      {/* Liste des offres avec boutons de gestion par statut */}
      {!isLoading && filtered.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {filtered.map((offer) => (
            <div key={offer.id} className="flex flex-col justify-between bg-white rounded-2xl border border-gray-100 shadow-sm p-3 hover:shadow-md transition-shadow">
              <OfferCard offer={offer} />

              {/* Actions de gestion du marchand */}
              <div className="flex items-center gap-1.5 mt-4 pt-3 border-t border-gray-100">
                {offer.status === 'DRAFT' && (
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1 text-blue-600 border-blue-200 hover:bg-blue-50 gap-1 text-xs px-2"
                    onClick={() => publishMutation.mutate(offer.id)}
                    disabled={publishMutation.isPending}
                  >
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    Publier
                  </Button>
                )}

                {offer.status !== 'CANCELLED' && (
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1 text-red-600 border-red-200 hover:bg-red-50 gap-1 text-xs px-2"
                    onClick={() => cancelMutation.mutate(offer.id)}
                    disabled={cancelMutation.isPending}
                  >
                    <Ban className="w-3.5 h-3.5" />
                    Annuler
                  </Button>
                )}

                {offer.status === 'CANCELLED' && (
                  <span className="flex-1 text-center text-xs font-medium text-gray-400 py-1.5 bg-gray-50 rounded-lg">
                    Annulée
                  </span>
                )}

                {/* Bouton Modifier */}
                <Button
                  size="sm"
                  variant="outline"
                  className="text-gray-600 border-gray-200 hover:bg-gray-50 px-2"
                  onClick={() => {
                    setSelectedOffer(offer); // Mode édition
                    setIsDialogOpen(true);
                  }}
                  title="Modifier l'offre"
                >
                  <Pencil className="w-3.5 h-3.5" />
                </Button>

                {/* Bouton de suppression définitive */}
                <Button
                  size="sm"
                  variant="outline"
                  className="text-gray-500 border-gray-200 hover:bg-red-50 hover:text-red-600 hover:border-red-200 px-2"
                  onClick={() => {
                    if (window.confirm("Voulez-vous vraiment supprimer cette offre ?")) {
                      deleteMutation.mutate(offer.id);
                    }
                  }}
                  disabled={deleteMutation.isPending}
                  title="Supprimer l'offre"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Dialog de création ou de modification d'offre */}
      <OfferFormDialog
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
        offerToEdit={selectedOffer}
      />
    </div>
  );
}