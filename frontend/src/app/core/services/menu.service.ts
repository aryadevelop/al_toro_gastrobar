import { computed, Injectable } from '@angular/core';
import { MenuItem } from '../models/navigation.models';
import { AuthService } from './auth.service';

const MENU_ITEMS: MenuItem[] = [
  { label: 'Inicio', path: '/app/dashboard', icon: 'home', roles: ['ADMIN', 'CLIENTE', 'MESERO', 'PRODUCCION', 'CAJERO'] },
  { label: 'Cliente', path: '/app/cliente', icon: 'person', roles: ['CLIENTE'] },
  { label: 'Historial', path: '/app/cliente/reservas/history', icon: 'history', roles: ['CLIENTE'] },
  { label: 'Mesero', path: '/app/mesero', icon: 'restaurant', roles: ['MESERO'] },
  { label: 'Producción', path: '/app/produccion', icon: 'kitchen', roles: ['PRODUCCION'] },
  { label: 'Mapa Mesas', path: '/app/cajero/mapa-mesas', icon: 'grid_view', roles: ['CAJERO'] },
  { label: 'Reservas', path: '/app/cajero/reservas', icon: 'book_online', roles: ['CAJERO'] },
  { label: 'Cierre Caja', path: '/app/cajero/cierre-caja', icon: 'point_of_sale', roles: ['CAJERO'] },
  { label: 'Administrador', path: '/app/admin', icon: 'shield', roles: ['ADMIN'] },
  { label: 'Historial visitas', path: '/app/admin/cliente-historial', icon: 'history', roles: ['ADMIN'] },
  { label: 'Mi perfil', path: '/app/profile', icon: 'badge', roles: ['ADMIN', 'CLIENTE', 'MESERO', 'PRODUCCION', 'CAJERO'] }
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
