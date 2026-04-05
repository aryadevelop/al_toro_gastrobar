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
          <!-- ════════════════════════════════════════════════════════════ -->
          <!-- LOGO: Reemplazar el contenido de .logo-placeholder con:
               <img src="assets/images/logo.png"
                    alt="Al Toro Gastrobar"
                    class="logo-img" />
               Tamaño recomendado: 120x120px (cuadrado) o 200x80px (horizontal)
               Agregar la imagen en: frontend/src/assets/images/logo.png
               y registrar la carpeta en angular.json > assets si es necesario.
          -->
          <!-- ════════════════════════════════════════════════════════════ -->
          <div class="logo-placeholder">
            <span class="logo-initials">AT</span>
          </div>

          <h1 class="brand-name">Al Toro</h1>
          <span class="brand-label">Gastrobar</span>
          <p class="brand-tagline">Gestión integral del restaurante</p>
        </div>

        <p class="auth-panel__footer">&copy; 2026 Al Toro Gastrobar &mdash; Equipo ARYA</p>
      </section>

      <section class="auth-content">
        <div class="auth-form-wrapper">
          <div class="mobile-brand">
            <div class="logo-placeholder-small">
              <span class="logo-initials-small">AT</span>
            </div>
            <h2 class="mobile-brand-name">Al Toro <span>Gastrobar</span></h2>
          </div>
          <router-outlet></router-outlet>
        </div>
      </section>
    </div>
  `,
  styleUrls: ['./auth-layout.component.scss']
})
export class AuthLayoutComponent {}