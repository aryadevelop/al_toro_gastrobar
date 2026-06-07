import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ModalBaseComponent } from '../modal-base/modal-base.component';
import { InventoryMovementService } from '../../../core/services/inventory-movement.service';
import { BackendInventarioMovimientoRequest } from '../../../core/models/api.models';

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
        </p>

        <!-- Mensaje de Stock Insuficiente -->
        <div *ngIf="stockErrorMsg" class="alert alert-danger mb-3">
          {{ stockErrorMsg }}
        </div>

        <div class="form-group mb-3">
          <label class="form-label">Cantidad a {{ tipoMovimiento === 'INGRESO' ? 'ingresar' : 'egresar' }} *</label>
          <input 
            type="number" 
            class="form-control" 
            formControlName="cantidad" 
            [placeholder]="'Ej. 10'" 
            min="1"
          >
          <div *ngIf="form.get('cantidad')?.invalid && form.get('cantidad')?.touched" class="text-danger small mt-1">
            La cantidad es obligatoria y debe ser un número entero positivo.
          </div>
        </div>

        <div class="form-group mb-3">
          <label class="form-label">Fecha (opcional)</label>
          <input type="date" class="form-control" formControlName="fecha">
          <div class="form-text">Por defecto se registrará con la fecha de hoy.</div>
        </div>

        <div class="form-group mb-4">
          <label class="form-label">Observaciones (opcional)</label>
          <textarea 
            class="form-control" 
            rows="3" 
            formControlName="observaciones"
            placeholder="Detalles del ingreso o egreso..."
          ></textarea>
        </div>

        <div class="d-flex justify-content-end gap-2">
          <button type="button" class="btn btn-secondary" (click)="cerrarModal()" [disabled]="isSubmitting">Cancelar</button>
          <button type="submit" class="btn btn-primary" [disabled]="isSubmitting">
            <span *ngIf="isSubmitting" class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
            Registrar {{ tipoMovimiento }}
          </button>
        </div>
      </form>
    </app-modal-base>
  `
})
export class InventoryMovementModalComponent implements OnInit {
  @Input() open = false;
  @Input() tipoMovimiento: 'INGRESO' | 'EGRESO' = 'INGRESO';
  @Input() tipoElemento: 'PRODUCTO' | 'INSUMO' = 'PRODUCTO';
  @Input() elementoId: number | null = null;
  @Input() nombreElemento: string = '';
  @Input() stockActual: number = 0;
  
  @Output() readonly close = new EventEmitter<void>();
  @Output() readonly success = new EventEmitter<void>();

  stockErrorMsg: string | null = null;
  isSubmitting = false;

  form = this.fb.group({
    cantidad: [null as number | null, [Validators.required, Validators.min(1)]],
    fecha: [''],
    observaciones: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly movementService: InventoryMovementService
  ) {}

  ngOnInit(): void {
    const today = new Date().toISOString().split('T')[0];
    this.form.patchValue({ fecha: today });
  }

  cerrarModal() {
    this.form.reset();
    const today = new Date().toISOString().split('T')[0];
    this.form.patchValue({ fecha: today });
    this.stockErrorMsg = null;
    this.close.emit();
  }

  onSubmit() {
    if (this.form.invalid || !this.elementoId) {
      this.form.markAllAsTouched();
      return;
    }

    const cantidad = this.form.value.cantidad as number;

    // Validación de Egreso vs Stock
    if (this.tipoMovimiento === 'EGRESO' && cantidad > this.stockActual) {
      this.stockErrorMsg = `No hay suficiente stock. Stock actual: ${this.stockActual}. Por favor ingresa una cantidad menor o igual.`;
      return;
    }
    
    this.stockErrorMsg = null;
    this.isSubmitting = true;

    const req: BackendInventarioMovimientoRequest = {
      productoId: this.tipoElemento === 'PRODUCTO' ? this.elementoId : null,
      insumoId: this.tipoElemento === 'INSUMO' ? this.elementoId : null,
      tipo: this.tipoMovimiento,
      cantidad: cantidad,
      // la fecha no es parte del request estándar según DTO backend, 
      // pero si el controlador registro/movimientos lo soporta, el DTO en Angular lo debe ignorar si no existe
      // o se debe mapear según la interfaz. (Actualmente la interfaz de api.models no tiene fecha).
      observaciones: this.form.value.observaciones || undefined
    };

    this.movementService.registrarMovimiento(req).subscribe({
      next: () => {
        this.isSubmitting = false;
        window.alert(`Movimiento de ${this.tipoMovimiento} registrado exitosamente.`);
        this.success.emit();
        this.cerrarModal();
      },
      error: () => {
        this.isSubmitting = false;
        window.alert('Error al registrar el movimiento.');
      }
    });
  }
}
