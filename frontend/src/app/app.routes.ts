import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { AppLayoutComponent } from './layouts/app-layout/app-layout.component';
import { AuthLayoutComponent } from './layouts/auth-layout/auth-layout.component';

export const appRoutes: Routes = [
  {
    path: 'auth',
    component: AuthLayoutComponent,
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES)
  },
  {
    path: 'app',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./shared/pages/role-dashboard-redirect.page').then((m) => m.RoleDashboardRedirectPage)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/auth/pages/profile-page/profile-page.component').then((m) => m.ProfilePageComponent)
      },
      {
        path: 'cliente',
        canActivate: [roleGuard],
        data: { roles: ['CLIENTE'] },
        loadChildren: () => import('./features/cliente/cliente.routes').then((m) => m.CLIENTE_ROUTES)
      },
      {
        path: 'mesero',
        canActivate: [roleGuard],
        data: { roles: ['MESERO'] },
        loadChildren: () => import('./features/mesero/mesero.routes').then((m) => m.MESERO_ROUTES)
      },
      {
        path: 'produccion',
        canActivate: [roleGuard],
        data: { roles: ['PRODUCCION'] },
        loadChildren: () => import('./features/produccion/produccion.routes').then((m) => m.PRODUCCION_ROUTES)
      },
      {
        path: 'cajero',
        canActivate: [roleGuard],
        data: { roles: ['CAJERO'] },
        loadChildren: () => import('./features/cajero/cajero.routes').then((m) => m.CAJERO_ROUTES)
      },
      {
        path: 'admin',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES)
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      }
    ]
  },
  {
    path: 'not-found',
    loadComponent: () => import('./shared/pages/not-found.page').then((m) => m.NotFoundPage)
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'app'
  },
  {
    path: '**',
    redirectTo: 'not-found'
  }
];