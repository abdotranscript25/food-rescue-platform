import { useState, useEffect } from 'react';
import { Star, MessageSquare, Send, CheckCircle2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { reviewApi } from '@/api/reviewApi';

interface ReviewSectionProps {
  merchantId: string | number;
  clientId?: number;
  canReview?: boolean;
}

export function ReviewSection({ merchantId, clientId, canReview = false }: ReviewSectionProps) {
  // Adaptation au format UUID (string) tout en gérant proprement les éventuelles chaînes vides
  const cleanMerchantId = String(merchantId || '').trim();

  const [reviews, setReviews] = useState<any[]>([]);
  const [averageRating, setAverageRating] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);

  // Formulaire d'avis
  const [rating, setRating] = useState<number>(5);
  const [comment, setComment] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const fetchReviewsData = async () => {
    if (!cleanMerchantId) return;
    try {
      setLoading(true);
      const reviewsData = await reviewApi.getReviewsByMerchant(cleanMerchantId);
      const avgData = await reviewApi.getAverageRating(cleanMerchantId);

      setReviews(Array.isArray(reviewsData) ? reviewsData : []);
      setAverageRating(typeof avgData === 'number' ? avgData : 0);
    } catch (err) {
      console.error("Erreur lors du chargement des avis", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (cleanMerchantId) {
      fetchReviewsData();
    }
  }, [cleanMerchantId]);

  const handleSubmitReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!clientId) {
      alert("Identifiant client introuvable.");
      return;
    }

    // 🔍 Extraction robuste du nom depuis Zustand / localStorage (clé 'food-rescue-auth')
    let clientName = "Client";
    try {
      const authStorage = localStorage.getItem('food-rescue-auth');
      if (authStorage) {
        const parsedAuth = JSON.parse(authStorage);
        const userState = parsedAuth?.state || parsedAuth;

        const firstName = userState.firstName || userState.prenom || '';
        const lastName = userState.lastName || userState.nom || '';

        if (firstName || lastName) {
          clientName = `${firstName} ${lastName}`.trim();
        } else if (userState.name || userState.fullName || userState.username) {
          clientName = userState.name || userState.fullName || userState.username;
        }
      }
    } catch (err) {
      console.error("Erreur lors de la lecture du localStorage pour le nom", err);
    }

    try {
      setSubmitting(true);
      await reviewApi.createReview({
        merchantId: cleanMerchantId,
        clientId: Number(clientId),
        rating,
        comment,
        clientName,
      });
      setSuccessMessage("Merci ! Votre avis a été publié avec succès.");
      setComment('');
      fetchReviewsData();
    } catch (err: any) {
      alert(err?.response?.data?.message || "Erreur lors de l'envoi de l'avis.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* En-tête : Note moyenne */}
      <Card className="p-5 bg-white border border-gray-100 shadow-sm flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-yellow-50 rounded-xl text-yellow-600">
            <Star className="w-6 h-6 fill-current" />
          </div>
          <div>
            <h4 className="text-lg font-bold text-gray-900">
              {averageRating ? averageRating.toFixed(1) : "0.0"} / 5
            </h4>
            <p className="text-xs text-gray-500">Basé sur {reviews.length} avis clients</p>
          </div>
        </div>
        <div className="flex items-center gap-1 text-yellow-400">
          {[1, 2, 3, 4, 5].map((star) => (
            <Star
              key={star}
              className={`w-5 h-5 ${
                star <= Math.round(averageRating) ? 'fill-current' : 'text-gray-200'
              }`}
            />
          ))}
        </div>
      </Card>

      {/* Formulaire d'ajout d'avis (si autorisé) */}
      {canReview && (
        <Card className="p-5 bg-white border border-gray-100 shadow-sm space-y-4">
          <h5 className="font-semibold text-gray-900 flex items-center gap-2">
            <MessageSquare className="w-4 h-4 text-green-600" />
            Laisser un avis sur votre expérience
          </h5>

          {successMessage ? (
            <div className="p-3 bg-green-50 border border-green-100 text-green-700 rounded-xl text-sm flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4" />
              {successMessage}
            </div>
          ) : (
            <form onSubmit={handleSubmitReview} className="space-y-4">
              <div className="space-y-1">
                <label className="text-xs font-medium text-gray-700">Note (de 1 à 5 étoiles)</label>
                <div className="flex items-center gap-2">
                  {[1, 2, 3, 4, 5].map((num) => (
                    <button
                      type="button"
                      key={num}
                      onClick={() => setRating(num)}
                      className="p-1 focus:outline-none transition-transform hover:scale-110"
                    >
                      <Star
                        className={`w-6 h-6 ${
                          num <= rating ? 'text-yellow-400 fill-current' : 'text-gray-300'
                        }`}
                      />
                    </button>
                  ))}
                  <span className="ml-2 text-sm font-semibold text-gray-700">{rating} / 5</span>
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-xs font-medium text-gray-700">Votre commentaire</label>
                <textarea
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Comment s'est passé le retrait de votre panier anti-gaspi ?"
                  className="w-full border border-gray-200 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-green-500 bg-gray-50"
                  rows={3}
                  required
                />
              </div>

              <Button
                type="submit"
                disabled={submitting}
                className="w-full bg-green-600 hover:bg-green-700 text-white font-medium py-2 rounded-xl text-sm flex items-center justify-center gap-2"
              >
                <Send className="w-4 h-4" />
                {submitting ? "Publication..." : "Publier mon avis"}
              </Button>
            </form>
          )}
        </Card>
      )}

      {/* Liste des avis existants */}
      <div className="space-y-3">
        <h5 className="font-semibold text-gray-900 text-sm">Avis récents</h5>
        {loading ? (
          <p className="text-xs text-gray-400">Chargement des avis...</p>
        ) : reviews.length === 0 ? (
          <p className="text-xs text-gray-400 italic">Aucun avis pour le moment sur ce commerçant.</p>
        ) : (
          reviews.map((rev, index) => (
            <Card key={rev.id || index} className="p-4 bg-white border border-gray-100 shadow-sm space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-xs text-gray-900">
                  {rev.clientName || "Client"}
                </span>
                <div className="flex items-center gap-1 text-yellow-400">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <Star
                      key={star}
                      className={`w-3.5 h-3.5 ${
                        star <= rev.rating ? 'fill-current' : 'text-gray-200'
                      }`}
                    />
                  ))}
                </div>
              </div>
              <p className="text-xs text-gray-600 leading-relaxed">{rev.comment}</p>
              <span className="text-[10px] text-gray-400 block">
                {rev.createdAt ? new Date(rev.createdAt).toLocaleDateString('fr-FR') : ''}
              </span>
            </Card>
          ))
        )}
      </div>
    </div>
  );
}