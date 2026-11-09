import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Mail, Shield, Lock, CheckCircle2, AlertTriangle, ArrowLeft, KeyRound, Edit3, Save } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { useAuthStore } from '@/store/authStore';
import axiosClient from '@/api/axiosClient';

export default function ProfilePage() {
  const navigate = useNavigate();
  const authState = useAuthStore();

  const [firstName, setFirstName] = useState(authState.firstName || '');
  const [lastName, setLastName] = useState(authState.lastName || '');
  const [email, setEmail] = useState(authState.email || '');

  const role = authState.role || 'CONSUMER';
  const isMfaEnabled = Boolean(authState.mfaEnabled);

  const [profileLoading, setProfileLoading] = useState(false);
  const [profileSuccess, setProfileSuccess] = useState('');
  const [profileError, setProfileError] = useState('');

  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [passwordError, setPasswordError] = useState('');

  // Synchroniser les champs si le store change
  useEffect(() => {
    setFirstName(authState.firstName || '');
    setLastName(authState.lastName || '');
    setEmail(authState.email || '');
  }, [authState.firstName, authState.lastName, authState.email]);

  // 💡 MISE À JOUR DU PROFIL (Nom/Prénom uniquement)
  const handleProfileUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileSuccess('');
    setProfileError('');

    try {
      setProfileLoading(true);

      // 1. Mise à jour dans le user-service (UNIQUEMENT prénom et nom)
      await axiosClient.put('/api/users/profiles/profile', {
        firstName,
        lastName
      });

      // 2. Synchronisation dans auth-service
      await axiosClient.put('/api/auth/update-profile', {
        firstName,
        lastName
      });

      // 3. Mise à jour du Store Zustand (Le persist mettra à jour le localStorage automatiquement)
      authState.setAuth({
        id: authState.id!,
        email: authState.email!,
        role: authState.role!,
        firstName,
        lastName,
        mfaEnabled: authState.mfaEnabled
      });

      setProfileSuccess('Vos informations personnelles ont été mises à jour avec succès.');
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Erreur lors de la mise à jour du profil.';
      setProfileError(msg);
    } finally {
      setProfileLoading(false);
    }
  };

  // GESTION DU CHANGEMENT DE MOT DE PASSE
  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordSuccess('');
    setPasswordError('');

    if (newPassword !== confirmPassword) {
      setPasswordError('Les nouveaux mots de passe ne correspondent pas.');
      return;
    }

    if (newPassword.length < 6) {
      setPasswordError('Le nouveau mot de passe doit contenir au moins 6 caractères.');
      return;
    }

    try {
      setPasswordLoading(true);
      await axiosClient.put('/api/auth/update-password', {
        oldPassword,
        newPassword
      });
      setPasswordSuccess('Votre mot de passe a été mis à jour avec succès.');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Erreur lors de la mise à jour du mot de passe.';
      setPasswordError(msg);
    } finally {
      setPasswordLoading(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto p-6 lg:p-8 space-y-8 pb-16">
      {/* Bouton Retour */}
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-2 text-gray-500 hover:text-gray-900 transition-colors font-medium text-sm group"
      >
        <ArrowLeft className="w-4 h-4 transition-transform group-hover:-translate-x-1" />
        <span>Retour</span>
      </button>

      {/* En-tête / Profil Hero */}
      <div className="relative overflow-hidden bg-gradient-to-r from-green-600 via-emerald-600 to-teal-700 text-white rounded-3xl p-8 shadow-xl flex flex-col sm:flex-row items-center gap-6">
        <div className="absolute -right-10 -bottom-10 w-48 h-48 bg-white/10 rounded-full blur-2xl pointer-events-none" />
        <div className="w-24 h-24 rounded-2xl bg-white/15 backdrop-blur-md flex items-center justify-center text-3xl font-extrabold tracking-wider uppercase border border-white/30 shadow-inner shrink-0">
          {firstName.charAt(0)}{lastName.charAt(0)}
        </div>
        <div className="space-y-2 text-center sm:text-left z-10">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/20 backdrop-blur-md text-xs font-semibold tracking-wide">
            <User className="w-3.5 h-3.5 text-yellow-300" />
            <span>Rôle : {role}</span>
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight">{firstName} {lastName}</h1>
          <p className="text-green-100 text-sm flex items-center justify-center sm:justify-start gap-2">
            <Mail className="w-4 h-4 opacity-80" /> {email}
          </p>
        </div>
      </div>

      {/* Formulaires en 2 Colonnes */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">

        {/* Colonne de gauche : Infos & Sécurité */}
        <div className="space-y-8">
          <Card className="border-0 shadow-md bg-white rounded-2xl overflow-hidden">
            <CardHeader className="bg-gray-50/50 border-b border-gray-100 pb-4">
              <CardTitle className="text-base font-bold flex items-center gap-2.5 text-gray-800">
                <div className="p-2 bg-green-50 text-green-600 rounded-xl">
                  <Edit3 className="w-4 h-4" />
                </div>
                <span>Informations Personnelles</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-6">
              <form onSubmit={handleProfileUpdate} className="space-y-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold uppercase tracking-wider text-gray-500">Prénom</label>
                  <input
                    type="text"
                    required
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    className="w-full text-sm bg-gray-50/50 border border-gray-200 rounded-xl p-3 text-gray-900 focus:bg-white focus:ring-2 focus:ring-green-500 focus:outline-none transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold uppercase tracking-wider text-gray-500">Nom</label>
                  <input
                    type="text"
                    required
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    className="w-full text-sm bg-gray-50/50 border border-gray-200 rounded-xl p-3 text-gray-900 focus:bg-white focus:ring-2 focus:ring-green-500 focus:outline-none transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold uppercase tracking-wider text-gray-500">Adresse E-mail</label>
                  <input
                    type="email"
                    readOnly
                    value={email}
                    className="w-full text-sm bg-gray-100 border border-gray-200 rounded-xl p-3 text-gray-500 cursor-not-allowed focus:outline-none"
                    title="Le changement d'email n'est pas disponible pour le moment."
                  />
                  <p className="text-[11px] text-gray-400">Le changement d'email n'est pas autorisé pour le moment.</p>
                </div>

                {profileError && (
                  <div className="p-3 bg-red-50 border border-red-200 text-red-600 text-xs rounded-xl font-medium flex items-center gap-2.5">
                    <AlertTriangle className="w-4 h-4 shrink-0" />
                    <span>{profileError}</span>
                  </div>
                )}

                {profileSuccess && (
                  <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs rounded-xl font-medium flex items-center gap-2.5">
                    <CheckCircle2 className="w-4 h-4 shrink-0" />
                    <span>{profileSuccess}</span>
                  </div>
                )}

                <Button
                  type="submit"
                  disabled={profileLoading}
                  className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold rounded-xl py-3 shadow-sm transition-all flex items-center justify-center gap-2 mt-2"
                >
                  <Save className="w-4 h-4" />
                  <span>{profileLoading ? "Enregistrement..." : "Enregistrer les modifications"}</span>
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md bg-white rounded-2xl overflow-hidden">
            <CardHeader className="bg-gray-50/50 border-b border-gray-100 pb-4">
              <CardTitle className="text-base font-bold flex items-center gap-2.5 text-gray-800">
                <div className="p-2 bg-emerald-50 text-emerald-600 rounded-xl">
                  <Shield className="w-4 h-4" />
                </div>
                <span>Sécurité du Compte & MFA</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-6 space-y-4">
              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-2xl border border-gray-100">
                <div>
                  <p className="text-sm font-semibold text-gray-900">Double Authentification</p>
                  <p className="text-xs text-gray-500">Protection renforcée par code OTP.</p>
                </div>
                <Badge className={isMfaEnabled ? "bg-emerald-100 text-emerald-800 border-0 px-3 py-1 text-xs" : "bg-amber-100 text-amber-800 border-0 px-3 py-1 text-xs"}>
                  {isMfaEnabled ? "Actif" : "Inactif"}
                </Badge>
              </div>

              <Button
                variant="outline"
                className="w-full justify-between border-gray-200 hover:bg-gray-50 rounded-xl py-3 h-auto"
                onClick={() => navigate('/security')}
              >
                <span className="text-sm font-medium text-gray-700">Configurer les paramètres MFA</span>
                <KeyRound className="w-4 h-4 text-gray-500" />
              </Button>
            </CardContent>
          </Card>
        </div>

        {/* Colonne de droite : Mot de passe */}
        <div>
          <Card className="border-0 shadow-md bg-white rounded-2xl overflow-hidden h-full">
            <CardHeader className="bg-gray-50/50 border-b border-gray-100 pb-4">
              <CardTitle className="text-base font-bold flex items-center gap-2.5 text-gray-800">
                <div className="p-2 bg-blue-50 text-blue-600 rounded-xl">
                  <Lock className="w-4 h-4" />
                </div>
                <span>Modifier le mot de passe</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-6">
              <form onSubmit={handlePasswordChange} className="space-y-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold uppercase tracking-wider text-gray-500">Ancien mot de passe</label>
                  <input
                    type="password"
                    required
                    value={oldPassword}
                    onChange={(e) => setOldPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full text-sm bg-gray-50/50 border border-gray-200 rounded-xl p-3 text-gray-900 focus:bg-white focus:ring-2 focus:ring-green-500 focus:outline-none transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold uppercase tracking-wider text-gray-500">Nouveau mot de passe</label>
                  <input
                    type="password"
                    required
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full text-sm bg-gray-50/50 border border-gray-200 rounded-xl p-3 text-gray-900 focus:bg-white focus:ring-2 focus:ring-green-500 focus:outline-none transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold uppercase tracking-wider text-gray-500">Confirmer le nouveau mot de passe</label>
                  <input
                    type="password"
                    required
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full text-sm bg-gray-50/50 border border-gray-200 rounded-xl p-3 text-gray-900 focus:bg-white focus:ring-2 focus:ring-green-500 focus:outline-none transition-all"
                  />
                </div>

                {passwordError && (
                  <div className="p-3 bg-red-50 border border-red-200 text-red-600 text-xs rounded-xl font-medium flex items-center gap-2.5">
                    <AlertTriangle className="w-4 h-4 shrink-0" />
                    <span>{passwordError}</span>
                  </div>
                )}

                {passwordSuccess && (
                  <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs rounded-xl font-medium flex items-center gap-2.5">
                    <CheckCircle2 className="w-4 h-4 shrink-0" />
                    <span>{passwordSuccess}</span>
                  </div>
                )}

                <Button
                  type="submit"
                  disabled={passwordLoading}
                  className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold rounded-xl py-3 shadow-sm transition-all flex items-center justify-center gap-2 mt-6"
                >
                  <Lock className="w-4 h-4" />
                  <span>{passwordLoading ? "Mise à jour..." : "Mettre à jour le mot de passe"}</span>
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>

      </div>
    </div>
  );
}