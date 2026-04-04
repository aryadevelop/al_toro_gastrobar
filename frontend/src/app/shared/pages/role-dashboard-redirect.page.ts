import { Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-role-dashboard-redirect-page',
  standalone: true,
  template: `<p>Redirigiendo...</p>`
})
export class RoleDashboardRedirectPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  constructor() {
    effect(() => {
      const userRole = this.authService.currentUser()?.role;
      if (userRole) {
        void this.router.navigateByUrl(this.authService.getLandingRouteForRole(userRole));
      } else {
        void this.router.navigateByUrl('/auth/login');
      }
    });
  }
}