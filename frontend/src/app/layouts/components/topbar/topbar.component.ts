import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { PendingProfileChangesService } from '../../../core/services/pending-profile-changes.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <header class="topbar card">
      <div class="topbar__left">
        <img src="assets/images/al-toro-logo-vector.svg" alt="Al Toro Gastrobar" class="topbar-logo" />
        <div>
          <h3>Panel operativo</h3>
          <small>Bienvenido: {{ authService.currentUser()?.fullName }}</small>
        </div>
      </div>

      <button type="button" class="btn-secondary" (click)="onLogout()">Cerrar sesión</button>
    </header>
  `,
  styleUrls: ['./topbar.component.scss']
})
export class TopbarComponent {
  constructor(
    public readonly authService: AuthService,
    private readonly router: Router,
    private readonly pendingChangesService: PendingProfileChangesService
  ) {}

  onLogout(): void {
    if (this.router.url.startsWith('/app/profile') && this.pendingChangesService.hasUnsavedChanges()) {
      const shouldLogout = window.confirm('Tienes cambios sin guardar. ¿Estás seguro de que deseas salir?');
      if (!shouldLogout) {
        return;
      }
      this.pendingChangesService.skipNextPrompt();
      this.pendingChangesService.setHasUnsavedChanges(false);
    }

    this.authService.logout().subscribe({
      next: () => void this.router.navigateByUrl('/auth/login'),
      error: () => void this.router.navigateByUrl('/auth/login')
    });
  }
}