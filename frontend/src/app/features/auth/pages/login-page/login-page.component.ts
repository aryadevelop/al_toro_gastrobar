import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { LoginCredentials } from '../../../../core/models/auth.models';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, ConfirmDialogComponent],
  template: `
    <div class="login-container">
      <header class="login-header">
        <h2>Iniciar sesión</h2>
        <p>Ingresa tus credenciales para acceder al sistema</p>
      </header>

      <form class="login-form" [formGroup]="loginForm" (ngSubmit)="onSubmit()">
        <!-- Campo: correo electrónico -->
        <div class="form-group">
          <label for="login-email">Correo electrónico</label>
          <input
            id="login-email"
            class="input-field"
            type="email"
            formControlName="email"
            autocomplete="username"
            placeholder="correo&#64;ejemplo.com"
            [class.input-error]="loginForm.controls.email.touched && loginForm.controls.email.invalid"
          />
          <!-- Criterio 5: campo correo vacío -->
          <p
            class="error-text"
            *ngIf="loginForm.controls.email.touched && loginForm.controls.email.hasError('required')"
          >
            El correo es obligatorio
          </p>
          <!-- Criterio 4: formato de correo inválido -->
          <p
            class="error-text"
            *ngIf="
              loginForm.controls.email.touched &&
              loginForm.controls.email.hasError('email') &&
              !loginForm.controls.email.hasError('required')
            "
          >
            Por favor ingresa un correo electrónico válido (ej. juan&#64;gmail.com)
          </p>
        </div>

        <!-- Campo: contraseña -->
        <div class="form-group">
          <label for="login-password">Contraseña</label>
          <input
            id="login-password"
            class="input-field"
            type="password"
            formControlName="password"
            autocomplete="current-password"
            [class.input-error]="loginForm.controls.password.touched && loginForm.controls.password.invalid"
          />
          <!-- Criterio 6: campo contraseña vacío -->
          <p
            class="error-text"
            *ngIf="loginForm.controls.password.touched && loginForm.controls.password.hasError('required')"
          >
            La contraseña es obligatoria
          </p>
        </div>

        <!-- Criterio 2: credenciales incorrectas / Criterio 3: cuenta suspendida -->
        <div class="auth-error" *ngIf="errorMessage()">
          <div class="auth-error__dot"></div>
          <p>{{ errorMessage() }}</p>
        </div>

        <button class="btn-primary btn-submit" type="submit" [disabled]="loading()">
          {{ loading() ? 'Ingresando...' : 'Ingresar' }}
        </button>
      </form>

      <footer class="login-footer">
        <a routerLink="/auth/register" class="register-link">
          ¿No tienes cuenta? Crear cuenta de cliente
        </a>
      </footer>
    </div>
    <app-confirm-dialog
      [open]="showActiveSessionDialog()"
      title="Sesión Activa"
      message="Ya tienes una sesión activa en otro dispositivo. ¿Deseas cerrar la otra sesión y continuar?"
      (confirm)="onConfirmForceLogin()"
      (cancel)="showActiveSessionDialog.set(false)"
    ></app-confirm-dialog>
  `,
  styles: [
    `
      .login-container {
        width: 100%;
      }

      .login-header {
        margin-bottom: 1.8rem;
      }

      .login-header h2 {
        margin: 0;
        font-size: 1.6rem;
        font-weight: 700;
        color: var(--text);
      }

      .login-header p {
        margin: 0.4rem 0 0;
        color: var(--muted);
        font-size: 0.9rem;
      }

      .login-form {
        display: grid;
        gap: 1.1rem;
      }

      .form-group {
        display: grid;
        gap: 0.35rem;
      }

      .form-group label {
        font-size: 0.88rem;
        font-weight: 600;
        color: var(--text);
      }

      .auth-error {
        display: flex;
        align-items: flex-start;
        gap: 0.6rem;
        padding: 0.75rem 0.9rem;
        border-radius: 10px;
        background: rgba(111, 78, 55, 0.07);
        border: 1px solid rgba(111, 78, 55, 0.2);
      }

      .auth-error__dot {
        flex-shrink: 0;
        width: 6px;
        height: 6px;
        margin-top: 0.45rem;
        border-radius: 50%;
        background: var(--danger);
      }

      .auth-error p {
        margin: 0;
        color: var(--danger);
        font-size: 0.85rem;
        font-weight: 500;
        line-height: 1.4;
      }

      .btn-submit {
        width: 100%;
        padding: 0.75rem;
        font-size: 0.95rem;
        margin-top: 0.4rem;
        transition: opacity 0.2s ease;
      }

      .btn-submit:disabled {
        opacity: 0.65;
        cursor: not-allowed;
      }

      .btn-submit:not(:disabled):hover {
        opacity: 0.9;
      }

      .login-footer {
        margin-top: 1.5rem;
        display: grid;
        gap: 0.6rem;
        text-align: center;
      }

      .register-link {
        color: var(--primary);
        font-weight: 600;
        font-size: 0.88rem;
        transition: opacity 0.2s ease;
      }

      .register-link:hover {
        opacity: 0.8;
      }

      .demo-hint {
        color: var(--muted);
        font-size: 0.75rem;
      }
    `
  ]
})
export class LoginPageComponent {
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly showActiveSessionDialog = signal(false);
  readonly pendingCredentials = signal<LoginCredentials | null>(null);

  readonly loginForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: (user) => {
        this.loading.set(false);
        void this.router.navigateByUrl(this.authService.getLandingRouteForRole(user.role));
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        const code = err.error?.code;
        const message = String(err.error?.message ?? '').toLowerCase();
        const hasActiveSession = err.status === 409 || message.includes('sesión activa') || message.includes('sesion activa');

        if (code === 'PASSWORD_EXPIRED') {
          void this.router.navigateByUrl('/auth/change-password');
        } else if (code === 'ACTIVE_SESSION' || hasActiveSession) {
          this.pendingCredentials.set(this.loginForm.getRawValue());
          this.showActiveSessionDialog.set(true);
        } else {
          this.errorMessage.set(err.error?.message ?? 'Error al iniciar sesión');
        }
      }
    });
  }

  onConfirmForceLogin(): void {
    const creds = this.pendingCredentials();
    if (!creds) return;

    this.showActiveSessionDialog.set(false);
    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.login({ ...creds, forceSessionOverride: true }).subscribe({
      next: (user) => {
        this.loading.set(false);
        void this.router.navigateByUrl(this.authService.getLandingRouteForRole(user.role));
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Error al iniciar sesión');
      }
    });
  }
}


