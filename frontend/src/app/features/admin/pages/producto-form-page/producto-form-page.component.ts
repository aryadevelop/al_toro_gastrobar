import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { InventoryMovementModalComponent } from '../../../../shared/ui/inventory-movement-modal/inventory-movement-modal.component';
import { ProductoAdminService } from '../../../../core/services/producto-admin.service';
import { InventoryMovementService } from '../../../../core/services/inventory-movement.service';
import { BackendProductoInventarioResponse, BackendMovimientoHistorialItem } from '../../../../core/models/api.models';

@Component({
  selector: 'app-producto-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, InventoryMovementModalComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Formulario de producto" subtitle="Alta o edición de productos de venta"></app-page-header>

      <!-- CA-06: Mensaje de éxito global -->
      <div *ngIf="mensajeExito" class="alert alert-success d-flex align-items-center gap-2" style="max-width: 760px;" role="alert">
        <span>✓</span> {{ mensajeExito }}
      </div>

      <!-- Panel de stock actual + botones (solo en modo edición y solo si tiene stock) -->
      <article *ngIf="isEditMode && productoDetalle" class="card mb-4"
        style="padding: 1rem; max-width: 760px; display: flex; justify-content: space-between; align-items: center; background-color: var(--color-bg-surface-alt);">
        <div>
          <h4 style="margin: 0;">
            Stock Actual:
            <span class="badge bg-primary fs-6">{{ stockActual ?? '—' }}</span>
            <small *ngIf="productoDetalle.stockActual === null" class="text-muted ms-2">
              (Preparación — sin stock propio)
            </small>
          </h4>
        </div>
        <div class="d-flex gap-2" *ngIf="productoDetalle.stockActual !== null">
          <!-- CA-03: Registrar ingreso de producto -->
          <button class="btn btn-outline-primary" (click)="abrirMovimiento('INGRESO')">Registrar Ingreso</button>
          <!-- CA-04: Registrar egreso de producto -->
          <button class="btn btn-outline-warning" (click)="abrirMovimiento('EGRESO')">Registrar Egreso</button>
        </div>
      </article>

      <!-- Skeleton mientras carga -->
      <div *ngIf="isEditMode && isLoadingDetalle" class="card mb-4"
        style="padding: 1rem; max-width: 760px; background-color: var(--color-bg-surface-alt);">
        <span class="text-muted">Cargando datos del producto...</span>
      </div>

      <article class="card" style="padding: 1rem; max-width: 760px;">
        <form class="form-grid form-compact" [formGroup]="form" (ngSubmit)="onSubmit()">
          <label><span>Nombre</span><input class="input-field" formControlName="name" /></label>
          <label><span>Categoría</span><input class="input-field" formControlName="category" /></label>
          <label><span>Precio</span><input class="input-field" type="number" min="0" formControlName="price" /></label>
          <label>
            <span>Tipo</span>
            <select class="input-field" formControlName="type">
              <option value="DIRECT_SALE">Venta directa</option>
              <option value="PREPARATION">Preparación</option>
            </select>
          </label>
          <button class="btn-primary" type="submit">Guardar producto</button>
          <p *ngIf="saved()" style="margin:0; color: var(--success);">Producto guardado.</p>
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
        tipoElemento="PRODUCTO"
        [elementoId]="productoId"
        [nombreElemento]="productoDetalle?.productoNombre || form.value.name || 'Producto'"
        [stockActual]="stockActual"
        (close)="isMovementModalOpen = false"
        (success)="onMovementSuccess()"
      ></app-inventory-movement-modal>
    </section>
  `
})
export class ProductoFormPageComponent implements OnInit {
  readonly saved = signal(false);
  isEditMode = false;
  isLoadingDetalle = false;
  productoId: number | null = null;
  productoDetalle: BackendProductoInventarioResponse | null = null;

  /** Stock real cargado desde el backend (CA-05, CA-06) */
  stockActual: number | null = null;

  /** Historial de movimientos del producto (CA-06) */
  movimientos: BackendMovimientoHistorialItem[] = [];

  /** Mensaje de éxito integrado (CA-06) */
  mensajeExito: string | null = null;

  // Estado del modal
  isMovementModalOpen = false;
  activeMovementType: 'INGRESO' | 'EGRESO' = 'INGRESO';

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required]],
    category: ['', [Validators.required]],
    price: [0, [Validators.required, Validators.min(0)]],
    type: ['DIRECT_SALE' as 'DIRECT_SALE' | 'PREPARATION', [Validators.required]]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly productoService: ProductoAdminService,
    private readonly movimientoService: InventoryMovementService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.isEditMode = true;
      this.productoId = parseInt(idParam, 10);
      this.cargarDetalle();
    }
  }

  /** Carga el detalle real del producto desde el backend (CA-03, CA-04, CA-05) */
  cargarDetalle(): void {
    if (!this.productoId) return;
    this.isLoadingDetalle = true;
    this.productoService.listarInventario().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const producto = res.data.find(p => p.productoId === this.productoId);
          if (producto) {
            this.productoDetalle = producto;
            this.stockActual = Number(producto.stockActual ?? 0);
            this.form.patchValue({
              name: producto.productoNombre,
              category: producto.categoriaNombre,
              price: Number(producto.productoPrecio)
            });
          }
        }
        this.isLoadingDetalle = false;
      },
      error: () => { this.isLoadingDetalle = false; }
    });

    // Cargar historial de movimientos del producto
    this.movimientoService.listarHistorialPorProducto(this.productoId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.movimientos = res.data.filter(m => m.productoId === this.productoId);
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
