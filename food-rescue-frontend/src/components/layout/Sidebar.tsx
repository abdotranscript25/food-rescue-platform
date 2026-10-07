import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  ShoppingBag,
  CalendarCheck,
  Store,
  Package,
  Users,
  ShieldCheck,
  Settings,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';
import type { Role } from '@/types/auth';

interface MenuItem {
  to: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  roles: Role[];
}

const MENU_ITEMS: MenuItem[] = [
  {
    to: '/dashboard',
    label: 'Tableau de bord',
    icon: LayoutDashboard,
    roles: ['CONSUMER', 'MERCHANT', 'ADMIN'],
  },
  {
    to: '/offers',
    label: 'Offres disponibles',
    icon: ShoppingBag,
    roles: ['CONSUMER', 'MERCHANT', 'ADMIN'],
  },
  {
    to: '/reservations',
    label: 'Mes réservations',
    icon: CalendarCheck,
    roles: ['CONSUMER'],
  },
  {
    to: '/my-offers',
    label: 'Mes offres',
    icon: Store,
    roles: ['MERCHANT', 'ADMIN'],
  },
  {
    to: '/my-products',
    label: 'Mes produits',
    icon: Package,
    roles: ['MERCHANT'],
  },
  {
    to: '/admin/users',
    label: 'Utilisateurs',
    icon: Users,
    roles: ['ADMIN'],
  },
  {
    to: '/security',
    label: 'Sécurité (MFA)',
    icon: ShieldCheck,
    roles: ['CONSUMER', 'MERCHANT', 'ADMIN'],
  },
  {
    to: '/profile',
    label: 'Mon profil',
    icon: Settings,
    roles: ['CONSUMER', 'MERCHANT', 'ADMIN'],
  },
];

export default function Sidebar() {
  const role = useAuthStore((s) => s.role);

  if (!role) return null;

  const visibleItems = MENU_ITEMS.filter((item) => item.roles.includes(role));

  return (
    <aside className="w-64 min-h-[calc(100vh-4rem)] bg-white border-r border-gray-200 hidden md:block">
      <nav className="p-4 space-y-1">
        {visibleItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-green-50 text-green-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                )
              }
            >
              <Icon className="w-4 h-4 shrink-0" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>
    </aside>
  );
}