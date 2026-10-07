import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { Leaf, Loader2, Mail, Lock, ArrowRight } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

import { authApi } from '@/api/authApi';
import { useAuthStore } from '@/store/authStore';

const loginSchema = z.object({
  email: z.string().email('Email invalide'),
  password: z.string().min(1, 'Mot de passe requis'),
});

type LoginForm = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [isLoading, setIsLoading] = useState(false);

  const successMessage = (location.state as { message?: string })?.message;

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = async (data: LoginForm) => {
    setIsLoading(true);
    try {
      const response = await authApi.login(data);

      // Cas MFA requis : redirection vers /mfa avec le partialToken et l'email
      if (response.mfaRequired && response.partialToken) {
        navigate('/mfa', {
          state: { partialToken: response.partialToken, email: response.email },
        });
        return;
      }

      // Cas normal : token JWT complet obtenu immédiatement
      if (response.token) {
        setAuth({
          token: response.token,
          email: response.email,
          role: response.role,
          firstName: response.firstName,
          lastName: response.lastName,
        });
        toast.success(`Bienvenue ${response.firstName} ! 🌿`);
        navigate('/dashboard');
      }
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        'Identifiants incorrects. Veuillez réessayer.';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex">
      {/* LEFT SIDE — Illustration / Branding */}
      <div className="hidden lg:flex lg:w-3/5 relative bg-gradient-to-br from-green-600 via-green-700 to-emerald-800 text-white overflow-hidden">
        {/* Cercles décoratifs */}
        <div className="absolute -top-24 -left-24 w-96 h-96 bg-white/10 rounded-full blur-3xl" />
        <div className="absolute -bottom-32 -right-32 w-[500px] h-[500px] bg-emerald-400/20 rounded-full blur-3xl" />

        <div className="relative z-10 flex flex-col justify-between p-12 w-full">
          {/* Logo */}
          <div className="flex items-center gap-3">
            <div className="bg-white/20 backdrop-blur-sm p-3 rounded-2xl">
              <Leaf className="w-8 h-8" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Food Rescue</h1>
              <p className="text-sm text-green-100">Plateforme anti-gaspillage</p>
            </div>
          </div>

          {/* Texte principal */}
          <div className="space-y-6 max-w-lg">
            <h2 className="text-4xl lg:text-5xl font-bold leading-tight">
              Sauvons ensemble les surplus alimentaires 🌱
            </h2>
            <p className="text-lg text-green-100">
              Rejoignez des milliers de commerçants et consommateurs qui luttent
              contre le gaspillage alimentaire au quotidien.
            </p>

            {/* Statistiques */}
            <div className="flex gap-8 pt-6">
              <div>
                <p className="text-3xl font-bold">10k+</p>
                <p className="text-sm text-green-100">Offres sauvées</p>
              </div>
              <div>
                <p className="text-3xl font-bold">500+</p>
                <p className="text-sm text-green-100">Commerçants</p>
              </div>
              <div>
                <p className="text-3xl font-bold">-40%</p>
                <p className="text-sm text-green-100">Gaspillage</p>
              </div>
            </div>
          </div>

          {/* Footer */}
          <p className="text-sm text-green-100">
            © 2026 Food Rescue Platform. Tous droits réservés.
          </p>
        </div>
      </div>

      {/* RIGHT SIDE — Formulaire */}
      <div className="flex-1 flex items-center justify-center p-6 bg-gray-50">
        <Card className="w-full max-w-md shadow-lg border-0">
          <CardHeader className="space-y-2">
            <div className="lg:hidden flex items-center gap-2 mb-2">
              <div className="bg-green-600 p-2 rounded-xl">
                <Leaf className="w-5 h-5 text-white" />
              </div>
              <span className="font-bold text-lg">Food Rescue</span>
            </div>
            <CardTitle className="text-2xl">Connexion</CardTitle>
            <CardDescription>
              Entrez vos identifiants pour accéder à votre espace
            </CardDescription>
          </CardHeader>

          <CardContent>
            {successMessage && (
              <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700">
                {successMessage}
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              {/* Email */}
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <Input
                    id="email"
                    type="email"
                    placeholder="vous@exemple.com"
                    className="pl-10"
                    {...register('email')}
                  />
                </div>
                {errors.email && (
                  <p className="text-sm text-red-500">{errors.email.message}</p>
                )}
              </div>

              {/* Mot de passe */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="password">Mot de passe</Label>
                  <Link
                    to="/forgot-password"
                    className="text-xs text-green-600 hover:text-green-700 hover:underline"
                  >
                    Mot de passe oublié ?
                  </Link>
                </div>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <Input
                    id="password"
                    type="password"
                    placeholder="••••••••"
                    className="pl-10"
                    {...register('password')}
                  />
                </div>
                {errors.password && (
                  <p className="text-sm text-red-500">{errors.password.message}</p>
                )}
              </div>

              {/* Bouton */}
              <Button
                type="submit"
                className="w-full bg-green-600 hover:bg-green-700 text-white h-11"
                disabled={isLoading}
              >
                {isLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    Connexion en cours...
                  </>
                ) : (
                  <>
                    Se connecter
                    <ArrowRight className="w-4 h-4 ml-2" />
                  </>
                )}
              </Button>
            </form>

            {/* Lien vers Register */}
            <p className="text-center text-sm text-gray-600 mt-6">
              Pas encore de compte ?{' '}
              <Link
                to="/register"
                className="text-green-600 hover:text-green-700 font-medium hover:underline"
              >
                Créer un compte
              </Link>
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}