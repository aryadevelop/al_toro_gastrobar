import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ModalBaseComponent } from '../modal-base/modal-base.component';
import { InventoryMovementService } from '../../../core/services/inventory-movement.service';
import { BackendInventarioMovimientoRequest } from '../../../core/models/api.models';

/**
 * Modal reutilizable para registrar ingresos y egresos de inventario.
 *
 * CA-01 (ingreso insumo) y CA-03 (ingreso producto): muestra cantidad, fecha y observaciones.
 * CA-02 (egreso insumo): muestra solo cantidad.
 * CA-04 (egreso producto): muestra cantidad, fecha y observaciones.
 * CA-05: valida que egreso no supere stock, mostrando mensaje exacto requerido.
 * CA-06: al éxito, emite evento para que el padre recargue el stock y muestra mensaje integrado.
 */
@Component({
  selector: 'app-inventory-movement-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ModalBaseComponent],
  template: `
    <app-modal-base
      [open]="open"
      [title]="tipoMovimiento === 'INGRESO' ? 'Registrar Ingreso' : 'Registrar Egreso'"
      (close)="cerrarModal()">

      <form [formGroup]="form" (ngSubmit)="onSubmit()">
        <p class="mb-3 text-secondary">
          Registrando movimiento para <strong>{{ nombreElemento }}</strong>
          &nbsp;·&nbsp;
          <span class="badge" [class.bg-success]="tipoMovimiento === 'INGRESO'" [class.bg-warning]="tipoMovimiento === 'EGRESO'">
            {{ tipoMovimiento }}
          </span>
        </p>

        <!-- CA-05: Mensaje de Stock Insuficiente -->
        <div *ngIf="stockErrorMsg" class="alert alert-danger mb-3" role="alert">
          {{ stockErrorMsg }}
        </div>

        <!-- CA-06: Mensaje de éxito integrado -->
        <div *ngIf="successMsg" class="alert alert-success mb-3" role="alert">
          <span class="me-2">✓</span>{{ successMsg }}
        </div>

        <!-- Campo: Cantidad (obligatorio siempre — CA-01, CA-02, CA-03, CA-04) -->
        <div class="form-group mb-3">
          <label class="form-label">
            Cantidad a {{ tipoMovimiento === 'INGRESO' ? 'ingresar' : 'egresar' }}
            <span class="text-danger">*</span>
          </label>
          <input
            type="number"
            class="form-control"
            formControlName="cantidad"
            placeholder="Ej. 10"
            [min]="1"
            [step]="esInsumo ? '0.001' : '1'"
          >
          <div *ngIf="form.get('cantidad')?.invalid && form.get('cantidad')?.touched" class="text-danger small mt-1">
            La cantidad es obligatoria y debe ser un número positivo.
          </div>
          <div class="form-text" *ngIf="tipoMovimiento === 'EGRESO'">
            Stock actual: <strong>{{ stockActual }}</strong> {{ unidadElemento }}
          </div>
        </div>

        <!-- Campos adicionales para INGRESO y para EGRESO de PRODUCTO (CA-01, CA-03, CA-04) -->
        <ng-container *ngIf="tipoMovimiento === 'INGRESO' || tipoElemento === 'PRODUCTO'">

          <!-- Fecha (hoy por defecto, opcional — CA-01, CA-03, CA-04) -->
          <div class="form-group mb-3">
            <label class="form-label">Fecha <span class="text-muted">(opcional)</span></label>
            <input type="date" class="form-control" formControlName="fecha">
            <div class="form-text">Por defecto se registrará con la fecha de hoy.</div>
          </div>

          <!-- Observaciones (opcional — CA-01, CA-03, CA-04) -->
          <div class="form-group mb-4">
            <label class="form-label">Observaciones <span class="text-muted">(opcional)</span></label>
            <textarea
              class="form-control"
              rows="3"
              formControlName="observaciones"
              placeholder="Detalles del movimiento..."
            ></textarea>
          </div>
        </ng-container>

        <div class="d-flex justify-content-end gap-2">
          <button type="button" class="btn btn-secondary" (click)="cerrarModal()" [disabled]="isSubmitting">Cancelar</button>
          <button type="submit" class="btn btn-primary" [disabled]="isSubmitting">
            <span *ngIf="isSubmitting" class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
            Confirmar {{ tipoMovimiento === 'INGRESO' ? 'Ingreso' : 'Egreso' }}
          </button>
        </div>
      </form>
    </app-modal-base>
  `
})
export class InventoryMovementModalComponent implements OnInit, OnChanges {
  @Input() open = false;
  @Input() tipoMovimiento: 'INGRESO' | 'EGRESO' = 'INGRESO';
  @Input() tipoElemento: 'PRODUCTO' | 'INSUMO' = 'PRODUCTO';
  @Input() elementoId: number | null = null;
  @Input() nombreElemento: string = '';
  @Input() stockActual: number | null = null;
  @Input() unidadElemento: string = '';

  @Output() readonly close = new EventEmitter<void>();
  /** Emite el nuevo stock actualizado para que el padre lo refleje sin recargar */
  @Output() readonly success = new EventEmitter<void>();

  stockErrorMsg: string | null = null;
  successMsg: string | null = null;
  isSubmitting = false;

  get esInsumo(): boolean {
    return this.tipoElemento === 'INSUMO';
  }

  form = this.fb.group({
    cantidad: [null as number | null, [Validators.required, Validators.min(0.001)]],
    fecha: [''],
    observaciones: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly movementService: InventoryMovementService
  ) {}

  ngOnInit(): void {
    this.resetFechaHoy();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Al abrir el modal, limpiar mensajes y restablecer fecha a hoy
    if (changes['open'] && this.open) {
      this.stockErrorMsg = null;
      this.successMsg = null;
      this.resetFechaHoy();
    }
  }

  cerrarModal() {
    this.form.reset();
    this.resetFechaHoy();
    this.stockErrorMsg = null;
    this.successMsg = null;
    this.close.emit();
  }

  onSubmit() {
    if (this.form.invalid || !this.elementoId) {
      this.form.markAllAsTouched();
      return;
    }

    const cantidad = this.form.value.cantidad as number;
    const stockSeguro = this.stockActual ?? 0;

    // CA-05: Validación de egreso mayor a stock actual
    if (this.tipoMovimiento === 'EGRESO' && cantidad > stockSeguro) {
      this.stockErrorMsg = `No hay suficiente stock. Stock actual: ${stockSeguro}. Por favor ingresa una cantidad menor o igual.`;
      return;
    }

    this.stockErrorMsg = null;
    this.successMsg = null;
    this.isSubmitting = true;

    // Construir fecha en formato ISO-8601 si se ingresó una
    let fechaISO: string | null = null;
    const fechaVal = this.form.value.fecha;
    if (fechaVal) {
      // El input date entrega YYYY-MM-DD; el backend acepta LocalDateTime, usamos T00:00:00
      fechaISO = `${fechaVal}T00:00:00`;
    }

    const req: BackendInventarioMovimientoRequest = {
      productoId: this.tipoElemento === 'PRODUCTO' ? this.elementoId : null,
      insumoId: this.tipoElemento === 'INSUMO' ? this.elementoId : null,
      tipo: this.tipoMovimiento,
      cantidad: cantidad,
      observaciones: this.form.value.observaciones || undefined,
      fecha: fechaISO
    };

    this.movementService.registrarMovimiento(req).subscribe({
      next: () => {
        this.isSubmitting = false;
        // CA-06: mostrar mensaje de éxito integrado (sin window.alert)
        this.successMsg = `${this.tipoMovimiento === 'INGRESO' ? 'Ingreso' : 'Egreso'} registrado exitosamente.`;
        this.success.emit();
        // Cerrar el modal automáticamente tras 1.5 segundos
        setTimeout(() => this.cerrarModal(), 1500);
      },
      error: (err) => {
        this.isSubmitting = false;
        // CA-05: si el backend rechaza por stock insuficiente, mostrar mensaje exacto
        const msg: string = err?.error?.message || '';
        if (msg.toLowerCase().includes('stock insuficiente') || msg.toLowerCase().includes('stock actual')) {
          // Extraer valor numérico del mensaje backend si viene en el error
          const match = msg.match(/(\d+[\.,]?\d*)/);
          const stockBackend = match ? match[1] : this.stockActual;
          this.stockErrorMsg = `No hay suficiente stock. Stock actual: ${stockBackend}. Por favor ingresa una cantidad menor o igual.`;
        } else {
          this.stockErrorMsg = 'Error al registrar el movimiento. Inténtalo de nuevo.';
        }
      }
    });
  }

  private resetFechaHoy(): void {
    const today = new Date().toISOString().split('T')[0];
    this.form.patchValue({ fecha: today });
  }
}
