import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/admin-dashboard-page/admin-dashboard-page.component').then((m) => m.AdminDashboardPageComponent)
  },
  {
    path: 'ventas',
    loadComponent: () => import('./pages/ventas-list-page/ventas-list-page.component').then((m) => m.VentasListPageComponent)
  },
  {
    path: 'ventas/:id',
    loadComponent: () => import('./pages/venta-detalle-page/venta-detalle-page.component').then((m) => m.VentaDetallePageComponent)
  },
  {
    path: 'cliente-historial',
    loadComponent: () => import('./pages/cliente-historial-page/cliente-historial-page.component').then((m) => m.ClienteHistorialPageComponent)
  },
  {
    path: 'preparaciones',
    loadComponent: () => import('./pages/preparaciones-list-page/preparaciones-list-page.component').then((m) => m.PreparacionesListPageComponent)
  },
  {
    path: 'preparaciones/new',
    loadComponent: () => import('./pages/preparacion-form-page/preparacion-form-page.component').then((m) => m.PreparacionFormPageComponent)
  },
  {
    path: 'productos',
    loadComponent: () => import('./pages/productos-list-page/productos-list-page.component').then((m) => m.ProductosListPageComponent)
  },
  {
    path: 'productos/new',
    loadComponent: () => import('./pages/producto-form-page/producto-form-page.component').then((m) => m.ProductoFormPageComponent)
  },
  {
    path: 'insumos',
    loadComponent: () => import('./pages/insumos-list-page/insumos-list-page.component').then((m) => m.InsumosListPageComponent)
  },
  {
    path: 'insumos/new',
    loadComponent: () => import('./pages/insumo-form-page/insumo-form-page.component').then((m) => m.InsumoFormPageComponent)
  },
  {
    path: 'decoraciones',
    loadComponent: () => import('./pages/decoraciones-page/decoraciones-page.component').then((m) => m.DecoracionesPageComponent)
  },
  {
    path: 'personal',
    loadComponent: () => import('./pages/personal-list-page/personal-list-page.component').then((m) => m.PersonalListPageComponent)
  },
  {
    path: 'personal/new',
    loadComponent: () => import('./pages/personal-form-page/personal-form-page.component').then((m) => m.PersonalFormPageComponent)
  },
  {
    path: 'clientes',
    loadComponent: () => import('./pages/clientes-list-page/clientes-list-page.component').then((m) => m.ClientesListPageComponent)
  },
  {
    path: 'clientes/:id',
    loadComponent: () => import('./pages/cliente-detalle-page/cliente-detalle-page.component').then((m) => m.ClienteDetallePageComponent)
  }
];