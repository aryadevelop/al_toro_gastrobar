import { CommonModule } from '@angular/common';
import { Component, effect, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { RoleChipComponent } from '../../../../shared/ui/role-chip/role-chip.component';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, RoleChipComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Mi perfil" subtitle="Informacion de usuario en sesion"></app-page-header>
      <article class="card" style="padding: 1rem; max-width: 680px;">
        <app-role-chip [role]="authService.currentUser()?.role ?? 'CLIENTE'"></app-role-chip>

        <form class="form-grid" [formGroup]="profileForm" (ngSubmit)="onSubmit()" style="margin-top: .8rem;">
          <label>
            <span>Nombre completo</span>
            <input class="input-field" formControlName="fullName" />
          </label>

          <label>
            <span>Telefono</span>
            <input class="input-field" formControlName="phone" />
          </label>

          <label>
            <span>Avatar URL</span>
            <input class="input-field" formControlName="avatarUrl" />
          </label>

          <button class="btn-primary" type="submit">Guardar cambios</button>
          <p *ngIf="saved()" style="color: var(--success); margin: 0;">Cambios guardados con exito.</p>
        </form>
      </article>
    </section>
  `
})
export class ProfilePageComponent {
  readonly saved = signal(false);

  readonly profileForm = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    phone: [''],
    avatarUrl: ['']
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    public readonly authService: AuthService
  ) {
    effect(() => {
      const user = this.authService.currentUser();
      if (user) {
        this.profileForm.patchValue({
          fullName: user.fullName,
          phone: user.phone ?? '',
          avatarUrl: user.avatarUrl ?? ''
        });
      }
    });
  }

  onSubmit(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.authService.updateProfile(this.profileForm.getRawValue()).subscribe(() => {
      this.saved.set(true);
      setTimeout(() => this.saved.set(false), 1500);
    });
  }
}
