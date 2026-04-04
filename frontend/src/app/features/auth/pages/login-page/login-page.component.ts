import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Iniciar sesion" subtitle="Accede segun tu rol operativo"></app-page-header>

      <article class="card" style="padding: 1rem; max-width: 460px;">
        <form class="form-grid" [formGroup]="loginForm" (ngSubmit)="onSubmit()">
          <label>
            <span>Correo</span>
            <input class="input-field" type="email" formControlName="email" />
            <p class="error-text" *ngIf="loginForm.controls.email.touched && loginForm.controls.email.invalid">
              Ingresa un correo valido.
            </p>
          </label>

          <label>
            <span>Contrasena</span>
            <input class="input-field" type="password" formControlName="password" />
            <p class="error-text" *ngIf="loginForm.controls.password.touched && loginForm.controls.password.invalid">
              La contrasena es obligatoria.
            </p>
          </label>

          <p class="error-text" *ngIf="errorMessage()">{{ errorMessage() }}</p>

          <button class="btn-primary" type="submit" [disabled]="loading()">
            {{ loading() ? 'Ingresando...' : 'Ingresar' }}
          </button>
        </form>

        <p style="margin-top: 1rem; color: var(--muted);">
          Demo: admin&#64;altoro.local / Admin123*
        </p>

        <a routerLink="/auth/register" style="display: inline-block; margin-top: .5rem;">Crear cuenta de cliente</a>
      </article>
    </section>
  `
})
export class LoginPageComponent {
  readonly loading = signal(false);
  readonly errorMessage = signal('');

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
      error: (error: Error) => {
        this.loading.set(false);
        this.errorMessage.set(error.message);
      }
    });
  }
}
