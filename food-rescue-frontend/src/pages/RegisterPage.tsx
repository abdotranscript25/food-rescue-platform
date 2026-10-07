import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import {
  Leaf, Loader2, Mail, Lock, User, Phone, MapPin, ArrowRight,
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

import { authApi } from '@/api/authApi';
import { useAuthStore } from '@/store/authStore';
import type { Role } from '@/types/auth';

const registerSchema = z
  .object({
    firstName: z.string().min(2, 'Prénom requis'),
    lastName: z.string().min(2, 'Nom requis'),
    email: z.string().email('Email invalide'),
    password: z.string().min(6, 'Minimum 6 caractères'),
    confirmPassword: z.string(),
    phone: z.string().optional(),
    role: z.enum(['CONSUMER', 'MERCHANT']),
    address: z.string().optional(),
    city: z.string().optional(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Les mots de passe ne correspondent pas',
    path: ['confirmPassword'],
  });

type RegisterForm = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
      phone: '',
      role: 'CONSUMER',
      address: '',
      city: '',
    },
  });

  const selectedRole = watch('role');

  const onSubmit = async (data: RegisterForm) => {
    setIsLoading(true);
    try {
      const { confirmPassword, ...payload } = data;
      const response = await authApi.register({ ...payload, role: data.role as Role });

      if (response.token) {
        setAuth({
          token: response.token,
          email: response.email,
          role: response.role,
          firstName: response.firstName,
          lastName: response.lastName,
        });
        toast.success(`Compte créé ! Bienvenue ${response.firstName} 🌿`);
        navigate('/dashboard');
      }
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        'Erreur lors de la création du compte.';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex">
      {/* LEFT SIDE */}
      <div className="hidden lg:flex lg:w-2/5 relative bg-gradient-to-br from-green-600 via-green-700 to-emerald-800 text-white overflow-hidden">
        <div className="absolute -top-24 -left-24 w-96 h-96 bg-white/10 rounded-full blur-3xl" />
        <div className="relative z-10 flex flex-col justify-between p-12 w-full">
          <div className="flex items-center gap-3">
            <div className="bg-white/20 backdrop-blur-sm p-3 rounded-2xl">
              <Leaf className="w-8 h-8" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Food Rescue</h1>
              <p className="text-sm text-green-100">Plateforme anti-gaspillage</p>
            </div>
          </div>

          <div className="space-y-6">
            <h2 className="text-3xl lg:text-4xl font-bold leading-tight">
              Créez votre compte en 30 secondes
            </h2>
            <p className="text-green-100">
              Que vous soyez commerçant ou consommateur, rejoignez notre
              communauté et faites la différence.
            </p>
          </div>

          <p className="text-sm text-green-100">
            © 2026 Food Rescue Platform
          </p>
        </div>
      </div>

      {/* RIGHT SIDE */}
      <div className="flex-1 flex items-center justify-center p-6 bg-gray-50 overflow-y-auto">
        <Card className="w-full max-w-lg shadow-lg border-0 my-8">
          <CardHeader>
            <CardTitle className="text-2xl">Créer un compte</CardTitle>
            <CardDescription>
              Rejoignez la communauté Food Rescue
            </CardDescription>
          </CardHeader>

          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              {/* Choix du rôle */}
              <div className="space-y-2">
                <Label>Je suis :</Label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setValue('role', 'CONSUMER')}
                    className={`p-3 rounded-lg border-2 transition-all text-left ${
                      selectedRole === 'CONSUMER'
                        ? 'border-green-600 bg-green-50'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <p className="font-medium text-sm">🛒 Consommateur</p>
                    <p className="text-xs text-gray-500">Je réserve des offres</p>
                  </button>
                  <button
                    type="button"
                    onClick={() => setValue('role', 'MERCHANT')}
                    className={`p-3 rounded-lg border-2 transition-all text-left ${
                      selectedRole === 'MERCHANT'
                        ? 'border-green-600 bg-green-50'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <p className="font-medium text-sm">🏪 Commerçant</p>
                    <p className="text-xs text-gray-500">Je publie des offres</p>
                  </button>
                </div>
              </div>

              {/* Prénom + Nom */}
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label htmlFor="firstName">Prénom</Label>
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <Input id="firstName" placeholder="Ahmed" className="pl-10" {...register('firstName')} />
                  </div>
                  {errors.firstName && <p className="text-xs text-red-500">{errors.firstName.message}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="lastName">Nom</Label>
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <Input id="lastName" placeholder="Benali" className="pl-10" {...register('lastName')} />
                  </div>
                  {errors.lastName && <p className="text-xs text-red-500">{errors.lastName.message}</p>}
                </div>
              </div>

              {/* Email */}
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <Input id="email" type="email" placeholder="vous@exemple.com" className="pl-10" {...register('email')} />
                </div>
                {errors.email && <p className="text-xs text-red-500">{errors.email.message}</p>}
              </div>

              {/* Téléphone */}
              <div className="space-y-2">
                <Label htmlFor="phone">Téléphone (optionnel)</Label>
                <div className="relative">
                  <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <Input id="phone" placeholder="+212 6XX XXX XXX" className="pl-10" {...register('phone')} />
                </div>
              </div>

              {/* Mot de passe */}
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label htmlFor="password">Mot de passe</Label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <Input id="password" type="password" placeholder="••••••" className="pl-10" {...register('password')} />
                  </div>
                  {errors.password && <p className="text-xs text-red-500">{errors.password.message}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="confirmPassword">Confirmer</Label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <Input id="confirmPassword" type="password" placeholder="••••••" className="pl-10" {...register('confirmPassword')} />
                  </div>
                  {errors.confirmPassword && <p className="text-xs text-red-500">{errors.confirmPassword.message}</p>}
                </div>
              </div>

              {/* Adresse + Ville */}
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label htmlFor="address">Adresse (optionnel)</Label>
                  <div className="relative">
                    <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <Input id="address" placeholder="Rue..." className="pl-10" {...register('address')} />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="city">Ville (optionnel)</Label>
                  <Input id="city" placeholder="Casablanca" {...register('city')} />
                </div>
              </div>

              <Button
                type="submit"
                className="w-full bg-green-600 hover:bg-green-700 text-white h-11"
                disabled={isLoading}
              >
                {isLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    Création en cours...
                  </>
                ) : (
                  <>
                    Créer mon compte
                    <ArrowRight className="w-4 h-4 ml-2" />
                  </>
                )}
              </Button>
            </form>

            <p className="text-center text-sm text-gray-600 mt-6">
              Déjà un compte ?{' '}
              <Link to="/login" className="text-green-600 hover:text-green-700 font-medium hover:underline">
                Se connecter
              </Link>
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}