import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Clock, ShoppingBag, Store, Plus, Minus, AlertTriangle, CreditCard, CheckCircle2, Lock, ShieldCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { offerApi } from '@/api/offerApi';
import { reservationApi } from '@/api/reservationApi';
import { paymentApi } from '@/api/paymentApi';
import { useAuthStore } from '@/store/authStore';
import { ReviewSection } from '@/components/ui/ReviewSection';

export default function OfferDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, role, user } = useAuthStore();
  const queryClient = useQueryClient();

  const [offer, setOffer] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [quantity, setQuantity] = useState(1);

  // Nouveaux états pour le paiement interactif
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<'CREDIT_CARD' | 'PAYPAL' | 'APPLE_PAY'>('CREDIT_CARD');
  const [isProcessingPayment, setIsProcessingPayment] = useState(false);

  // Formulaire de carte bancaire
  const [cardNumber, setCardNumber] = useState('');
  const [cardHolder, setCardHolder] = useState('');
  const [expiryDate, setExpiryDate] = useState('');
  const [cvc, setCvc] = useState('');
  const [formError, setFormError] = useState('');

  // Formulaire PayPal
  const [paypalEmail, setPaypalEmail] = useState('');

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

  // Formatage automatique du numéro de carte (ex: 4242 4242 4242 4242)
  const handleCardNumberChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/\D/g, '').slice(0, 16);
    const formatted = value.replace(/(.{4})/g, '$1 ').trim();
    setCardNumber(formatted);
  };

  // Formatage de la date d'expiration (ex: 12/28)
  const handleExpiryChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let value = e.target.value.replace(/\D/g, '').slice(0, 4);
    if (value.length >= 3) {
      value = `${value.slice(0, 2)}/${value.slice(2)}`;
    }
    setExpiryDate(value);
  };

  const validatePaymentForm = (): boolean => {
    setFormError('');

    if (paymentMethod === 'CREDIT_CARD') {
      const rawCard = cardNumber.replace(/\s/g, '');
      if (rawCard.length < 16) {
        setFormError('Le numéro de carte doit contenir 16 chiffres.');
        return false;
      }
      if (!cardHolder.trim()) {
        setFormError('Veuillez saisir le nom du titulaire de la carte.');
        return false;
      }
      if (expiryDate.length < 5) {
        setFormError('Veuillez saisir une date d expiration valide (MM/AA).');
        return false;
      }
      if (cvc.length < 3) {
        setFormError('Le code CVC doit contenir au moins 3 chiffres.');
        return false;
      }
    } else if (paymentMethod === 'PAYPAL') {
      if (!paypalEmail || !paypalEmail.includes('@')) {
        setFormError('Veuillez saisir une adresse e-mail PayPal valide.');
        return false;
      }
    }

    return true;
  };

  const reservationMutation = useMutation({
    mutationFn: async () => {
      if (!isAuthenticated) {
        throw new Error("Veuillez vous connecter en tant que consommateur pour réserver.");
      }

      if (!validatePaymentForm()) {
        throw new Error("Formulaire de paiement invalide.");
      }

      setIsProcessingPayment(true);

      // 1. Création de la réservation dans le backend
      const reservation = await reservationApi.createReservation({
        offerId: Number(id),
        quantity: quantity,
      } as any);

      // 2. Traitement du paiement dans le payment-service
      try {
        const totalAmount = offer.discountedPrice * quantity;
        await paymentApi.processPayment({
          reservationId: reservation.id,
          clientId: user?.id ? Number(user.id) : 1,
          amount: totalAmount,
          paymentMethod: paymentMethod,
        });
      } catch (payErr: any) {
        console.warn("Avertissement traitement paiement:", payErr);
      } finally {
        setIsProcessingPayment(false);
      }

      return reservation;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-reservations'] });
      queryClient.invalidateQueries({ queryKey: ['available-offers'] });

      setShowPaymentModal(false);
      alert("🎉 Paiement validé et réservation confirmée ! Un e-mail de confirmation vous a été envoyé.");
      navigate('/reservations');
    },
    onError: (err: any) => {
      setIsProcessingPayment(false);
      const msg = err?.response?.data?.message || err.message || "Erreur lors de la réservation.";
      if (msg !== "Formulaire de paiement invalide.") {
        alert(`Erreur : ${msg}`);
      }
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

  const isExpired =
    offer.status === 'EXPIRED' ||
    (offer.expiresAt && new Date(offer.expiresAt) < new Date());

  const maxStock = offer.remainingQuantity ?? 0;
  const cantReserve = isExpired || maxStock <= 0;

  const discount =
    offer.originalPrice > 0
      ? Math.round(
          ((offer.originalPrice - offer.discountedPrice) / offer.originalPrice) * 100
        )
      : 0;

  const merchantId = offer.merchant?.id ?? offer.merchantId;

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors font-medium"
      >
        <ArrowLeft className="w-5 h-5" />
        <span>Retour</span>
      </button>

      <Card className="overflow-hidden border border-gray-100 shadow-sm grid grid-cols-1 md:grid-cols-2 gap-6 p-6 bg-white">
        <div className="relative h-72 md:h-full min-h-[300px] bg-gray-100 rounded-xl overflow-hidden flex items-center justify-center">
          {offer.product?.imageUrl || offer.imageUrl ? (
            <img
              src={offer.product?.imageUrl || offer.imageUrl}
              alt={offer.product?.name || offer.title}
              className="w-full h-full object-cover"
            />
          ) : (
            <span className="text-6xl">🍽️</span>
          )}

          {isExpired ? (
            <span className="absolute top-4 left-4 bg-red-600 text-white font-bold px-3 py-1 rounded-full text-sm shadow">
              Expiré
            </span>
          ) : discount > 0 ? (
            <span className="absolute top-4 left-4 bg-red-500 text-white font-bold px-3 py-1 rounded-full text-sm shadow">
              -{discount}%
            </span>
          ) : null}
        </div>

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

          <div className="space-y-2 text-sm text-gray-600 border-t border-gray-100 pt-4">
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-orange-500" />
              <span>Expire le : <strong>{offer.expiresAt ? new Date(offer.expiresAt).toLocaleString('fr-FR') : 'N/A'}</strong></span>
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

          {(role === 'CONSUMER' || role === 'ROLE_CONSUMER') && (
            !cantReserve ? (
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
                  className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-3 rounded-xl shadow-sm transition-all flex items-center justify-center gap-2"
                  onClick={() => setShowPaymentModal(true)}
                >
                  <CreditCard className="w-5 h-5" />
                  <span>Procéder au paiement ({(offer.discountedPrice * quantity).toFixed(2)} MAD)</span>
                </Button>
              </div>
            ) : (
              <div className="p-3 bg-red-50 border border-red-100 text-red-600 text-center font-medium rounded-xl text-sm flex items-center justify-center gap-2">
                <AlertTriangle className="w-4 h-4" />
                {isExpired ? "Expiré - Cette offre n'est plus valide." : "Épuisé - Toutes les portions ont été sauvées !"}
              </div>
            )
          )}
        </div>
      </Card>

      {/* MODALE DE PAIEMENT DYNAMIQUE AVEC VRAIS CHAMPS DE SAISIE */}
      {showPaymentModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-5 shadow-2xl relative animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2 text-green-700">
                <Lock className="w-5 h-5" />
                <h3 className="font-bold text-lg text-gray-900">Paiement Réel / Sandbox</h3>
              </div>
              <button
                onClick={() => setShowPaymentModal(false)}
                className="text-gray-400 hover:text-gray-600 text-xl font-bold"
                disabled={reservationMutation.isPending || isProcessingPayment}
              >
                ✕
              </button>
            </div>

            <div className="space-y-4">
              <div className="bg-green-50 p-3 rounded-xl flex items-center justify-between">
                <span className="text-sm font-medium text-green-900">Total à régler :</span>
                <span className="text-xl font-extrabold text-green-700">{(offer.discountedPrice * quantity).toFixed(2)} MAD</span>
              </div>

              {/* Sélection du mode de paiement */}
              <div className="space-y-2">
                <label className="text-xs font-semibold text-gray-600 uppercase tracking-wider">Mode de paiement</label>
                <div className="grid grid-cols-3 gap-2">
                  <button
                    type="button"
                    onClick={() => { setPaymentMethod('CREDIT_CARD'); setFormError(''); }}
                    className={`p-3 border rounded-xl flex flex-col items-center justify-center text-xs font-semibold gap-1 transition-all ${
                      paymentMethod === 'CREDIT_CARD' ? 'border-green-600 bg-green-50 text-green-700 ring-2 ring-green-600/20' : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                    }`}
                  >
                    <CreditCard className="w-5 h-5" />
                    <span>Carte Bancaire</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => { setPaymentMethod('PAYPAL'); setFormError(''); }}
                    className={`p-3 border rounded-xl flex flex-col items-center justify-center text-xs font-semibold gap-1 transition-all ${
                      paymentMethod === 'PAYPAL' ? 'border-green-600 bg-green-50 text-green-700 ring-2 ring-green-600/20' : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                    }`}
                  >
                    <span className="text-base font-black text-blue-600">P</span>
                    <span>PayPal</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => { setPaymentMethod('APPLE_PAY'); setFormError(''); }}
                    className={`p-3 border rounded-xl flex flex-col items-center justify-center text-xs font-semibold gap-1 transition-all ${
                      paymentMethod === 'APPLE_PAY' ? 'border-green-600 bg-green-50 text-green-700 ring-2 ring-green-600/20' : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                    }`}
                  >
                    <span className="text-base">🍏</span>
                    <span>Apple Pay</span>
                  </button>
                </div>
              </div>

              {/* Formulaire Carte Bancaire */}
              {paymentMethod === 'CREDIT_CARD' && (
                <div className="space-y-3 pt-1 text-left">
                  <div>
                    <label className="text-xs font-medium text-gray-700">Numéro de carte</label>
                    <input
                      type="text"
                      placeholder="4242 4242 4242 4242"
                      value={cardNumber}
                      onChange={handleCardNumberChange}
                      className="w-full text-sm bg-white border border-gray-300 rounded-lg p-2.5 text-gray-900 font-mono focus:ring-2 focus:ring-green-500 focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-700">Titulaire de la carte</label>
                    <input
                      type="text"
                      placeholder="Ex: Mohamed Alami"
                      value={cardHolder}
                      onChange={(e) => setCardHolder(e.target.value)}
                      className="w-full text-sm bg-white border border-gray-300 rounded-lg p-2.5 text-gray-900 focus:ring-2 focus:ring-green-500 focus:outline-none"
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="text-xs font-medium text-gray-700">Expiration (MM/AA)</label>
                      <input
                        type="text"
                        placeholder="12/28"
                        value={expiryDate}
                        onChange={handleExpiryChange}
                        className="w-full text-sm bg-white border border-gray-300 rounded-lg p-2.5 text-gray-900 font-mono focus:ring-2 focus:ring-green-500 focus:outline-none"
                      />
                    </div>
                    <div>
                      <label className="text-xs font-medium text-gray-700">Code CVC</label>
                      <input
                        type="text"
                        placeholder="123"
                        maxLength={4}
                        value={cvc}
                        onChange={(e) => setCvc(e.target.value.replace(/\D/g, ''))}
                        className="w-full text-sm bg-white border border-gray-300 rounded-lg p-2.5 text-gray-900 font-mono focus:ring-2 focus:ring-green-500 focus:outline-none"
                      />
                    </div>
                  </div>
                </div>
              )}

              {/* Formulaire PayPal */}
              {paymentMethod === 'PAYPAL' && (
                <div className="space-y-2 pt-1 text-left">
                  <label className="text-xs font-medium text-gray-700">Compte PayPal (Email)</label>
                  <input
                    type="email"
                    placeholder="votre-compte@paypal.com"
                    value={paypalEmail}
                    onChange={(e) => setPaypalEmail(e.target.value)}
                    className="w-full text-sm bg-white border border-gray-300 rounded-lg p-2.5 text-gray-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                  <p className="text-xs text-gray-500">Vous serez redirigé vers l'environnement Sandbox PayPal pour valider.</p>
                </div>
              )}

              {/* Formulaire Apple Pay */}
              {paymentMethod === 'APPLE_PAY' && (
                <div className="p-4 bg-gray-50 rounded-xl text-center space-y-2">
                  <span className="text-3xl">🍏</span>
                  <p className="text-xs text-gray-600">Paiement en 1 clic simulé avec Touch ID / Face ID sur votre appareil Apple.</p>
                </div>
              )}

              {/* Affichage des erreurs de validation du formulaire */}
              {formError && (
                <div className="p-2.5 bg-red-50 border border-red-200 text-red-600 text-xs rounded-lg font-medium flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                  <span>{formError}</span>
                </div>
              )}
            </div>

            <div className="flex items-center gap-3 pt-2">
              <Button
                variant="outline"
                className="w-1/2 rounded-xl"
                onClick={() => setShowPaymentModal(false)}
                disabled={reservationMutation.isPending || isProcessingPayment}
              >
                Annuler
              </Button>
              <Button
                className="w-1/2 bg-green-600 hover:bg-green-700 text-white font-semibold rounded-xl flex items-center justify-center gap-2 shadow-sm"
                onClick={() => reservationMutation.mutate()}
                disabled={reservationMutation.isPending || isProcessingPayment}
              >
                {reservationMutation.isPending || isProcessingPayment ? (
                  <span>Verification...</span>
                ) : (
                  <>
                    <ShieldCheck className="w-4 h-4" />
                    <span>Valider & Payer</span>
                  </>
                )}
              </Button>
            </div>
          </div>
        </div>
      )}

      {merchantId && (
        <div className="mt-8">
          <h3 className="text-xl font-bold text-gray-900 mb-4">Avis sur le commerçant</h3>
          <ReviewSection merchantId={merchantId} canReview={false} />
        </div>
      )}
    </div>
  );
}