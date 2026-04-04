import { computed, Injectable } from '@angular/core';
import { MenuItem } from '../models/navigation.models';
import { AuthService } from './auth.service';

const MENU_ITEMS: MenuItem[] = [
  { label: 'Inicio', path: '/app/dashboard', icon: 'home', roles: ['ADMIN', 'CLIENTE', 'MESERO', 'PRODUCCION', 'CAJERO'] },
  { label: 'Cliente', path: '/app/cliente', icon: 'person', roles: ['CLIENTE'] },
  { label: 'Mesero', path: '/app/mesero', icon: 'restaurant', roles: ['MESERO'] },
  { label: 'Produccin', path: '/app/produccion', icon: 'kitchen', roles: ['PRODUCCION'] },
  { label: 'Cajero', path: '/app/cajero', icon: 'payments', roles: ['CAJERO'] },
  { label: 'Administrador', path: '/app/admin', icon: 'shield', roles: ['ADMIN'] },
  { label: 'Perfil', path: '/app/profile', icon: 'badge', roles: ['ADMIN', 'CLIENTE', 'MESERO', 'PRODUCCION', 'CAJERO'] }
];

@Injectable({ providedIn: 'root' })
export class MenuService {
  readonly menuByRole = computed(() => {
    const currentRole = this.authService.currentUser()?.role;
    if (!currentRole) {
      return [];
    }

    return MENU_ITEMS.filter((item) => item.roles.includes(currentRole));
  });

  constructor(private readonly authService: AuthService) {}
}