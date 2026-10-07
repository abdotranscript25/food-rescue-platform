import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { Loader2, Image as ImageIcon } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { productApi, type CreateProductRequest } from '@/api/productApi';
import { useQueryClient } from '@tanstack/react-query';

const productSchema = z.object({
  name: z.string().min(2, 'Nom requis (min. 2 caractères)'),
  category: z.string().optional(),
  description: z.string().optional(),
  imageUrl: z.string().url('URL invalide').optional().or(z.literal('')),
  allergens: z.string().optional(),
  isPerishable: z.boolean().default(true),
});

type ProductForm = z.infer<typeof productSchema>;

interface ProductFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export default function ProductFormDialog({ open, onOpenChange }: ProductFormDialogProps) {
  const queryClient = useQueryClient();
  const [isLoading, setIsLoading] = useState(false);
  const [previewError, setPreviewError] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<ProductForm>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      name: '',
      category: '',
      description: '',
      imageUrl: '',
      allergens: '',
      isPerishable: true,
    },
  });

  const imageUrl = watch('imageUrl');

  // Reset previewError quand l'URL change
  useEffect(() => {
    setPreviewError(false);
  }, [imageUrl]);

  // Reset le formulaire quand le dialog se ferme
  useEffect(() => {
    if (!open) {
      reset();
      setPreviewError(false);
    }
  }, [open, reset]);

  const onSubmit = async (data: ProductForm) => {
    setIsLoading(true);
    try {
      const payload: CreateProductRequest = {
        name: data.name,
        category: data.category || undefined,
        description: data.description || undefined,
        imageUrl: data.imageUrl || undefined,
        allergens: data.allergens || undefined,
        isPerishable: data.isPerishable,
      };

      await productApi.createProduct(payload);

      toast.success(`Produit "${data.name}" créé avec succès ! 🎉`);
      await queryClient.invalidateQueries({ queryKey: ['products'] });
      onOpenChange(false);
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        'Erreur lors de la création du produit.';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Créer un nouveau produit</DialogTitle>
          <DialogDescription>
            Ajoutez un produit à votre catalogue pour pouvoir créer des offres ensuite.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Nom + Catégorie */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="name">
                Nom du produit <span className="text-red-500">*</span>
              </Label>
              <Input
                id="name"
                placeholder="Ex: Pizza Margherita"
                {...register('name')}
              />
              {errors.name && (
                <p className="text-xs text-red-500">{errors.name.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="category">Catégorie</Label>
              <Input
                id="category"
                placeholder="Ex: Pizza"
                {...register('category')}
              />
            </div>
          </div>

          {/* Description */}
          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <textarea
              id="description"
              rows={3}
              placeholder="Décrivez votre produit..."
              className="w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              {...register('description')}
            />
          </div>

          {/* URL Image + Preview */}
          <div className="space-y-2">
            <Label htmlFor="imageUrl">URL de l'image</Label>
            <div className="relative">
              <ImageIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <Input
                id="imageUrl"
                placeholder="https://images.unsplash.com/photo-..."
                className="pl-10"
                {...register('imageUrl')}
              />
            </div>
            {errors.imageUrl && (
              <p className="text-xs text-red-500">{errors.imageUrl.message}</p>
            )}
            <p className="text-xs text-gray-500">
              💡 Collez l'URL d'une image (Unsplash, Imgur...)
            </p>

            {/* Preview */}
            {imageUrl && !previewError && (
              <div className="mt-2 border rounded-lg overflow-hidden bg-gray-50">
                <img
                  src={imageUrl}
                  alt="Preview"
                  className="w-full h-48 object-cover"
                  onError={() => setPreviewError(true)}
                />
              </div>
            )}
            {imageUrl && previewError && (
              <div className="mt-2 p-4 border border-red-200 bg-red-50 rounded-lg text-sm text-red-600">
                ❌ Impossible de charger l'image. Vérifiez l'URL.
              </div>
            )}
          </div>

          {/* Allergènes + Périssable */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="allergens">Allergènes</Label>
              <Input
                id="allergens"
                placeholder="Ex: Gluten, Lactose"
                {...register('allergens')}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="isPerishable">Type de produit</Label>
              <select
                id="isPerishable"
                className="w-full h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm"
                {...register('isPerishable', {
                  setValueAs: (v) => v === 'true' || v === true,
                })}
              >
                <option value="true">Périssable (frais)</option>
                <option value="false">Non périssable</option>
              </select>
            </div>
          </div>

          <DialogFooter className="pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isLoading}
            >
              Annuler
            </Button>
            <Button
              type="submit"
              className="bg-green-600 hover:bg-green-700"
              disabled={isLoading}
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Création...
                </>
              ) : (
                'Créer le produit'
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}