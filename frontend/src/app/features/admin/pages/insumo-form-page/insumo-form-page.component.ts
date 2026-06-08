import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { InventoryMovementModalComponent } from '../../../../shared/ui/inventory-movement-modal/inventory-movement-modal.component';

@Component({
  selector: 'app-insumo-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, InventoryMovementModalComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Formulario de insumo" subtitle="Control de inventario base"></app-page-header>
      
      <!-- Panel superior de acciones de stock (solo visible en modo edición) -->
      <article *ngIf="isEditMode" class="card mb-4" style="padding: 1rem; max-width: 760px; display: flex; justify-content: space-between; align-items: center; background-color: var(--color-bg-surface-alt);">
        <div>
          <h4 style="margin: 0;">Stock Actual: <span class="badge bg-primary fs-6">{{ mockStockActual }}</span></h4>
        </div>
        <div class="d-flex gap-2">
          <button class="btn btn-outline-primary" (click)="abrirMovimiento('INGRESO')">Registrar Ingreso</button>
          <button class="btn btn-outline-warning" (click)="abrirMovimiento('EGRESO')">Registrar Egreso</button>
        </div>
      </article>

      <article class="card" style="padding: 1rem; max-width: 760px;">
        <form class="form-grid form-compact" [formGroup]="form" (ngSubmit)="onSubmit()">
          <label><span>Nombre</span><input class="input-field" formControlName="name" /></label>
          <label><span>Unidad</span><input class="input-field" formControlName="unit" /></label>
          <label><span>Stock actual</span><input class="input-field" type="number" min="0" formControlName="currentStock" /></label>
          <label><span>Stock minimo</span><input class="input-field" type="number" min="0" formControlName="minStock" /></label>
          <button class="btn-primary" type="submit">Guardar insumo</button>
          <p *ngIf="saved()" style="margin:0; color: var(--success);">Insumo guardado (mock).</p>
        </form>
      </article>

      <!-- Modal de Movimiento de Inventario -->
      <app-inventory-movement-modal
        [open]="isMovementModalOpen"
        [tipoMovimiento]="activeMovementType"
        tipoElemento="INSUMO"
        [elementoId]="insumoId"
        [nombreElemento]="form.value.name || 'Insumo Sin Nombre'"
        [stockActual]="mockStockActual"
        (close)="isMovementModalOpen = false"
        (success)="onMovementSuccess()"
      ></app-inventory-movement-modal>
    </section>
  `
})
export class InsumoFormPageComponent implements OnInit {
  readonly saved = signal(false);
  isEditMode = false;
  insumoId: number | null = null;
  
  // Mock data for stock simulation
  mockStockActual = 120;

  // Modal State
  isMovementModalOpen = false;
  activeMovementType: 'INGRESO' | 'EGRESO' = 'INGRESO';

  readonly form = this.formBuilder.nonNullable.group({
    name: ['Tomate Cherry', [Validators.required]],
    unit: ['kg', [Validators.required]],
    currentStock: [120, [Validators.required, Validators.min(0)]],
    minStock: [10, [Validators.required, Validators.min(0)]]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.isEditMode = true;
      this.insumoId = parseInt(idParam, 10) || 456;
    } else {
      this.isEditMode = true;
      this.insumoId = 456;
    }
  }

  abrirMovimiento(tipo: 'INGRESO' | 'EGRESO') {
    this.activeMovementType = tipo;
    this.isMovementModalOpen = true;
  }

  onMovementSuccess() {
    if (this.activeMovementType === 'INGRESO') {
      this.mockStockActual += 10;
    } else {
      this.mockStockActual -= 5;
    }
    this.form.patchValue({ currentStock: this.mockStockActual });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saved.set(true);
  }
}
