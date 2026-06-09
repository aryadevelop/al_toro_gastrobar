import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Venta } from '../../../../core/models/domain.models';
import { SalesService } from '../../../../core/services/sales.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-ventas-list-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Ventas" subtitle="Consulta y filtros de ventas"></app-page-header>

      <article class="card filter-card">
        <form class="filters-grid" [formGroup]="filtersForm" (ngSubmit)="applyFilters()">
          <label>
            <span>Identificador de venta</span>
            <input
              class="input-field"
              type="text"
              formControlName="ventaId"
              placeholder="Ej: VENTA-1001"
              autocomplete="off"
            />
          </label>

          <label>
            <span>Fecha</span>
            <input class="input-field" type="date" formControlName="fecha" />
          </label>

          <label>
            <span>Fecha inicial</span>
            <input class="input-field" type="date" formControlName="fechaDesde" />
          </label>

          <label>
            <span>Fecha final</span>
            <input class="input-field" type="date" formControlName="fechaHasta" />
          </label>

          <label>
            <span>Metodo de pago</span>
            <select class="input-field" formControlName="metodoPago">
              <option value="">Todos</option>
              <option value="CASH">Efectivo</option>
              <option value="TRANSFER">Nequi</option>
              <option value="CARD">Tarjeta</option>
            </select>
          </label>

          <div class="actions-row">
            <button class="btn-secondary" type="button" (click)="clearFilters()">Limpiar filtros</button>
            <button class="btn-primary" type="submit">Buscar</button>
          </div>
        </form>
      </article>

      <article class="card state-card" *ngIf="loading()">
        <p>Cargando ventas...</p>
      </article>

      <article class="card state-card" *ngIf="infoMessage() && !loading()">
        <p>{{ infoMessage() }}</p>
      </article>

      <article class="card summary-card" *ngIf="showPeriodSummary() && filteredVentas().length > 0 && !loading()">
        <div>
          <p class="muted">Total acumulado del periodo</p>
          <strong>{{ totalPeriodo() | currency:'COP':'symbol':'1.0-0' }}</strong>
        </div>
      </article>

      <article class="card table-card" *ngIf="filteredVentas().length > 0 && !loading()">
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Identificador</th>
                <th>Fecha</th>
                <th>Metodo de pago</th>
                <th>Total</th>
                <th>Detalle</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let venta of filteredVentas()">
                <td data-label="Identificador">{{ venta.id }}</td>
                <td data-label="Fecha">{{ venta.createdAt | date:'short':'':'es-CO' }}</td>
                <td data-label="Metodo de pago">{{ formatMetodoPago(venta) }}</td>
                <td data-label="Total">{{ venta.total | currency:'COP':'symbol':'1.0-0' }}</td>
                <td data-label="Detalle">
                  <a class="btn-secondary" [routerLink]="['/app/admin/ventas', venta.id]">Ver detalle</a>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </section>
  `,
  styles: [
    `
      .filter-card {
        padding: 0.9rem;
      }

      .filters-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
        gap: 0.55rem 0.75rem;
        align-items: end;
      }

      .actions-row {
        display: flex;
        justify-content: flex-end;
        gap: 0.55rem;
      }

      .state-card {
        padding: 0.85rem;
      }

      .state-card p {
        margin: 0;
      }

      .summary-card {
        padding: 0.9rem;
        display: flex;
        align-items: center;
        justify-content: space-between;
      }

      .summary-card .muted {
        margin: 0;
        font-size: 0.82rem;
      }

      .table-card {
        padding: 0.8rem;
      }

      .table-wrapper {
        overflow: auto;
      }

      table {
        width: 100%;
        border-collapse: collapse;
        min-width: 780px;
      }

      th,
      td {
        text-align: left;
        padding: 0.55rem;
        border-bottom: 1px solid rgba(10, 10, 10, 0.1);
        font-size: 0.84rem;
      }

      th {
        font-size: 0.8rem;
        color: var(--muted);
      }

      a.btn-secondary {
        text-decoration: none;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }
    `,
  ],
})
export class VentasListPageComponent implements OnInit {
  private readonly defaultDate = this.toDateInputValue(new Date());

  readonly loading = signal(true);
  readonly infoMessage = signal('');
  readonly ventas = signal<Venta[]>([]);
  readonly filteredVentas = signal<Venta[]>([]);
  readonly totalPeriodo = signal(0);

  readonly filtersForm = this.formBuilder.nonNullable.group({
    ventaId: [''],
    fecha: [this.defaultDate],
    fechaDesde: [''],
    fechaHasta: [''],
    metodoPago: ['' as '' | 'CASH' | 'TRANSFER' | 'CARD'],
  });

  constructor(private readonly formBuilder: FormBuilder, private readonly salesService: SalesService) {}

  ngOnInit(): void {
    this.loadVentas();
  }

  applyFilters(): void {
    this.infoMessage.set('');
    const filters = this.filtersForm.getRawValue();

    if (!this.isVentaIdSafe(filters.ventaId)) {
      this.infoMessage.set('Caracteres no permitidos en la busqueda');
      return;
    }

    if (this.adjustFutureDates(filters)) {
      this.infoMessage.set('No es posible consultar ventas en fechas futuras');
    }

    const baseVentas = [...this.ventas()];
    let results = this.filterByDate(baseVentas, this.filtersForm.getRawValue());

    const metodoPago = filters.metodoPago;
    if (metodoPago !== '') {
      results = results.filter((venta) => this.matchesPaymentMethod(venta, metodoPago));
    }

    const ventaId = filters.ventaId.trim();
    if (ventaId) {
      const idLower = ventaId.toLowerCase();
      const filteredById = results.filter((venta) => venta.id.toLowerCase() === idLower);
      if (filteredById.length === 0) {
        this.infoMessage.set('No existe una venta con el identificador ingresado');
        return;
      }
      results = filteredById;
    }

    results = this.sortResults(results, this.hasRangeFilters());
    this.filteredVentas.set(results);
    this.totalPeriodo.set(this.computeTotal(results));

    if (results.length === 0 && !this.infoMessage()) {
      this.infoMessage.set(
        'No se encontraron ventas con los filtros seleccionados. Sugerencia: amplia el rango de busqueda.'
      );
    }
  }

  clearFilters(): void {
    this.filtersForm.setValue({
      ventaId: '',
      fecha: this.defaultDate,
      fechaDesde: '',
      fechaHasta: '',
      metodoPago: '',
    });
    this.infoMessage.set('');
    this.applyFilters();
  }

  showPeriodSummary(): boolean {
    return this.hasRangeFilters();
  }

  formatMetodoPago(venta: Venta): string {
    const method = venta.payments?.[0]?.method;
    if (method === 'CASH') {
      return 'Efectivo';
    }
    if (method === 'CARD') {
      return 'Tarjeta';
    }
    return method === 'TRANSFER' ? 'Nequi' : 'No registrado';
  }

  private loadVentas(): void {
    this.loading.set(true);
    this.salesService.list().subscribe({
      next: (result) => {
        this.ventas.set(result);
        this.loading.set(false);
        this.applyFilters();
      },
      error: () => {
        this.infoMessage.set('No fue posible cargar el listado de ventas.');
        this.loading.set(false);
      },
    });
  }

  private filterByDate(ventas: Venta[], filters: { fecha: string; fechaDesde: string; fechaHasta: string }): Venta[] {
    const hasRange = this.hasRangeFilters();
    if (hasRange) {
      const desde = filters.fechaDesde ? new Date(`${filters.fechaDesde}T00:00:00`) : null;
      const hasta = filters.fechaHasta ? new Date(`${filters.fechaHasta}T23:59:59`) : null;
      return ventas.filter((venta) => {
        const saleDate = new Date(venta.createdAt);
        if (desde && saleDate < desde) {
          return false;
        }
        if (hasta && saleDate > hasta) {
          return false;
        }
        return true;
      });
    }

    if (!filters.fecha) {
      return ventas;
    }

    return ventas.filter((venta) => this.isSameDate(venta.createdAt, filters.fecha));
  }

  private matchesPaymentMethod(venta: Venta, method: 'CASH' | 'TRANSFER' | 'CARD'): boolean {
    return (venta.payments ?? []).some((payment) => payment.method === method);
  }

  private sortResults(results: Venta[], chronological: boolean): Venta[] {
    return [...results].sort((a, b) => {
      const timeA = new Date(a.createdAt).getTime();
      const timeB = new Date(b.createdAt).getTime();
      return chronological ? timeA - timeB : timeB - timeA;
    });
  }

  private hasRangeFilters(): boolean {
    const { fechaDesde, fechaHasta } = this.filtersForm.getRawValue();
    return Boolean(fechaDesde || fechaHasta);
  }

  private computeTotal(ventas: Venta[]): number {
    return ventas.reduce((acc, venta) => acc + (venta.total ?? 0), 0);
  }

  private isVentaIdSafe(value: string): boolean {
    if (!value.trim()) {
      return true;
    }
    return /^[a-zA-Z0-9-]+$/.test(value.trim());
  }

  private adjustFutureDates(filters: { fecha: string; fechaDesde: string; fechaHasta: string }): boolean {
    const today = this.defaultDate;
    let adjusted = false;

    if (filters.fecha && filters.fecha > today) {
      this.filtersForm.patchValue({ fecha: today });
      adjusted = true;
    }

    if (filters.fechaDesde && filters.fechaDesde > today) {
      this.filtersForm.patchValue({ fechaDesde: today });
      adjusted = true;
    }

    if (filters.fechaHasta && filters.fechaHasta > today) {
      this.filtersForm.patchValue({ fechaHasta: today });
      adjusted = true;
    }

    return adjusted;
  }

  private isSameDate(isoDate: string, dateInput: string): boolean {
    const target = new Date(dateInput);
    const sale = new Date(isoDate);
    return (
      sale.getFullYear() === target.getFullYear() &&
      sale.getMonth() === target.getMonth() &&
      sale.getDate() === target.getDate()
    );
  }

  private toDateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
