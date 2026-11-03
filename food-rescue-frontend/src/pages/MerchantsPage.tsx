import React, { useEffect, useState } from 'react';
import { merchantApi } from '@/api/merchantApi';
import type { UserProfile } from '@/types/user';
import type { Offer } from '@/types/offer';
import type { Review } from '@/types/review'; // Ajustez le chemin vers votre type Review si nécessaire

export const MerchantsPage: React.FC = () => {
  const [merchants, setMerchants] = useState<UserProfile[]>([]);
  const [filteredMerchants, setFilteredMerchants] = useState<UserProfile[]>([]);
  const [selectedMerchant, setSelectedMerchant] = useState<UserProfile | null>(null);

  const [offers, setOffers] = useState<Offer[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [averageRating, setAverageRating] = useState<number>(0);

  const [searchTerm, setSearchTerm] = useState('');
  const [cityFilter, setCityFilter] = useState('');
  const [loading, setLoading] = useState(false);

  // 1. Charger tous les profils au montage du composant
  useEffect(() => {
    const fetchMerchants = async () => {
      try {
        setLoading(true);
        const data = await merchantApi.getAllProfiles();
        setMerchants(data);
        setFilteredMerchants(data);
      } catch (error) {
        console.error("Erreur lors du chargement des marchands", error);
      } finally {
        setLoading(false);
      }
    };

    fetchMerchants();
  }, []);

  // 2. Gérer la recherche et le filtrage en temps réel
  useEffect(() => {
    let result = merchants;
    if (searchTerm) {
      result = result.filter(m =>
        m.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        m.lastName.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
    if (cityFilter) {
      result = result.filter(m =>
        m.city?.toLowerCase().includes(cityFilter.toLowerCase())
      );
    }
    setFilteredMerchants(result);
  }, [searchTerm, cityFilter, merchants]);

  // 3. Charger les détails d'un marchand (offres, avis, note moyenne)
  const handleSelectMerchant = async (merchant: UserProfile) => {
    setSelectedMerchant(merchant);
    try {
      const [merchantOffers, merchantReviews, merchantRating] = await Promise.all([
        merchantApi.getOffersByMerchant(merchant.id),
        merchantApi.getReviewsByMerchant(merchant.id),
        merchantApi.getAverageRating(merchant.id)
      ]);
      setOffers(merchantOffers);
      setReviews(merchantReviews);
      setAverageRating(merchantRating);
    } catch (error) {
      console.error("Erreur lors du chargement des données du marchand", error);
    }
  };

  return (
    <div className="container mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">Découverte des Marchands &amp; Paniers Solidaires</h1>

      {!selectedMerchant ? (
        <div>
          {/* Barre de recherche et filtres */}
          <div className="flex gap-4 mb-6">
            <input
              type="text"
              placeholder="Rechercher par nom..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="border p-2 rounded w-1/3"
            />
            <input
              type="text"
              placeholder="Filtrer par ville..."
              value={cityFilter}
              onChange={(e) => setCityFilter(e.target.value)}
              className="border p-2 rounded w-1/3"
            />
          </div>

          {loading ? (
            <p>Chargement des marchands...</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {filteredMerchants.map((merchant) => (
                <div
                  key={merchant.id}
                  onClick={() => handleSelectMerchant(merchant)}
                  className="border p-4 rounded-lg shadow hover:shadow-lg cursor-pointer transition bg-white"
                >
                  <h3 className="text-xl font-semibold">{merchant.firstName} {merchant.lastName}</h3>
                  <p className="text-gray-600">📍 {merchant.city || 'Ville non spécifiée'}</p>
                  <p className="text-gray-500 text-sm mt-2">{merchant.address}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : (
        <div>
          {/* Bouton retour */}
          <button
            onClick={() => setSelectedMerchant(null)}
            className="mb-6 bg-gray-200 px-4 py-2 rounded hover:bg-gray-300 transition"
          >
            ← Retour à la liste des marchands
          </button>

          {/* En-tête du Marchand sélectionné */}
          <div className="bg-white p-6 rounded-lg shadow mb-6 border">
            <h2 className="text-2xl font-bold">{selectedMerchant.firstName} {selectedMerchant.lastName}</h2>
            <p className="text-gray-600">📍 {selectedMerchant.address}, {selectedMerchant.city}</p>
            <p className="text-gray-600">📞 {selectedMerchant.phone || 'Non renseigné'}</p>
            <div className="mt-3 flex items-center gap-2">
              <span className="text-yellow-500 font-bold text-lg">★ {averageRating.toFixed(1)} / 5</span>
              <span className="text-gray-500 text-sm">({reviews.length} avis)</span>
            </div>
          </div>

          {/* Catalogue des offres du marchand */}
          <h3 className="text-xl font-semibold mb-4">Catalogue des offres disponibles</h3>
          {offers.length === 0 ? (
            <p className="text-gray-500 mb-6">Ce marchand n'a aucune offre active pour le moment.</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
              {offers.map((offer) => (
                <div key={offer.id} className="border p-4 rounded shadow bg-white">
                  {offer.imageUrl && (
                    <img src={offer.imageUrl} alt={offer.title || 'Offre'} className="w-full h-40 object-cover rounded mb-2" />
                  )}
                  <h4 className="font-bold text-lg">{offer.title}</h4>
                  <p className="text-gray-600 text-sm mb-2">{offer.description}</p>
                  <div className="flex justify-between items-center">
                    <div>
                      <span className="line-through text-gray-400 text-sm mr-2">{offer.originalPrice} MAD</span>
                      <span className="text-green-600 font-bold">{offer.discountedPrice} MAD</span>
                    </div>
                    <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
                      Restant : {offer.remainingQuantity}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Section des Avis & Commentaires */}
          <h3 className="text-xl font-semibold mb-4">Avis des clients</h3>
          {reviews.length === 0 ? (
            <p className="text-gray-500">Aucun avis pour l'instant.</p>
          ) : (
            <div className="space-y-3">
              {reviews.map((review) => (
                <div key={review.id} className="border p-4 rounded bg-white shadow-sm">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-yellow-500 font-bold">{"★".repeat(review.rating)}</span>
                    <span className="text-gray-400 text-xs">({review.rating}/5)</span>
                  </div>
                  <p className="text-gray-700">{review.comment}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};