import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="auth-layout">
      <section class="auth-panel">
        <div class="auth-panel__content">
          <img src="assets/images/al-toro-logo-vector.svg" alt="Al Toro Gastrobar" class="logo-img logo-img--desktop" />
          <p class="brand-tagline">Gestión integral del restaurante</p>
        </div>

        <p class="auth-panel__footer">&copy; 2026 Al Toro Gastrobar &mdash; Equipo ARYA</p>
      </section>

      <section class="auth-content">
        <div class="auth-form-wrapper">
          <div class="mobile-brand">
            <img src="assets/images/al-toro-logo-vector.svg" alt="Al Toro Gastrobar" class="logo-img logo-img--mobile" />
          </div>
          <router-outlet></router-outlet>
        </div>
      </section>
    </div>
  `,
  styleUrls: ['./auth-layout.component.scss']
})
export class AuthLayoutComponent {}