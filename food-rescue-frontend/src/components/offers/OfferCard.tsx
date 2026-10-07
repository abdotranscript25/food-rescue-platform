import { Link } from 'react-router-dom';
import { MapPin, Clock, TrendingDown } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import type { OfferWithDetails } from '@/types/offer';
import { cn } from '@/lib/utils';

interface OfferCardProps {
  offer: OfferWithDetails;
}

export default function OfferCard({ offer }: OfferCardProps) {
  const discount =
    offer.originalPrice > 0
      ? Math.round(
          ((offer.originalPrice - offer.discountedPrice) / offer.originalPrice) * 100
        )
      : 0;

  const expiresIn = getExpiresIn(offer.expiresAt);

  return (
    <Link to={`/offers/${offer.id}`}>
      <Card className="overflow-hidden hover:shadow-lg transition-all duration-200 hover:-translate-y-1 cursor-pointer h-full">
        {/* Image */}
        <div className="relative aspect-video bg-gray-100 overflow-hidden">
          {offer.product?.imageUrl ? (
            <img
              src={offer.product.imageUrl}
              alt={offer.product.name}
              className="w-full h-full object-cover"
              loading="lazy"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-300">
              <span className="text-4xl">🍽️</span>
            </div>
          )}

          {/* Badge réduction */}
          {discount > 0 && (
            <Badge className="absolute top-2 left-2 bg-red-500 hover:bg-red-500 text-white font-semibold">
              -{discount}%
            </Badge>
          )}

          {/* Badge stock */}
          {offer.remainingQuantity <= 3 && offer.remainingQuantity > 0 && (
            <Badge className="absolute top-2 right-2 bg-orange-500 hover:bg-orange-500 text-white">
              Plus que {offer.remainingQuantity}
            </Badge>
          )}
        </div>

        {/* Contenu */}
        <div className="p-4 space-y-3">
          <div>
            <p className="text-xs text-green-600 font-medium uppercase">
              {offer.product?.category ?? 'Sans catégorie'}
            </p>
            <h3 className="font-semibold text-gray-900 line-clamp-1">
              {offer.product?.name ?? offer.title ?? 'Offre sans titre'}
            </h3>
          </div>

          {/* Merchant */}
          {offer.merchant && (
            <p className="text-xs text-gray-500 flex items-center gap-1">
              <MapPin className="w-3 h-3" />
              {offer.merchant.firstName} {offer.merchant.lastName}
              {offer.merchant.city && ` · ${offer.merchant.city}`}
            </p>
          )}

          {/* Prix */}
          <div className="flex items-baseline gap-2">
            <span className="text-xl font-bold text-green-600">
              {offer.discountedPrice.toFixed(2)} MAD
            </span>
            <span className="text-sm text-gray-400 line-through">
              {offer.originalPrice.toFixed(2)} MAD
            </span>
          </div>

          {/* Expiration */}
          <div
            className={cn(
              'flex items-center gap-1 text-xs',
              expiresIn.urgent ? 'text-red-500 font-medium' : 'text-gray-500'
            )}
          >
            <Clock className="w-3 h-3" />
            {expiresIn.label}
          </div>
        </div>
      </Card>
    </Link>
  );
}

function getExpiresIn(expiresAt: string): { label: string; urgent: boolean } {
  const now = new Date().getTime();
  const exp = new Date(expiresAt).getTime();
  const diffMs = exp - now;
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffHours / 24);

  if (diffMs < 0) return { label: 'Expiré', urgent: true };
  if (diffHours < 24)
    return { label: `Expire dans ${diffHours}h`, urgent: true };
  if (diffDays < 7) return { label: `Expire dans ${diffDays}j`, urgent: false };
  return { label: `Expire le ${new Date(expiresAt).toLocaleDateString('fr-FR')}`, urgent: false };
}