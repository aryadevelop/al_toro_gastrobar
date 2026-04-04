import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-comanda-editor-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Editor de comanda" subtitle="Agregar y ajustar productos consumidos"></app-page-header>
      <article class="card" style="padding: 1rem; max-width: 700px;">
        <form class="form-grid" [formGroup]="comandaForm" (ngSubmit)="onSubmit()">
          <label>
            <span>Mesa</span>
            <input class="input-field" formControlName="mesaCode" />
          </label>
          <label>
            <span>Producto</span>
            <input class="input-field" formControlName="productName" />
          </label>
          <label>
            <span>Cantidad</span>
            <input class="input-field" type="number" min="1" formControlName="quantity" />
          </label>
          <label>
            <span>Observaciones</span>
            <textarea class="input-field" rows="3" formControlName="notes"></textarea>
          </label>
          <button class="btn-primary" type="submit">Actualizar comanda</button>
          <p style="margin:0; color: var(--success);" *ngIf="saved()">Comanda actualizada (simulado).</p>
        </form>
      </article>
    </section>
  `
})
export class ComandaEditorPageComponent {
  readonly saved = signal(false);

  readonly comandaForm = this.formBuilder.nonNullable.group({
    mesaCode: ['', [Validators.required]],
    productName: ['', [Validators.required]],
    quantity: [1, [Validators.required, Validators.min(1)]],
    notes: ['']
  });

  constructor(private readonly formBuilder: FormBuilder) {}

  onSubmit(): void {
    if (this.comandaForm.invalid) {
      this.comandaForm.markAllAsTouched();
      return;
    }

    this.saved.set(true);
  }
}
