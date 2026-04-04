import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-preparacion-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Formulario de preparacion" subtitle="Crear o editar recetas base"></app-page-header>
      <article class="card" style="padding: 1rem; max-width: 760px;">
        <form class="form-grid" [formGroup]="form" (ngSubmit)="onSubmit()">
          <label><span>Nombre</span><input class="input-field" formControlName="name" /></label>
          <label><span>Minutos estimados</span><input class="input-field" type="number" min="1" formControlName="estimatedMinutes" /></label>
          <label><span>Ingredientes (coma separada)</span><textarea class="input-field" rows="3" formControlName="ingredients"></textarea></label>
          <button class="btn-primary" type="submit">Guardar preparacion</button>
          <p *ngIf="saved()" style="margin:0; color: var(--success);">Preparacion guardada (mock).</p>
        </form>
      </article>
    </section>
  `
})
export class PreparacionFormPageComponent {
  readonly saved = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required]],
    estimatedMinutes: [15, [Validators.required, Validators.min(1)]],
    ingredients: ['', [Validators.required]]
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
