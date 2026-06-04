import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from '../models/domain.models';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const userRole = authService.currentUser()?.role;
  const allowedRoles = (route.data?.['roles'] ?? []) as Role[];

  if (!userRole) {
    return router.createUrlTree(['/auth/login']);
  }

  if (allowedRoles.length === 0 || allowedRoles.includes(userRole)) {
    return true;
  }

  sessionStorage.setItem('flash_message', 'No tienes permisos para acceder a esta sección');
  return router.createUrlTree([authService.getLandingRouteForRole(userRole)]);
};