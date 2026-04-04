import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-insumo-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Formulario de insumo" subtitle="Control de inventario base"></app-page-header>
      <article class="card" style="padding: 1rem; max-width: 760px;">
        <form class="form-grid" [formGroup]="form" (ngSubmit)="onSubmit()">
          <label><span>Nombre</span><input class="input-field" formControlName="name" /></label>
          <label><span>Unidad</span><input class="input-field" formControlName="unit" /></label>
          <label><span>Stock actual</span><input class="input-field" type="number" min="0" formControlName="currentStock" /></label>
          <label><span>Stock minimo</span><input class="input-field" type="number" min="0" formControlName="minStock" /></label>
          <button class="btn-primary" type="submit">Guardar insumo</button>
          <p *ngIf="saved()" style="margin:0; color: var(--success);">Insumo guardado (mock).</p>
        </form>
      </article>
    </section>
  `
})
export class InsumoFormPageComponent {
  readonly saved = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required]],
    unit: ['', [Validators.required]],
    currentStock: [0, [Validators.required, Validators.min(0)]],
    minStock: [0, [Validators.required, Validators.min(0)]]
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
