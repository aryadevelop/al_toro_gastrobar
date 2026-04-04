import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Registro de cliente" subtitle="Crea una cuenta para reservas y preordenes"></app-page-header>
      <article class="card" style="padding: 1rem; max-width: 560px;">
        <form class="form-grid" [formGroup]="registerForm" (ngSubmit)="onSubmit()">
          <label>
            <span>Nombre completo</span>
            <input class="input-field" formControlName="fullName" />
          </label>

          <label>
            <span>Correo</span>
            <input class="input-field" type="email" formControlName="email" />
          </label>

          <label>
            <span>Telefono</span>
            <input class="input-field" formControlName="phone" />
          </label>

          <label>
            <span>Contrasena</span>
            <input class="input-field" type="password" formControlName="password" />
          </label>

          <p class="error-text" *ngIf="registerForm.invalid && registerForm.touched">Completa todos los campos requeridos.</p>

          <button class="btn-primary" type="submit" [disabled]="loading()">
            {{ loading() ? 'Registrando...' : 'Crear cuenta' }}
          </button>
        </form>
        <a routerLink="/auth/login" style="display: inline-block; margin-top: .8rem;">Ya tengo cuenta</a>
      </article>
    </section>
  `
})
export class RegisterPageComponent {
  readonly loading = signal(false);

  readonly registerForm = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.minLength(7)]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.authService.register(this.registerForm.getRawValue()).subscribe((user) => {
      this.loading.set(false);
      void this.router.navigateByUrl(this.authService.getLandingRouteForRole(user.role));
    });
  }
}
