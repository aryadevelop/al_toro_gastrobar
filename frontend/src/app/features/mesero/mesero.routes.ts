import { Routes } from '@angular/router';

export const MESERO_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/mesero-dashboard/mesero-dashboard.component').then((m) => m.MeseroDashboardComponent)
  },
  {
    path: 'reservas',
    loadComponent: () => import('./pages/reservas-list-page/reservas-list-page.component').then((m) => m.ReservasListPageComponent)
  },
  {
    path: 'llegada-reserva',
    loadComponent: () => import('./pages/llegada-reserva-page/llegada-reserva-page.component').then((m) => m.LlegadaReservaPageComponent)
  },
  {
    path: 'mapa-mesas',
    loadComponent: () => import('./pages/mapa-mesas-page/mapa-mesas-page.component').then((m) => m.MapaMesasPageComponent)
  },
  {
    path: 'comanda-editor',
    loadComponent: () => import('./pages/comanda-editor-page/comanda-editor-page.component').then((m) => m.ComandaEditorPageComponent)
  },
  {
    path: 'notificaciones',
    loadComponent: () => import('./pages/notificaciones-page/notificaciones-page.component').then((m) => m.NotificacionesPageComponent)
  }
];