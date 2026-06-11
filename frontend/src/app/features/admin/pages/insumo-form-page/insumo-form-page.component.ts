import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { InventoryMovementModalComponent } from '../../../../shared/ui/inventory-movement-modal/inventory-movement-modal.component';
import { InsumoAdminService } from '../../../../core/services/insumo-admin.service';
import { BackendInsumoDetalleResponse, BackendMovimientoHistorialItem } from '../../../../core/models/api.models';

@Component({
  selector: 'app-insumo-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, InventoryMovementModalComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Formulario de insumo" subtitle="Control de inventario base"></app-page-header>

      <!-- CA-06: Mensaje de éxito global -->
      <div *ngIf="mensajeExito" class="alert alert-success d-flex align-items-center gap-2" style="max-width: 760px;" role="alert">
        <span>✓</span> {{ mensajeExito }}
      </div>

      <!-- Panel de stock actual + botones (solo en modo edición) -->
      <article *ngIf="isEditMode && insumoDetalle" class="card mb-4"
        style="padding: 1rem; max-width: 760px; display: flex; justify-content: space-between; align-items: center; background-color: var(--color-bg-surface-alt);">
        <div>
          <h4 style="margin: 0;">
            Stock Actual:
            <span class="badge bg-primary fs-6">{{ stockActual }}</span>
            <span class="text-muted ms-2" style="font-size: 0.85rem;">{{ insumoDetalle.insumoUnidad }}</span>
          </h4>
        </div>
        <div class="d-flex gap-2">
          <!-- CA-01: Registrar ingreso -->
          <button class="btn btn-outline-primary" (click)="abrirMovimiento('INGRESO')">Registrar Ingreso</button>
          <!-- CA-02: Registrar egreso -->
          <button class="btn btn-outline-warning" (click)="abrirMovimiento('EGRESO')">Registrar Egreso</button>
        </div>
      </article>

      <!-- Skeleton mientras carga -->
      <div *ngIf="isEditMode && isLoadingDetalle" class="card mb-4"
        style="padding: 1rem; max-width: 760px; background-color: var(--color-bg-surface-alt);">
        <span class="text-muted">Cargando datos del insumo...</span>
      </div>

      <article class="card" style="padding: 1rem; max-width: 760px;">
        <form class="form-grid form-compact" [formGroup]="form" (ngSubmit)="onSubmit()">
          <label><span>Nombre</span><input class="input-field" formControlName="name" /></label>
          <label><span>Unidad</span><input class="input-field" formControlName="unit" /></label>
          <label><span>Stock actual</span><input class="input-field" type="number" min="0" formControlName="currentStock" /></label>
          <label><span>Stock mínimo</span><input class="input-field" type="number" min="0" formControlName="minStock" /></label>
          <button class="btn-primary" type="submit">Guardar insumo</button>
          <p *ngIf="saved()" style="margin:0; color: var(--success);">Insumo guardado.</p>
        </form>
      </article>

      <!-- CA-06: Historial de movimientos (solo en edición) -->
      <article *ngIf="isEditMode && movimientos.length > 0" class="card mt-4" style="max-width: 760px; padding: 1rem;">
        <h5 style="margin-bottom: 1rem;">Historial de Movimientos</h5>
        <div class="table-responsive">
          <table class="table table-sm table-striped">
            <thead>
              <tr>
                <th>Tipo</th>
                <th>Cantidad</th>
                <th>Fecha</th>
                <th>Observaciones</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let m of movimientos">
                <td>
                  <span class="badge" [class.bg-success]="m.tipo === 'INGRESO'" [class.bg-warning]="m.tipo === 'EGRESO'">
                    {{ m.tipo }}
                  </span>
                </td>
                <td>{{ m.cantidad }}</td>
                <td>{{ m.movimientoFechaHora | date:'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ m.observaciones || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <!-- Modal de Movimiento de Inventario -->
      <app-inventory-movement-modal
        [open]="isMovementModalOpen"
        [tipoMovimiento]="activeMovementType"
        tipoElemento="INSUMO"
        [elementoId]="insumoId"
        [nombreElemento]="insumoDetalle?.insumoNombre || form.value.name || 'Insumo'"
        [stockActual]="stockActual"
        [unidadElemento]="insumoDetalle?.insumoUnidad || ''"
        (close)="isMovementModalOpen = false"
        (success)="onMovementSuccess()"
      ></app-inventory-movement-modal>
    </section>
  `
})
export class InsumoFormPageComponent implements OnInit {
  readonly saved = signal(false);
  isEditMode = false;
  isLoadingDetalle = false;
  insumoId: number | null = null;
  insumoDetalle: BackendInsumoDetalleResponse | null = null;

  /** Stock real cargado desde el backend (CA-05, CA-06) */
  stockActual = 0;

  /** Historial de movimientos del insumo (CA-06) */
  movimientos: BackendMovimientoHistorialItem[] = [];

  /** Mensaje de éxito integrado (CA-06) */
  mensajeExito: string | null = null;

  // Estado del modal
  isMovementModalOpen = false;
  activeMovementType: 'INGRESO' | 'EGRESO' = 'INGRESO';

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required]],
    unit: ['', [Validators.required]],
    currentStock: [0, [Validators.required, Validators.min(0)]],
    minStock: [0, [Validators.required, Validators.min(0)]]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly insumoService: InsumoAdminService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.isEditMode = true;
      this.insumoId = parseInt(idParam, 10);
      this.cargarDetalle();
    }
  }

  /** Carga el detalle real del insumo desde el backend (CA-01, CA-02, CA-05) */
  cargarDetalle(): void {
    if (!this.insumoId) return;
    this.isLoadingDetalle = true;
    this.insumoService.obtenerDetalle(this.insumoId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.insumoDetalle = res.data;
          this.stockActual = Number(res.data.insumoStockActual);
          this.form.patchValue({
            name: res.data.insumoNombre,
            unit: res.data.insumoUnidad,
            currentStock: this.stockActual,
            minStock: 0
          });
        }
        this.isLoadingDetalle = false;
      },
      error: () => { this.isLoadingDetalle = false; }
    });

    // Cargar historial de movimientos
    this.insumoService.listarMovimientos(this.insumoId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          // Filtrar solo los movimientos de este insumo
          this.movimientos = res.data.filter(m => m.insumoId === this.insumoId);
        }
      },
      error: () => { /* silencioso */ }
    });
  }

  abrirMovimiento(tipo: 'INGRESO' | 'EGRESO') {
    this.activeMovementType = tipo;
    this.mensajeExito = null;
    this.isMovementModalOpen = true;
  }

  /** CA-06: Al registrar exitosamente, recargar el stock desde backend y mostrar mensaje */
  onMovementSuccess() {
    const tipoTexto = this.activeMovementType === 'INGRESO' ? 'Ingreso' : 'Egreso';
    this.mensajeExito = `${tipoTexto} registrado correctamente. Stock actualizado.`;

    // Recargar el detalle desde el backend para obtener el stock actualizado
    this.cargarDetalle();

    // Ocultar el mensaje tras 4 segundos
    setTimeout(() => { this.mensajeExito = null; }, 4000);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saved.set(true);
  }
}
