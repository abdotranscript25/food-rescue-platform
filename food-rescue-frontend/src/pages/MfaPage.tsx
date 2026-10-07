import { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { toast } from 'sonner';
import { ShieldCheck, Loader2, ArrowRight, ArrowLeft } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

import { authApi } from '@/api/authApi';
import { useAuthStore } from '@/store/authStore';

export default function MfaPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const setAuth = useAuthStore((s) => s.setAuth);

  const state = location.state as { partialToken?: string; email?: string } | null;
  const partialToken = state?.partialToken;
  const email = state?.email;

  const [code, setCode] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    // Si on arrive ici sans partialToken, on redirige vers login
    if (!partialToken) {
      navigate('/login');
    }
  }, [partialToken, navigate]);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (code.length !== 6) {
      toast.error('Le code doit contenir 6 chiffres');
      return;
    }
    if (!partialToken) return;

    setIsLoading(true);
    try {
      const response = await authApi.mfaValidate({ partialToken, code });
      if (response.token) {
        setAuth({
          token: response.token,
          email: response.email,
          role: response.role,
          firstName: response.firstName,
          lastName: response.lastName,
        });
        toast.success('Connexion réussie ! 🌿');
        navigate('/dashboard');
      }
    } catch (error: any) {
      const message =
        error.response?.data?.message || 'Code MFA invalide. Réessayez.';
      toast.error(message);
      setCode('');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-green-50 via-white to-emerald-50 p-6">
      <Card className="w-full max-w-md shadow-xl border-0">
        <CardHeader className="text-center space-y-3">
          <div className="mx-auto bg-green-100 p-4 rounded-full w-fit">
            <ShieldCheck className="w-10 h-10 text-green-600" />
          </div>
          <CardTitle className="text-2xl">Vérification en 2 étapes</CardTitle>
          <CardDescription>
            Entrez le code à 6 chiffres de votre application Google Authenticator
            {email && (
              <>
                <br />
                <span className="text-xs text-gray-500">Compte : {email}</span>
              </>
            )}
          </CardDescription>
        </CardHeader>

        <CardContent>
          <form onSubmit={onSubmit} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="code" className="sr-only">Code MFA</Label>
              <Input
                id="code"
                type="text"
                inputMode="numeric"
                maxLength={6}
                placeholder="000000"
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                className="text-center text-3xl tracking-[0.5em] font-mono h-16"
                autoFocus
              />
            </div>

            <Button
              type="submit"
              className="w-full bg-green-600 hover:bg-green-700 text-white h-11"
              disabled={isLoading || code.length !== 6}
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Vérification...
                </>
              ) : (
                <>
                  Valider
                  <ArrowRight className="w-4 h-4 ml-2" />
                </>
              )}
            </Button>

            <Link
              to="/login"
              className="flex items-center justify-center gap-2 text-sm text-gray-500 hover:text-gray-700"
            >
              <ArrowLeft className="w-4 h-4" />
              Retour à la connexion
            </Link>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}