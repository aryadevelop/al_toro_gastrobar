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
      <div>
        <h3>Panel operativo</h3>
        <small>Bienvenido: {{ authService.currentUser()?.fullName }}</small>
      </div>

      <button type="button" class="btn-secondary" (click)="onLogout()">Cerrar sesin</button>
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
    this.authService.logout();
    void this.router.navigateByUrl('/auth/login');
  }
}