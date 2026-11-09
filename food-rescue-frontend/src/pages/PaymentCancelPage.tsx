import { useNavigate } from 'react-router-dom';
import { XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function PaymentCancelPage() {
  const navigate = useNavigate();

  return (
    <div className="max-w-md mx-auto mt-20 p-8 bg-white rounded-2xl shadow-sm border border-gray-100 text-center space-y-6">
      <div className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto">
        <XCircle className="w-10 h-10 text-red-600" />
      </div>
      <h1 className="text-2xl font-bold text-gray-900">Paiement Annulé</h1>
      <p className="text-gray-600">
        Vous avez annulé le processus de paiement. Aucun montant n'a été débité.
      </p>
      <Button onClick={() => navigate('/offers')} variant="outline" className="w-full">
        Retour aux offres
      </Button>
    </div>
  );
}