import { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Shield, ShieldCheck, CheckCircle2, AlertTriangle, Lock } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { authApi } from '@/api/authApi';
import type { MfaSetupResponse } from '@/types/auth';

export default function MfaSecurityPage() {
  const [setupData, setSetupData] = useState<MfaSetupResponse | null>(null);
  const [otpCode, setOtpCode] = useState('');
  const [isActivating, setIsActivating] = useState(false);
  const [mfaEnabled, setMfaEnabled] = useState(false);
  const [isLoadingStatus, setIsLoadingStatus] = useState(true);

  const queryClient = useQueryClient();

  // 1. Récupérer le statut réel de l'utilisateur au chargement de la page
  useEffect(() => {
    const fetchUserStatus = async () => {
      try {
        // Remplacez selon votre api profile/auth (ex: authApi.getProfile ou authApi.getCurrentUser)
        const userProfile = await authApi.getCurrentUser?.() || await authApi.getProfile?.();
        if (userProfile && typeof userProfile.mfaEnabled === 'boolean') {
          setMfaEnabled(userProfile.mfaEnabled);
        }
      } catch (err) {
        console.error("Impossible de récupérer le statut MFA de l'utilisateur", err);
      } finally {
        setIsLoadingStatus(false);
      }
    };

    fetchUserStatus();
  }, []);

  // Mutation pour lancer le setup (générer le QR code)
  const setupMutation = useMutation({
    mutationFn: async () => {
      return await authApi.mfaSetup();
    },
    onSuccess: (data) => {
      setSetupData(data);
      setIsActivating(true);
    },
    onError: (err: any) => {
      alert(`Erreur lors de l'initialisation de la MFA : ${err?.response?.data?.message || err.message}`);
    },
  });

  // Mutation pour vérifier le code et finaliser l'activation
  const verifyMutation = useMutation({
    mutationFn: async (code: string) => {
      await authApi.mfaVerify({ code });
    },
    onSuccess: () => {
      setMfaEnabled(true);
      setIsActivating(false);
      setSetupData(null);
      setOtpCode('');
      alert("🎉 Authentification à deux facteurs (MFA) activée avec succès !");
      // Invalider les requêtes du cache si nécessaire
      queryClient.invalidateQueries({ queryKey: ['user'] });
    },
    onError: (err: any) => {
      alert(`Code invalide ou erreur : ${err?.response?.data?.message || err.message}`);
    },
  });

  if (isLoadingStatus) {
    return (
      <div className="max-w-3xl mx-auto p-6 text-center text-gray-500">
        Chargement des paramètres de sécurité...
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* En-tête de la page */}
      <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="bg-green-100 p-3 rounded-xl">
            <Shield className="w-8 h-8 text-green-600" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Sécurité et Double Facteur (MFA)</h1>
            <p className="text-sm text-gray-500">Sécurisez votre compte avec une application d'authentification (Google Authenticator, Authy).</p>
          </div>
        </div>
        <div>
          {mfaEnabled ? (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-green-50 text-green-700 border border-green-200">
              <ShieldCheck className="w-4 h-4" /> Activée
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200">
              <AlertTriangle className="w-4 h-4" /> Non activée
            </span>
          )}
        </div>
      </div>

      {/* Corps principal */}
      <Card className="p-6 space-y-6 bg-white border border-gray-100 shadow-sm">
        {mfaEnabled ? (
          <div className="space-y-4">
            <div className="flex items-center gap-2 text-green-700 font-semibold">
              <CheckCircle2 className="w-5 h-5" /> Votre compte est entièrement protégé
            </div>
            <p className="text-sm text-gray-600 leading-relaxed">
              La double authentification (MFA) est active sur votre compte. Un code unique à 6 chiffres vous sera demandé à chaque connexion pour valider votre identité.
            </p>
          </div>
        ) : !isActivating ? (
          <div className="space-y-4">
            <h3 className="font-semibold text-gray-900">Qu'est-ce que la MFA ?</h3>
            <p className="text-sm text-gray-600 leading-relaxed">
              L'authentification multifacteur ajoute une couche de sécurité supplémentaire à votre compte. Lors de la connexion, il vous sera demandé de saisir un code à 6 chiffres généré par votre application mobile en plus de votre mot de passe.
            </p>
            <Button
              onClick={() => setupMutation.mutate()}
              disabled={setupMutation.isPending}
              className="bg-green-600 hover:bg-green-700 text-white font-semibold flex items-center gap-2"
            >
              <Lock className="w-4 h-4" />
              {setupMutation.isPending ? "Génération en cours..." : "Configurer la MFA"}
            </Button>
          </div>
        ) : (
          <div className="space-y-6 border-t border-gray-100 pt-6">
            <div className="space-y-2">
              <h3 className="font-semibold text-gray-900">Étape 1 : Scannez le QR Code</h3>
              <p className="text-sm text-gray-500">
                Ouvrez votre application d'authentification (Google Authenticator, Microsoft Authenticator, etc.) et scannez l'image ci-dessous :
              </p>
            </div>

            {setupData?.qrCodeImage && (
              <div className="flex flex-col items-center justify-center p-4 bg-gray-50 rounded-xl border border-gray-200 w-fit mx-auto">
                <img
                  src={setupData.qrCodeImage}
                  alt="QR Code MFA"
                  className="w-48 h-48 object-contain"
                />
                <div className="mt-3 text-xs text-gray-400 text-center">
                  Secret manuel : <code className="bg-white px-2 py-1 rounded border text-gray-600 select-all">{setupData.secret}</code>
                </div>
              </div>
            )}

            <div className="space-y-3 pt-4 border-t border-gray-100">
              <h3 className="font-semibold text-gray-900">Étape 2 : Confirmez le code à 6 chiffres</h3>
              <p className="text-sm text-gray-500">
                Entrez le code affiché sur votre application pour valider l'activation :
              </p>

              <div className="flex gap-3 max-w-sm">
                <Input
                  type="text"
                  maxLength={6}
                  placeholder="123456"
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value)}
                  className="text-center tracking-widest text-lg font-bold"
                />
                <Button
                  onClick={() => verifyMutation.mutate(otpCode)}
                  disabled={verifyMutation.isPending || otpCode.length !== 6}
                  className="bg-green-600 hover:bg-green-700 text-white font-semibold"
                >
                  {verifyMutation.isPending ? "Vérification..." : "Activer"}
                </Button>
              </div>
            </div>

            <div className="pt-2">
              <Button
                variant="outline"
                onClick={() => {
                  setIsActivating(false);
                  setSetupData(null);
                  setOtpCode('');
                }}
              >
                Annuler
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}