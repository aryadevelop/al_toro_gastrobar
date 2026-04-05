import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login-page/login-page.component').then((m) => m.LoginPageComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register-page/register-page.component').then((m) => m.RegisterPageComponent)
  },
  {
    path: 'change-password',
    loadComponent: () => import('./pages/change-password-page/change-password-page.component').then((m) => m.ChangePasswordPageComponent)
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login'
  }
];