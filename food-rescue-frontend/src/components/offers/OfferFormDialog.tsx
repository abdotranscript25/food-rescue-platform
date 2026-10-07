import { useState, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { productApi } from '@/api/productApi';
import { offerApi } from '@/api/offerApi';
import { useAuthStore } from '@/store/authStore';
import type { OfferWithDetails } from '@/types/offer';

interface OfferFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  offerToEdit?: OfferWithDetails | null; // Reçoit l'offre à modifier (optionnel)
}

export default function OfferFormDialog({ open, onOpenChange, offerToEdit }: OfferFormDialogProps) {
  const { user } = useAuthStore();
  const queryClient = useQueryClient();
  const merchantId = user?.id ?? 1;

  const isEditing = !!offerToEdit;

  const [productId, setProductId] = useState<number | ''>('');
  const [originalPrice, setOriginalPrice] = useState<number>(0);
  const [discountedPrice, setDiscountedPrice] = useState<number>(0);
  const [quantity, setQuantity] = useState<number>(1);
  const [expiresAt, setExpiresAt] = useState<string>('');

  // Remplir ou réinitialiser le formulaire à l'ouverture ou au changement d'offre à éditer
  useEffect(() => {
    if (offerToEdit) {
      setProductId(offerToEdit.productId ?? '');
      setOriginalPrice(offerToEdit.originalPrice ?? 0);
      setDiscountedPrice(offerToEdit.discountedPrice ?? 0);
      setQuantity(offerToEdit.quantity ?? 1);
      setExpiresAt(
        offerToEdit.expiresAt ? new Date(offerToEdit.expiresAt).toISOString().slice(0, 16) : ''
      );
    } else {
      setProductId('');
      setOriginalPrice(0);
      setDiscountedPrice(0);
      setQuantity(1);
      setExpiresAt('');
    }
  }, [offerToEdit, open]);

  const { data: products = [] } = useQuery({
    queryKey: ['products'],
    queryFn: productApi.getAllProducts,
  });

  // Mutation pour la création
  const createMutation = useMutation({
    mutationFn: offerApi.createOffer,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['merchant-offers', merchantId] });
      onOpenChange(false);
    },
  });

  // Mutation pour la mise à jour
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<OfferWithDetails> }) =>
      offerApi.updateOffer(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['merchant-offers', merchantId] });
      onOpenChange(false);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!productId || !expiresAt) return;

    const selectedProduct = products.find((p) => p.id === productId);

    const payload = {
      merchantId,
      productId: Number(productId),
      title: selectedProduct?.name ?? offerToEdit?.title ?? 'Offre',
      description: selectedProduct?.description ?? offerToEdit?.description,
      originalPrice,
      discountedPrice,
      quantity,
      remainingQuantity: isEditing ? offerToEdit.remainingQuantity : quantity,
      expiresAt: new Date(expiresAt).toISOString(),
    };

    if (isEditing && offerToEdit) {
      updateMutation.mutate({ id: offerToEdit.id, data: payload });
    } else {
      createMutation.mutate({
        ...payload,
        status: 'AVAILABLE',
      });
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Modifier l'offre" : 'Créer une nouvelle offre'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-sm font-medium text-gray-700">Produit</label>
            <select
              className="w-full mt-1 p-2 border rounded-md text-sm bg-white"
              value={productId}
              onChange={(e) => setProductId(Number(e.target.value))}
              required
            >
              <option value="">Sélectionner un produit</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.price} MAD)
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="text-sm font-medium text-gray-700">Prix original (MAD)</label>
              <Input
                type="number"
                step="0.01"
                value={originalPrice}
                onChange={(e) => setOriginalPrice(Number(e.target.value))}
                required
              />
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700">Prix soldé (MAD)</label>
              <Input
                type="number"
                step="0.01"
                value={discountedPrice}
                onChange={(e) => setDiscountedPrice(Number(e.target.value))}
                required
              />
            </div>
          </div>

          <div>
            <label className="text-sm font-medium text-gray-700">Quantité disponible</label>
            <Input
              type="number"
              min={1}
              value={quantity}
              onChange={(e) => setQuantity(Number(e.target.value))}
              required
            />
          </div>

          <div>
            <label className="text-sm font-medium text-gray-700">Date d'expiration</label>
            <Input
              type="datetime-local"
              value={expiresAt}
              onChange={(e) => setExpiresAt(e.target.value)}
              required
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Annuler
            </Button>
            <Button type="submit" className="bg-green-600 hover:bg-green-700" disabled={isPending}>
              {isPending ? 'Enregistrement...' : 'Enregistrer'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}