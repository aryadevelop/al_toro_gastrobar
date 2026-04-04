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

  return router.createUrlTree(['/app/dashboard']);
};