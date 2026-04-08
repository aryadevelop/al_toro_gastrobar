import { Routes } from '@angular/router';

export const CLIENTE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/cliente-dashboard/cliente-dashboard.component').then((m) => m.ClienteDashboardComponent)
  },
  {
    path: 'reserva/create',
    loadComponent: () => import('./pages/reserva-create-page/reserva-create-page.component').then((m) => m.ReservaCreatePageComponent)
  },
  {
    path: 'reserva/edit/:id',
    loadComponent: () => import('./pages/reserva-edit-page/reserva-edit-page.component').then((m) => m.ReservaEditPageComponent)
  },
  {
    path: 'reservas/history',
    loadComponent: () => import('./pages/reservas-history-page/reservas-history-page.component').then((m) => m.ReservasHistoryPageComponent)
  },
  {
    path: 'preorden',
    loadComponent: () => import('./pages/preorden-page/preorden-page.component').then((m) => m.PreordenPageComponent)
  },
  {
    path: 'orden-actual',
    loadComponent: () => import('./pages/orden-actual-page/orden-actual-page.component').then((m) => m.OrdenActualPageComponent)
  },
  {
    path: 'asistencia',
    loadComponent: () => import('./pages/asistencia-page/asistencia-page.component').then((m) => m.AsistenciaPageComponent)
  }
];