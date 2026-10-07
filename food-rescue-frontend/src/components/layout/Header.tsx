import { Link, useNavigate } from 'react-router-dom';
import { Leaf, LogOut, User as UserIcon, ShieldCheck, Menu } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { useAuth } from '@/hooks/useAuth';

export default function Header() {
  const { firstName, lastName, email, role, logout } = useAuth();
  const navigate = useNavigate();

  const initials = `${firstName?.[0] ?? ''}${lastName?.[0] ?? ''}`.toUpperCase();

  return (
    <header className="sticky top-0 z-40 bg-white border-b border-gray-200">
      <div className="flex items-center justify-between h-16 px-4 lg:px-6">
        {/* Logo */}
        <Link to="/dashboard" className="flex items-center gap-2">
          <div className="bg-green-600 p-2 rounded-xl">
            <Leaf className="w-5 h-5 text-white" />
          </div>
          <span className="font-bold text-lg hidden sm:inline">Food Rescue</span>
        </Link>

        {/* Right side */}
        <div className="flex items-center gap-3">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="flex items-center gap-2 h-10 px-2">
                <Avatar className="w-8 h-8">
                  <AvatarFallback className="bg-green-100 text-green-700 text-xs font-medium">
                    {initials || 'U'}
                  </AvatarFallback>
                </Avatar>
                <div className="hidden md:flex flex-col items-start">
                  <span className="text-sm font-medium">
                    {firstName} {lastName}
                  </span>
                  <span className="text-xs text-gray-500">{role}</span>
                </div>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <div className="flex flex-col">
                  <span>{firstName} {lastName}</span>
                  <span className="text-xs text-gray-500 font-normal">{email}</span>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => navigate('/profile')}>
                <UserIcon className="w-4 h-4 mr-2" />
                Mon profil
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => navigate('/security')}>
                <ShieldCheck className="w-4 h-4 mr-2" />
                Sécurité (MFA)
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={logout} className="text-red-600">
                <LogOut className="w-4 h-4 mr-2" />
                Déconnexion
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </header>
  );
}