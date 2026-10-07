import { Link } from 'react-router-dom';
import { ShoppingBag, CalendarCheck, Store, Package, TrendingUp } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/hooks/useAuth';

export default function DashboardPage() {
  const { firstName, role } = useAuth();

  return (
    <div className="space-y-6">
      {/* Bienvenue */}
      <div className="bg-gradient-to-br from-green-600 to-emerald-700 text-white rounded-2xl p-6 lg:p-8">
        <h1 className="text-2xl lg:text-3xl font-bold">
          Bonjour {firstName} 👋
        </h1>
        <p className="text-green-100 mt-2">
          {role === 'CONSUMER' && 'Découvrez les surplus alimentaires près de chez vous.'}
          {role === 'MERCHANT' && 'Gérez vos offres et vos produits.'}
          {role === 'ADMIN' && 'Administrez la plateforme.'}
        </p>
      </div>

      {/* Actions rapides */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <Card className="p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="bg-green-100 p-3 rounded-xl">
              <ShoppingBag className="w-6 h-6 text-green-600" />
            </div>
            <h3 className="font-semibold">Offres disponibles</h3>
          </div>
          <p className="text-sm text-gray-500 mb-4">
            Parcourez les offres actuellement disponibles.
          </p>
          <Button asChild className="w-full bg-green-600 hover:bg-green-700">
            <Link to="/offers">Voir les offres</Link>
          </Button>
        </Card>

        {role === 'CONSUMER' && (
          <Card className="p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="bg-blue-100 p-3 rounded-xl">
                <CalendarCheck className="w-6 h-6 text-blue-600" />
              </div>
              <h3 className="font-semibold">Mes réservations</h3>
            </div>
            <p className="text-sm text-gray-500 mb-4">
              Consultez l'historique de vos réservations.
            </p>
            <Button asChild variant="outline" className="w-full">
              <Link to="/consumer/reservations">Voir mes réservations</Link>
            </Button>
          </Card>
        )}

        {role === 'MERCHANT' && (
          <>
            <Card className="p-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="bg-orange-100 p-3 rounded-xl">
                  <Store className="w-6 h-6 text-orange-600" />
                </div>
                <h3 className="font-semibold">Mes offres</h3>
              </div>
              <p className="text-sm text-gray-500 mb-4">
                Créez et gérez vos offres commerciales.
              </p>
              <Button asChild variant="outline" className="w-full">
                <Link to="/my-offers">Gérer mes offres</Link>
              </Button>
            </Card>

            <Card className="p-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="bg-emerald-100 p-3 rounded-xl">
                  <Package className="w-6 h-6 text-emerald-600" />
                </div>
                <h3 className="font-semibold">Mes produits</h3>
              </div>
              <p className="text-sm text-gray-500 mb-4">
                Gérez votre catalogue de produits.
              </p>
              <Button asChild variant="outline" className="w-full">
                <Link to="/my-products">Gérer les produits</Link>
              </Button>
            </Card>
          </>
        )}

        <Card className="p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="bg-purple-100 p-3 rounded-xl">
              <TrendingUp className="w-6 h-6 text-purple-600" />
            </div>
            <h3 className="font-semibold">Impact écologique</h3>
          </div>
          <p className="text-sm text-gray-500 mb-4">
            Ensemble, sauvons les surplus alimentaires.
          </p>
          <div className="text-2xl font-bold text-green-600">🌱 0 kg sauvés</div>
        </Card>
      </div>
    </div>
  );
}