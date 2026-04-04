import { Routes } from '@angular/router';

export const PRODUCCION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/produccion-dashboard/produccion-dashboard.component').then((m) => m.ProduccionDashboardComponent)
  },
  {
    path: 'comandas-board',
    loadComponent: () => import('./pages/comandas-board-page/comandas-board-page.component').then((m) => m.ComandasBoardPageComponent)
  },
  {
    path: 'comanda/:id',
    loadComponent: () => import('./pages/comanda-detalle-page/comanda-detalle-page.component').then((m) => m.ComandaDetallePageComponent)
  },
  {
    path: 'inventario-egreso',
    loadComponent: () => import('./pages/inventario-egreso-page/inventario-egreso-page.component').then((m) => m.InventarioEgresoPageComponent)
  }
];