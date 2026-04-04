import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SalesService } from '../../../../core/services/sales.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-pago-cierre-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Pago y cierre de venta" subtitle="Registrar pago de comanda y cerrar venta"></app-page-header>
      <article class="card" style="padding: 1rem; max-width: 740px;">
        <form class="form-grid" [formGroup]="paymentForm" (ngSubmit)="onSubmit()">
          <label>
            <span>ID Comanda</span>
            <input class="input-field" formControlName="comandaId" />
          </label>

          <label>
            <span>Monto total</span>
            <input class="input-field" type="number" min="0" formControlName="total" />
          </label>

          <label>
            <span>Metodo de pago</span>
            <select class="input-field" formControlName="method">
              <option value="CASH">Efectivo</option>
              <option value="CARD">Tarjeta</option>
              <option value="TRANSFER">Transferencia</option>
            </select>
          </label>

          <button class="btn-primary" type="submit">Registrar pago</button>
          <p style="margin: 0; color: var(--success);" *ngIf="saved()">Pago registrado y venta cerrada.</p>
        </form>
      </article>
    </section>
  `
})
export class PagoCierrePageComponent {
  readonly saved = signal(false);

  readonly paymentForm = this.formBuilder.nonNullable.group({
    comandaId: ['', [Validators.required]],
    total: [0, [Validators.required, Validators.min(0)]],
    method: ['CASH' as 'CASH' | 'CARD' | 'TRANSFER', [Validators.required]]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly salesService: SalesService
  ) {}

  onSubmit(): void {
    if (this.paymentForm.invalid) {
      this.paymentForm.markAllAsTouched();
      return;
    }

    const formValue = this.paymentForm.getRawValue();
    this.salesService
      .closeSale({
        clienteId: 'u-2',
        comandaId: formValue.comandaId,
        total: formValue.total,
        paid: true,
        payments: [
          {
            id: `pay-${Date.now()}`,
            saleId: 'pending-id',
            method: formValue.method,
            amount: formValue.total,
            paidAt: new Date().toISOString()
          }
        ]
      })
      .subscribe(() => {
        this.saved.set(true);
      });
  }
}
