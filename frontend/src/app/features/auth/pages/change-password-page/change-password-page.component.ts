import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-change-password-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="login-container">
      <header class="login-header" style="margin-bottom: 1.2rem;">
        <h2>Cambio de contraseña</h2>
        <p style="color: var(--danger); margin-top: 0.5rem; font-weight: 500;">
          Tu contraseña ha expirado. Por favor crea una nueva.
        </p>
      </header>

      <form class="login-form">
        <div class="form-group">
          <label>Nueva contraseña</label>
          <input class="input-field" type="password" placeholder="Ingresa nueva contraseña" />
        </div>
        <div class="form-group">
          <label>Confirmar contraseña</label>
          <input class="input-field" type="password" placeholder="Confirma nueva contraseña" />
        </div>

        <button class="btn-primary btn-submit" type="submit" style="margin-top: 1rem;">
          Actualizar contraseña
        </button>
      </form>

      <footer class="login-footer">
        <a routerLink="/auth/login" class="register-link">Volver al inicio de sesión</a>
      </footer>
    </div>
  `
})
export class ChangePasswordPageComponent {}

