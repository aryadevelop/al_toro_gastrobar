import { Routes } from '@angular/router';

export const CAJERO_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/cajero-dashboard/cajero-dashboard.component').then((m) => m.CajeroDashboardComponent)
  },
  {
    path: 'reservas',
    loadComponent: () => import('./pages/reservas-cajero-page/reservas-cajero-page.component').then((m) => m.ReservasCajeroPageComponent)
  },
  {
    path: 'mapa-mesas',
    loadComponent: () => import('./pages/mapa-mesas-cajero-page/mapa-mesas-cajero-page.component').then((m) => m.MapaMesasCajeroPageComponent)
  },
  {
    path: 'pago-cierre',
    loadComponent: () => import('./pages/pago-cierre-page/pago-cierre-page.component').then((m) => m.PagoCierrePageComponent)
  },
  {
    path: 'cierre-caja',
    loadComponent: () => import('./pages/cierre-caja-page/cierre-caja-page.component').then((m) => m.CierreCajaPageComponent)
  }
];