import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-personal-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Formulario de personal" subtitle="Alta o edicion de empleados"></app-page-header>
      <article class="card" style="padding: 1rem; max-width: 760px;">
        <form class="form-grid" [formGroup]="form" (ngSubmit)="onSubmit()">
          <label><span>Nombre completo</span><input class="input-field" formControlName="fullName" /></label>
          <label><span>Correo</span><input class="input-field" type="email" formControlName="email" /></label>
          <label>
            <span>Rol</span>
            <select class="input-field" formControlName="role">
              <option value="MESERO">Mesero</option>
              <option value="PRODUCCION">Produccion</option>
              <option value="CAJERO">Cajero</option>
              <option value="ADMIN">Administrador</option>
            </select>
          </label>
          <label><span>Codigo interno</span><input class="input-field" formControlName="employeeCode" /></label>
          <button class="btn-primary" type="submit">Guardar personal</button>
          <p *ngIf="saved()" style="margin:0; color: var(--success);">Personal guardado (mock).</p>
        </form>
      </article>
    </section>
  `
})
export class PersonalFormPageComponent {
  readonly saved = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    role: ['MESERO' as 'ADMIN' | 'MESERO' | 'PRODUCCION' | 'CAJERO', [Validators.required]],
    employeeCode: ['', [Validators.required]]
  });

  constructor(private readonly formBuilder: FormBuilder) {}

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saved.set(true);
  }
}
