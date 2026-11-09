import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { CheckCircle2 } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function PaymentSuccessPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    // Optionnel : On pourrait appeler le backend ici pour vérifier la session
    // Mais le Webhook fait déjà le travail de confirmation
  }, [searchParams]);

  return (
    <div className="max-w-md mx-auto mt-20 p-8 bg-white rounded-2xl shadow-sm border border-gray-100 text-center space-y-6">
      <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto">
        <CheckCircle2 className="w-10 h-10 text-green-600" />
      </div>
      <h1 className="text-2xl font-bold text-gray-900">Paiement Réussi !</h1>
      <p className="text-gray-600">
        Merci pour votre réservation. Votre paiement a bien été validé par Stripe. Un e-mail de confirmation vous a été envoyé.
      </p>
      <Button onClick={() => navigate('/reservations')} className="w-full bg-green-600 hover:bg-green-700 text-white">
        Voir mes réservations
      </Button>
    </div>
  );
}