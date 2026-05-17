import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  ClienteBusqueda,
  ClienteSearchMode,
  ClienteVentasAdminService,
  ClienteVentasHistory,
  VentaAgrupadaAnio,
  VentaAgrupadaMes,
} from '../../../../core/services/cliente-ventas-admin.service';

type GroupMode = 'none' | 'anio' | 'mes';

@Component({
  selector: 'app-cliente-historial-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="page-grid">
      <h1>Historial de compras</h1>
      <p class="muted">Consulta administrativa por cliente</p>

      <article class="card search-card">
        <form class="form-grid form-compact" [formGroup]="searchForm" (ngSubmit)="onSearch()">
          <label>
            <span>Buscar por</span>
            <select class="input-field" formControlName="mode">
              <option *ngFor="let mode of searchModes" [value]="mode.value">{{ mode.label }}</option>
            </select>
          </label>
          <label>
            <span>Valor</span>
            <input
              class="input-field"
              type="text"
              formControlName="value"
              [placeholder]="searchPlaceholder()"
            />
          </label>
          <button class="btn-primary" type="submit" [disabled]="searching()">Buscar</button>
          <button class="btn-secondary back-btn" type="button" (click)="onBack()">← Volver</button>
        </form>
        <p class="muted" *ngIf="searchMessage() && !searching()">{{ searchMessage() }}</p>
      </article>

      <article class="card" style="padding: 0.8rem;" *ngIf="searching()">
        <p style="margin: 0;">Buscando clientes...</p>
      </article>

      <section class="page-grid" *ngIf="searchResults().length > 0">
        <h2 class="section-title">Resultados de busqueda</h2>
        <article class="card result-card" *ngFor="let cliente of searchResults()">
          <div class="result-info">
            <strong>{{ cliente.nombre }}</strong>
            <p class="muted">{{ cliente.email }}</p>
            <p class="muted">Telefono: {{ cliente.telefono }}</p>
          </div>
          <button class="btn-secondary" type="button" (click)="onSelectCliente(cliente)">Ver historial</button>
        </article>
      </section>

      <article class="card" style="padding: 0.8rem;" *ngIf="historyLoading()">
        <p style="margin: 0;">Cargando historial...</p>
      </article>

      <article class="card error-box" *ngIf="historyError() && !historyLoading()">
        <p>{{ historyError() }}</p>
      </article>

      <section class="page-grid" *ngIf="historial() as history">
        <article class="card summary-card">
          <div class="section-head">
            <h3>Resumen del cliente</h3>
            <span class="badge">ID {{ history.cliente.clienteId }}</span>
          </div>
          <div class="kv-grid">
            <p><strong>Nombre:</strong> {{ history.cliente.nombre }}</p>
            <p><strong>Correo:</strong> {{ history.cliente.email }}</p>
            <p><strong>Telefono:</strong> {{ history.cliente.telefono }}</p>
            <p><strong>Cliente desde:</strong> {{ formatDate(history.resumen.clienteDesde) }}</p>
          </div>
        </article>

        <article class="card highlight-box" *ngIf="history.mensajeCumpleanos">
          <p>{{ history.mensajeCumpleanos }}</p>
        </article>

        <article class="card warning-box" *ngIf="history.mensajeInactivo">
          <div class="warning-main">
            <p>{{ history.mensajeInactivo }}</p>
            <button
              class="btn-secondary"
              type="button"
              [disabled]="recordatorioSending() || !history.mostrarRecordatorio"
              (click)="onEnviarRecordatorio()"
            >
              {{ recordatorioSending() ? 'Enviando...' : 'Enviar recordatorio' }}
            </button>
          </div>
          <p class="muted" *ngIf="recordatorioMessage()">{{ recordatorioMessage() }}</p>
        </article>

        <article class="card stats-card">
          <h3>Resumen de compras</h3>
          <div class="kv-grid">
            <p><strong>Total visitas:</strong> {{ history.resumen.totalVisitas }}</p>
            <p><strong>Total gastado:</strong> {{ history.resumen.totalGastado | currency:'COP':'symbol':'1.0-0' }}</p>
            <p><strong>Promedio por visita:</strong> {{ history.resumen.promedioPorVisita | currency:'COP':'symbol':'1.0-0' }}</p>
            <p><strong>Ultima visita:</strong> {{ formatDate(history.resumen.ultimaVisita) }}</p>
          </div>
        </article>

        <article class="card section-box">
          <div class="section-head">
            <h3>Historial de ventas</h3>
            <label class="group-filter">
              <span>Agrupar por</span>
              <select class="input-field" [value]="groupMode()" (change)="onGroupModeChange($any($event.target).value)">
                <option value="none">Sin agrupar</option>
                <option value="anio">Anio</option>
                <option value="mes">Mes</option>
              </select>
            </label>
          </div>

          <article class="card" style="padding: 0.7rem;" *ngIf="groupLoading()">
            <p style="margin: 0;">Cargando agrupacion...</p>
          </article>

          <ng-container *ngIf="groupMode() === 'none'">
            <article class="card sale-card" *ngFor="let venta of history.ventas">
              <div>
                <p><strong>Fecha:</strong> {{ formatDateTime(venta.fechaHora) }}</p>
                <p><strong>Metodo:</strong> {{ formatMetodoPago(venta.metodo) }}</p>
              </div>
              <div class="sale-total">
                <strong>{{ venta.total | currency:'COP':'symbol':'1.0-0' }}</strong>
              </div>
            </article>

            <article class="card empty-state-box" *ngIf="history.ventas.length === 0">
              <p class="empty-state">{{ historyMessage() || 'Este cliente no tiene historial de compras' }}</p>
            </article>
          </ng-container>

          <ng-container *ngIf="groupMode() === 'anio'">
            <article class="card sale-card" *ngFor="let item of ventasAgrupadasAnio()">
              <div>
                <p><strong>Anio:</strong> {{ item.anio }}</p>
                <p><strong>Cantidad:</strong> {{ item.cantidad }}</p>
              </div>
              <div class="sale-total">
                <strong>{{ item.total | currency:'COP':'symbol':'1.0-0' }}</strong>
              </div>
            </article>
            <article class="card empty-state-box" *ngIf="ventasAgrupadasAnio().length === 0 && !groupLoading()">
              <p class="empty-state">No hay ventas para agrupar.</p>
            </article>
          </ng-container>

          <ng-container *ngIf="groupMode() === 'mes'">
            <article class="card sale-card" *ngFor="let item of ventasAgrupadasMes()">
              <div>
                <p><strong>Periodo:</strong> {{ formatMonth(item.mes) }}/{{ item.anio }}</p>
                <p><strong>Cantidad:</strong> {{ item.cantidad }}</p>
              </div>
              <div class="sale-total">
                <strong>{{ item.total | currency:'COP':'symbol':'1.0-0' }}</strong>
              </div>
            </article>
            <article class="card empty-state-box" *ngIf="ventasAgrupadasMes().length === 0 && !groupLoading()">
              <p class="empty-state">No hay ventas para agrupar.</p>
            </article>
          </ng-container>
        </article>
      </section>
    </section>
  `,
  styles: [
    `
      h1 {
        margin: 0 0 0.3rem 0;
        font-size: 1.5rem;
        font-weight: 700;
        color: #4d3323;
      }

      .back-btn {
        padding: 0.42rem 0.62rem;
        font-size: 0.78rem;
        border-radius: 8px;
      }

      @media (max-width: 768px) {
        h1 {
          font-size: 1.2rem;
        }

        .back-btn {
          width: 100%;
          padding: 0.5rem 0.8rem;
        }
      }

      .search-card {
        padding: 1rem;
        max-width: 900px;
        display: grid;
        gap: 0.6rem;
      }

      .result-card {
        padding: 0.8rem;
        display: flex;
        justify-content: space-between;
        gap: 0.6rem;
        align-items: center;
        flex-wrap: wrap;
      }

      .result-info {
        display: grid;
        gap: 0.2rem;
      }

      .summary-card {
        padding: 0.9rem;
        display: grid;
        gap: 0.6rem;
      }

      .stats-card {
        padding: 0.85rem;
        display: grid;
        gap: 0.55rem;
      }

      .highlight-box {
        border: 1px solid rgba(111, 78, 55, 0.4);
        background: rgba(111, 78, 55, 0.08);
        padding: 0.75rem;
      }

      .warning-box {
        border: 1px solid rgba(196, 30, 58, 0.45);
        background: rgba(196, 30, 58, 0.08);
        padding: 0.75rem;
        display: grid;
        gap: 0.4rem;
      }

      .warning-main {
        display: flex;
        flex-wrap: wrap;
        gap: 0.6rem;
        align-items: center;
        justify-content: space-between;
      }

      .section-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.6rem;
        flex-wrap: wrap;
      }

      .section-box {
        padding: 0.85rem;
        display: grid;
        gap: 0.6rem;
      }

      .kv-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
        gap: 0.35rem 0.8rem;
      }

      .kv-grid p {
        margin: 0;
        font-size: 0.84rem;
      }

      .sale-card {
        padding: 0.75rem;
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 0.6rem;
        align-items: center;
      }

      .sale-card p {
        margin: 0;
        font-size: 0.84rem;
      }

      .sale-total {
        font-size: 0.9rem;
      }

      .badge {
        border: 1px solid rgba(111, 78, 55, 0.45);
        border-radius: 999px;
        padding: 0.2rem 0.6rem;
        font-size: 0.76rem;
        font-weight: 700;
        color: #4d3323;
      }

      .group-filter {
        display: flex;
        align-items: center;
        gap: 0.35rem;
      }

      .empty-state-box {
        padding: 0.75rem 0.85rem;
        display: grid;
        gap: 0.5rem;
      }

      .empty-state {
        margin: 0;
        color: var(--muted);
        font-size: 0.86rem;
      }

      .muted {
        color: var(--muted);
        opacity: 0.7;
        margin: 0;
        font-size: 0.8rem;
      }
    `
  ]
})
export class ClienteHistorialPageComponent implements OnInit {
  readonly searching = signal(false);
  readonly searchMessage = signal('');
  readonly searchResults = signal<ClienteBusqueda[]>([]);

  readonly historyLoading = signal(false);
  readonly historyError = signal('');
  readonly historyMessage = signal('');
  readonly historial = signal<ClienteVentasHistory | null>(null);
  readonly selectedClienteId = signal<string | null>(null);

  readonly groupMode = signal<GroupMode>('none');
  readonly groupLoading = signal(false);
  readonly ventasAgrupadasAnio = signal<VentaAgrupadaAnio[]>([]);
  readonly ventasAgrupadasMes = signal<VentaAgrupadaMes[]>([]);

  readonly recordatorioSending = signal(false);
  readonly recordatorioMessage = signal('');

  readonly searchModes: Array<{ value: ClienteSearchMode; label: string }> = [
    { value: 'nombre', label: 'Nombre' },
    { value: 'correo', label: 'Correo' },
    { value: 'telefono', label: 'Telefono' },
  ];

  readonly searchForm = this.formBuilder.nonNullable.group({
    mode: ['correo' as ClienteSearchMode, [Validators.required]],
    value: ['', [Validators.required]],
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly clientesAdminService: ClienteVentasAdminService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const clienteId = this.route.snapshot.queryParamMap.get('clienteId');
    if (clienteId) {
      this.selectedClienteId.set(clienteId);
      this.loadHistorial(clienteId);
    }
  }

  onBack(): void {
    this.router.navigate(['/app/admin/dashboard']);
  }

  searchPlaceholder(): string {
    const mode = this.searchForm.getRawValue().mode;
    if (mode === 'nombre') {
      return 'Ej: Juan Perez';
    }
    if (mode === 'telefono') {
      return 'Ej: 3001234567';
    }
    return 'cliente@correo.com';
  }

  onSearch(): void {
    if (this.searchForm.invalid) {
      this.searchForm.markAllAsTouched();
      return;
    }

    const { mode, value } = this.searchForm.getRawValue();
    const trimmedValue = value.trim();
    if (!trimmedValue) {
      this.searchMessage.set('Ingresa un valor para buscar.');
      return;
    }

    this.searching.set(true);
    this.searchMessage.set('');
    this.searchResults.set([]);

    this.clientesAdminService.buscarClientes(mode, trimmedValue).subscribe({
      next: (result) => {
        this.searchResults.set(result.results);
        this.searchMessage.set(
          result.message ?? (result.results.length === 0 ? 'No se encontraron clientes' : '')
        );
        this.searching.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const backendMessage =
          (typeof err.error?.message === 'string' && err.error.message.trim().length > 0
            ? err.error.message
            : '') ||
          (typeof err.error === 'string' && err.error.trim().length > 0 ? err.error : '');

        this.searchMessage.set(backendMessage || 'No fue posible buscar clientes.');
        this.searching.set(false);
      }
    });
  }

  onSelectCliente(cliente: ClienteBusqueda): void {
    this.selectedClienteId.set(cliente.clienteId);
    this.router.navigate([], {
      queryParams: { clienteId: cliente.clienteId },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
    this.loadHistorial(cliente.clienteId);
  }

  onGroupModeChange(mode: GroupMode): void {
    this.groupMode.set(mode);
    this.refreshAgrupado();
  }

  onEnviarRecordatorio(): void {
    const clienteId = this.selectedClienteId();
    if (!clienteId || this.recordatorioSending()) {
      return;
    }

    this.recordatorioSending.set(true);
    this.recordatorioMessage.set('');

    this.clientesAdminService.enviarRecordatorio(clienteId).subscribe({
      next: (message) => {
        this.recordatorioMessage.set(message);
        this.recordatorioSending.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const backendMessage =
          (typeof err.error?.message === 'string' && err.error.message.trim().length > 0
            ? err.error.message
            : '') ||
          (typeof err.error === 'string' && err.error.trim().length > 0 ? err.error : '');

        this.recordatorioMessage.set(backendMessage || 'No fue posible enviar el recordatorio.');
        this.recordatorioSending.set(false);
      }
    });
  }

  formatDateTime(date: Date): string {
    return date.toLocaleString('es-CO', {
      dateStyle: 'short',
      timeStyle: 'short',
    });
  }

  formatDate(date: Date | null | undefined): string {
    if (!date) {
      return 'Sin registro';
    }
    return date.toLocaleDateString('es-CO', {
      dateStyle: 'short',
    });
  }

  formatMetodoPago(method?: string | null): string {
    const normalized = String(method ?? '').toUpperCase();
    if (normalized === 'EFECTIVO') {
      return 'Efectivo';
    }
    if (normalized === 'TARJETA') {
      return 'Tarjeta';
    }
    if (normalized === 'TRANSFERENCIA') {
      return 'Transferencia';
    }
    if (normalized === 'OTRO') {
      return 'Otro';
    }
    return method ?? 'Sin especificar';
  }

  formatMonth(value: number): string {
    return value < 10 ? `0${value}` : String(value);
  }

  private loadHistorial(clienteId: string): void {
    this.historyLoading.set(true);
    this.historyError.set('');
    this.historyMessage.set('');
    this.historial.set(null);
    this.ventasAgrupadasAnio.set([]);
    this.ventasAgrupadasMes.set([]);
    this.recordatorioMessage.set('');

    this.clientesAdminService.obtenerHistorial(clienteId).subscribe({
      next: (result) => {
        this.historial.set(result.data);
        this.historyMessage.set(result.message ?? '');
        this.historyLoading.set(false);
        this.refreshAgrupado();
      },
      error: (err: HttpErrorResponse) => {
        const backendMessage =
          (typeof err.error?.message === 'string' && err.error.message.trim().length > 0
            ? err.error.message
            : '') ||
          (typeof err.error === 'string' && err.error.trim().length > 0 ? err.error : '');

        this.historyError.set(backendMessage || 'No fue posible cargar el historial.');
        this.historyLoading.set(false);
      }
    });
  }

  private refreshAgrupado(): void {
    const mode = this.groupMode();
    if (mode === 'none') {
      return;
    }

    const clienteId = this.selectedClienteId();
    if (!clienteId) {
      return;
    }

    this.groupLoading.set(true);

    const request$ = mode === 'anio'
      ? this.clientesAdminService.obtenerAgrupadoPorAnio(clienteId)
      : this.clientesAdminService.obtenerAgrupadoPorMes(clienteId);

    request$.subscribe({
      next: (result) => {
        if (mode === 'anio') {
          this.ventasAgrupadasAnio.set(result as VentaAgrupadaAnio[]);
        } else {
          this.ventasAgrupadasMes.set(result as VentaAgrupadaMes[]);
        }
        this.groupLoading.set(false);
      },
      error: () => {
        this.groupLoading.set(false);
      }
    });
  }
}
