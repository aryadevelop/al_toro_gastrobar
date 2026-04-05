import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

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
    private readonly router: Router
  ) {}

  onLogout(): void {
    this.authService.logout().subscribe({
      next: () => void this.router.navigateByUrl('/auth/login'),
      error: () => void this.router.navigateByUrl('/auth/login')
    });
  }
}