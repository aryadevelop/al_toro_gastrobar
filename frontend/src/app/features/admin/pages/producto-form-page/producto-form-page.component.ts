import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-producto-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Formulario de producto" subtitle="Alta o edicion de productos de venta"></app-page-header>
      <article class="card" style="padding: 1rem; max-width: 760px;">
        <form class="form-grid" [formGroup]="form" (ngSubmit)="onSubmit()">
          <label><span>Nombre</span><input class="input-field" formControlName="name" /></label>
          <label><span>Categoria</span><input class="input-field" formControlName="category" /></label>
          <label><span>Precio</span><input class="input-field" type="number" min="0" formControlName="price" /></label>
          <label>
            <span>Tipo</span>
            <select class="input-field" formControlName="type">
              <option value="DIRECT_SALE">Venta directa</option>
              <option value="PREPARATION">Preparacion</option>
            </select>
          </label>
          <button class="btn-primary" type="submit">Guardar producto</button>
          <p *ngIf="saved()" style="margin:0; color: var(--success);">Producto guardado (mock).</p>
        </form>
      </article>
    </section>
  `
})
export class ProductoFormPageComponent {
  readonly saved = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required]],
    category: ['', [Validators.required]],
    price: [0, [Validators.required, Validators.min(0)]],
    type: ['DIRECT_SALE' as 'DIRECT_SALE' | 'PREPARATION', [Validators.required]]
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
