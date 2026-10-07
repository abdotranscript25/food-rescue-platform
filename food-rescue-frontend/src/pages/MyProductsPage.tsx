import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Pencil, Trash2, Search, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { toast } from 'sonner';

// Helper pour récupérer le token JWT stocké dans Zustand (food-rescue-auth)
function getAuthToken(): string | null {
  const authStorage = localStorage.getItem('food-rescue-auth');
  if (!authStorage) return null;
  try {
    const parsed = JSON.parse(authStorage);
    return parsed?.state?.token ?? null;
  } catch (error) {
    console.error("Erreur lecture authentification :", error);
    return null;
  }
}

interface ProductResponse {
  id: any;
  name: string;
  description: string;
  category: string;
  imageUrl: string;
  allergens: string;
  isPerishable: boolean;
  merchantId: number;
  createdAt?: string;
}

interface ProductRequest {
  name: string;
  description: string;
  category: string;
  imageUrl: string;
  allergens: string;
  isPerishable: boolean;
  merchantId: number;
}

export default function MyProductsPage() {
  const queryClient = useQueryClient();
  const merchantId = 1;

  const [searchTerm, setSearchTerm] = useState('');
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<ProductResponse | null>(null);

  const [formData, setFormData] = useState<ProductRequest>({
    name: '',
    description: '',
    category: 'BOULANGERIE',
    imageUrl: '',
    allergens: '',
    isPerishable: true,
    merchantId: merchantId,
  });

  // Récupération des produits avec le token JWT
  const { data: products = [], isLoading, error } = useQuery<ProductResponse[]>({
    queryKey: ['products', merchantId],
    queryFn: async () => {
      const token = getAuthToken();
      const response = await fetch(`http://localhost:8080/api/products/merchant/${merchantId}`, {
        headers: {
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
      });
      if (!response.ok) {
        throw new Error("Erreur lors de la récupération des produits");
      }
      return response.json();
    },
  });

  // Mutation de création (POST) avec le token JWT
  const createMutation = useMutation({
    mutationFn: async (newProduct: ProductRequest) => {
      const token = getAuthToken();
      const response = await fetch('http://localhost:8080/api/products', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify(newProduct),
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Erreur ${response.status}: ${errorText || "Création échouée"}`);
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products', merchantId] });
      setIsDialogOpen(false);
      resetForm();
      toast.success("Produit créé avec succès.");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });

  // Mutation de mise à jour (PUT) avec le token JWT et nettoyage de l'ID
  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: any; data: ProductRequest }) => {
      const token = getAuthToken();
      const stringId = String(id);
      const cleanId = stringId.includes(':') ? stringId.split(':')[0] : stringId;

      const response = await fetch(`http://localhost:8080/api/products/${cleanId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify(data),
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Erreur ${response.status}: ${errorText || "Mise à jour échouée"}`);
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products', merchantId] });
      setIsDialogOpen(false);
      setEditingProduct(null);
      resetForm();
      toast.success("Produit mis à jour avec succès.");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });

  // Mutation de suppression (DELETE) avec le token JWT et nettoyage de l'ID
  const deleteMutation = useMutation({
    mutationFn: async (id: any) => {
      const token = getAuthToken();
      const stringId = String(id ?? '');
      const cleanId = stringId.includes(':') ? stringId.split(':')[0] : stringId;

      const response = await fetch(`http://localhost:8080/api/products/${cleanId}`, {
        method: 'DELETE',
        headers: {
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Erreur ${response.status}: ${errorText || "Suppression échouée"}`);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products', merchantId] });
      toast.success("Le produit a été supprimé avec succès.");
    },
    onError: (err: Error) => {
      toast.error(err.message);
    },
  });

  const resetForm = () => {
    setFormData({
      name: '',
      description: '',
      category: 'BOULANGERIE',
      imageUrl: '',
      allergens: '',
      isPerishable: true,
      merchantId: merchantId,
    });
    setEditingProduct(null);
  };

  const handleOpenCreate = () => {
    resetForm();
    setIsDialogOpen(true);
  };

  const handleOpenEdit = (product: ProductResponse) => {
    setEditingProduct(product);
    setFormData({
      name: product.name || '',
      description: product.description || '',
      category: product.category || 'BOULANGERIE',
      imageUrl: product.imageUrl || '',
      allergens: product.allergens || '',
      isPerishable: product.isPerishable ?? true,
      merchantId: product.merchantId || merchantId,
    });
    setIsDialogOpen(true);
  };

  const handleDelete = (id: any) => {
    if (!id) return;
    if (window.confirm("Voulez-vous vraiment supprimer ce produit de votre catalogue ?")) {
      deleteMutation.mutate(id);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editingProduct) {
      updateMutation.mutate({ id: editingProduct.id, data: formData });
    } else {
      createMutation.mutate(formData);
    }
  };

  const filteredProducts = products.filter((p) =>
    p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    p.category.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-gray-900">Mes produits du catalogue</h1>
          <p className="text-sm text-gray-500">
            Gérez votre catalogue de produits.
          </p>
        </div>
        <Button onClick={handleOpenCreate} className="bg-emerald-600 hover:bg-emerald-700 text-white gap-2">
          <Plus className="w-4 h-4" /> Ajouter un produit
        </Button>
      </div>

      <div className="relative max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4" />
        <Input
          placeholder="Rechercher par nom ou catégorie..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="pl-9"
        />
      </div>

      {isLoading && <div className="text-center py-12 text-gray-500">Chargement de vos produits...</div>}
      {error && (
        <div className="bg-red-50 border border-red-200 p-4 rounded-lg flex items-center gap-3 text-red-700">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>Impossible de charger les produits. Vérifiez que le microservice Backend est bien démarré.</span>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
        {filteredProducts.map((product) => (
          <div key={product.id} className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden flex flex-col hover:shadow-md transition-shadow">
            <div className="h-48 bg-gray-100 relative overflow-hidden">
              {product.imageUrl ? (
                <img
                  src={product.imageUrl}
                  alt={product.name}
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    (e.target as HTMLImageElement).src = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c';
                  }}
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center text-gray-400">Pas d'image</div>
              )}
              <Badge className="absolute top-3 left-3 bg-white/90 text-emerald-800 hover:bg-white shadow-sm font-semibold">
                {product.category}
              </Badge>
            </div>

            <div className="p-4 flex-1 flex flex-col justify-between space-y-3">
              <div>
                <h3 className="font-semibold text-gray-900 text-base line-clamp-1">{product.name}</h3>
                <p className="text-xs text-gray-500 mt-1 line-clamp-2">{product.description}</p>
              </div>

              <div className="flex items-center gap-2 pt-3 border-t border-gray-100">
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 text-gray-700 hover:bg-gray-50 gap-1"
                  onClick={() => handleOpenEdit(product)}
                >
                  <Pencil className="w-3.5 h-3.5" /> Modifier
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1 text-red-600 border-red-200 hover:bg-red-50 hover:text-red-700 gap-1"
                  onClick={() => handleDelete(product.id)}
                  disabled={deleteMutation.isPending}
                >
                  <Trash2 className="w-3.5 h-3.5" /> Supprimer
                </Button>
              </div>
            </div>
          </div>
        ))}
      </div>

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>{editingProduct ? "Modifier le produit" : "Ajouter un produit au catalogue"}</DialogTitle>
          </DialogHeader>

          <form onSubmit={handleSubmit} className="space-y-4 py-2">
            <div className="space-y-1.5">
              <Label htmlFor="name">Nom du produit</Label>
              <Input
                id="name"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="Ex: Panier Boulangerie Surprise"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="category">Catégorie</Label>
                <Select
                  value={formData.category}
                  onValueChange={(val) => setFormData({ ...formData, category: val })}
                >
                  <SelectTrigger id="category">
                    <SelectValue placeholder="Sélectionner" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="BOULANGERIE">Boulangerie</SelectItem>
                    <SelectItem value="RESTAURANT">Restaurant</SelectItem>
                    <SelectItem value="SUPERMARCHE">Supermarché</SelectItem>
                    <SelectItem value="PIZZA">Pizza</SelectItem>
                    <SelectItem value="PATISSERIE">Pâtisserie</SelectItem>
                    <SelectItem value="AUTRE">Autre</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="allergens">Allergènes</Label>
                <Input
                  id="allergens"
                  value={formData.allergens}
                  onChange={(e) => setFormData({ ...formData, allergens: e.target.value })}
                  placeholder="Ex: Gluten, Lait"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="description">Description</Label>
              <textarea
                id="description"
                rows={3}
                className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="Détails sur le produit..."
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="imageUrl">URL de l'image</Label>
              <Input
                id="imageUrl"
                value={formData.imageUrl}
                onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                placeholder="https://images.unsplash.com/..."
              />
            </div>

            <div className="flex items-center justify-between pt-2">
              <div className="space-y-0.5">
                <Label className="text-base" htmlFor="perishable-switch">Produit périssable</Label>
                <p className="text-xs text-gray-500">Ce produit a une date de durabilité courte</p>
              </div>
              <Switch
                id="perishable-switch"
                checked={formData.isPerishable}
                onCheckedChange={(checked) => setFormData({ ...formData, isPerishable: checked })}
              />
            </div>

            <DialogFooter className="pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsDialogOpen(false)}
              >
                Annuler
              </Button>
              <Button
                type="submit"
                className="bg-emerald-600 hover:bg-emerald-700 text-white"
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                {editingProduct ? "Mettre à jour" : "Créer le produit"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}