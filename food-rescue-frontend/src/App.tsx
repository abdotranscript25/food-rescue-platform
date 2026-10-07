import { Toaster } from '@/components/ui/sonner';
import AppRouter from '@/router/AppRouter';

export default function App() {
  return (
    <>
      <AppRouter />
      <Toaster position="top-right" richColors />
    </>
  );
}