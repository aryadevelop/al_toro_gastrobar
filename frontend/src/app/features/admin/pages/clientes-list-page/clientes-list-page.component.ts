import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import {
  ClienteListado,
  ClienteVentasAdminService,
  EstadoClienteFiltro,
} from '../../../../core/services/cliente-ventas-admin.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-clientes-list-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, RouterLink],
  template: `
    <section class="page-grid">
      <app-page-header title="Clientes" subtitle="Consulta y segmentación de clientes"></app-page-header>

      <form [formGroup]="filtersForm" (ngSubmit)="applyFilters()">
        <article class="card filter-card">
          <div class="filters-grid">
          <label>
            <span>Buscar por nombre</span>
            <input class="input-field" type="text" formControlName="nombre" placeholder="Ej: Juan Pérez" />
          </label>

          <label>
            <span>Buscar por correo</span>
            <input class="input-field" type="text" formControlName="correo" placeholder="Ej: correo@dominio.com" />
          </label>

          <label>
            <span>Visitas mínimas</span>
            <input class="input-field" type="number" min="0" formControlName="minVisitas" />
          </label>

          <label>
            <span>Visitas máximas</span>
            <input class="input-field" type="number" min="0" formControlName="maxVisitas" />
          </label>

          <label>
            <span>Registro desde</span>
            <input class="input-field" type="date" formControlName="desdeRegistro" />
          </label>

          <label>
            <span>Registro hasta</span>
            <input class="input-field" type="date" formControlName="hastaRegistro" />
          </label>

          <label>
            <span>Estado</span>
            <select class="input-field" formControlName="estado" (change)="applyFilters()">
              <option value="">Todos</option>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
            </select>
          </label>

          <label>
            <span>Filtro rápido</span>
            <select class="input-field" formControlName="filtroRapido" (change)="applyFilters()">
              <option value="">Ninguno</option>
              <option value="INACTIVOS_6M">Clientes inactivos (6 meses)</option>
              <option value="CUMPLEANOS_HOY">Cumpleaños hoy</option>
            </select>
          </label>

          </div>
        </article>

        <div class="actions-row" style="margin-top: 0.8rem;">
          <button class="btn-secondary" type="button" (click)="clearFilters()">Limpiar</button>
          <button class="btn-primary" type="submit">Buscar</button>
        </div>
      </form>

      <article class="card state-card" *ngIf="loading()">
        <p>Cargando clientes...</p>
      </article>

      <article class="card state-card" *ngIf="errorMessage() && !loading()">
        <p>{{ errorMessage() }}</p>
      </article>

      <article class="card state-card" *ngIf="showFirstTimeEmptyState() && !loading() && !errorMessage()">
        <p>No hay clientes registrados. Los clientes se crean automáticamente al realizar su primera reserva</p>
      </article>

      <article class="card state-card" *ngIf="showFilteredEmptyState() && !loading() && !errorMessage()">
        <p>{{ emptyResultsMessage() || 'No se encontraron clientes' }}</p>
      </article>

      <article class="card table-card" *ngIf="clientes().length > 0 && !loading()">
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Nombres y apellidos</th>
                <th>Correo electrónico</th>
                <th>Teléfono</th>
                <th>Total visitas</th>
                <th>Total gastado</th>
                <th>Puntos acumulados</th>
                <th>Estado</th>
                <th>Cliente frecuente</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let cliente of clientes()">
                <td data-label="Nombres y apellidos">{{ cliente.nombre }}</td>
                <td data-label="Correo electrónico">{{ cliente.correoElectronico }}</td>
                <td data-label="Teléfono">{{ cliente.telefono }}</td>
                <td data-label="Total visitas">{{ cliente.totalVisitas }}</td>
                <td data-label="Total gastado">{{ formatMoney(cliente.totalGastado) }}</td>
                <td data-label="Puntos acumulados">{{ cliente.puntosAcumulados }}</td>
                <td data-label="Estado">
                  <span class="badge" [class.badge-active]="isEstadoActivo(cliente.estado)">
                    {{ formatEstado(cliente.estado) }}
                  </span>
                </td>
                <td data-label="Cliente frecuente">
                  <span class="badge" [class.badge-frequent]="cliente.clienteFrecuente">
                    {{ cliente.clienteFrecuente ? 'Frecuente' : 'No' }}
                  </span>
                </td>
                <td data-label="Detalle">
                  <a class="btn-secondary action-btn" [routerLink]="['/app/admin/cliente-historial']" [queryParams]="{ clienteId: cliente.clienteId }">
                    Ver historial
                  </a>
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

      .table-card {
        padding: 0.8rem;
      }

      .table-wrapper {
        overflow: auto;
      }

      table {
        width: 100%;
        border-collapse: collapse;
        min-width: 100%;
      }

      th,
      td {
        text-align: left;
        padding: 0.4rem 0.5rem;
        font-size: 0.82rem;
      }

      td {
        border-bottom: 1px solid rgba(255, 255, 255, 0.15);
        border-right: 1px solid rgba(255, 255, 255, 0.15);
      }

      th {
        border-right: 1px solid rgba(181, 18, 18, 0.15);
      }

      th:last-child,
      td:last-child {
        border-right: none;
      }

      th {
        font-size: 0.8rem;
        background: var(--bg);
        color: var(--primary);
        font-weight: 700;
        text-transform: uppercase;
      }

      th:first-child {
        border-top-left-radius: 8px;
        border-bottom-left-radius: 8px;
      }

      th:last-child {
        border-top-right-radius: 8px;
        border-bottom-right-radius: 8px;
      }

      .badge {
        background: #ffffff;
        border: 1px solid rgba(196, 30, 58, 0.45);
        color: #9f1239;
        border-radius: 999px;
        padding: 0.15rem 0.5rem;
        font-size: 0.74rem;
        font-weight: 600;
      }

      .badge-active {
        border-color: rgba(17, 122, 59, 0.45);
        color: #137333;
      }

      .badge-frequent {
        border-color: rgba(133, 77, 14, 0.45);
        color: #92400e;
      }

      .action-btn {
        padding: 0.35rem 0.55rem;
        font-size: 0.76rem;
        display: inline-block;
        white-space: nowrap;
        text-align: center;
      }

      @media (max-width: 1024px) {
        table {
          min-width: 100%;
        }
        thead {
          display: none;
        }
        tr {
          display: block;
          margin-bottom: 0.8rem;
          border: 1px solid rgba(10, 10, 10, 0.1);
          border-radius: 8px;
          padding: 0.4rem;
          background: #fffaf5;
          color: var(--text);
        }
        td {
          display: flex;
          justify-content: space-between;
          align-items: center;
          text-align: right;
          border-bottom: 1px solid rgba(10, 10, 10, 0.05);
          padding: 0.35rem 0;
        }
        td:last-child {
          border-bottom: 0;
        }
        td::before {
          content: attr(data-label);
          font-weight: 600;
          color: var(--muted);
          text-align: left;
          margin-right: 1rem;
        }
      }
    `,
  ]
})
export class ClientesListPageComponent implements OnInit {
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly emptyResultsMessage = signal('');
  readonly clientes = signal<ClienteListado[]>([]);

  readonly filtersForm = this.formBuilder.nonNullable.group({
    nombre: [''],
    correo: [''],
    minVisitas: [''],
    maxVisitas: [''],
    desdeRegistro: [''],
    hastaRegistro: [''],
    estado: ['' as EstadoClienteFiltro | ''],
    filtroRapido: ['' as '' | 'INACTIVOS_6M' | 'CUMPLEANOS_HOY'],
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly clientesAdminService: ClienteVentasAdminService
  ) {}

  ngOnInit(): void {
    this.loadClientes();
  }

  applyFilters(): void {
    this.loadClientes();
  }

  clearFilters(): void {
    this.filtersForm.setValue({
      nombre: '',
      correo: '',
      minVisitas: '',
      maxVisitas: '',
      desdeRegistro: '',
      hastaRegistro: '',
      estado: '',
      filtroRapido: '',
    });
    this.loadClientes();
  }

  showFirstTimeEmptyState(): boolean {
    return this.clientes().length === 0 && !this.hasActiveFilters();
  }

  showFilteredEmptyState(): boolean {
    return this.clientes().length === 0 && this.hasActiveFilters();
  }

  isEstadoActivo(estado: string): boolean {
    return estado.trim().toLowerCase() === 'activo';
  }

  formatEstado(estado: string): string {
    return this.isEstadoActivo(estado) ? 'Activo' : 'Inactivo';
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0,
    }).format(value ?? 0);
  }

  private loadClientes(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.emptyResultsMessage.set('');

    const filters = this.filtersForm.getRawValue();
    const filtroRapido = filters.filtroRapido;

    this.clientesAdminService
      .listarClientes({
        nombre: filters.nombre,
        correo: filters.correo,
        minVisitas: this.toOptionalNumber(filters.minVisitas),
        maxVisitas: this.toOptionalNumber(filters.maxVisitas),
        desdeRegistro: filters.desdeRegistro || null,
        hastaRegistro: filters.hastaRegistro || null,
        estado: filters.estado,
        cumpleanosHoy: filtroRapido === 'CUMPLEANOS_HOY',
        reservasUltimosMeses: filtroRapido === 'INACTIVOS_6M' ? 6 : null,
      })
      .subscribe({
        next: (result) => {
          this.clientes.set(result.results);
          this.emptyResultsMessage.set(result.message ?? '');
          this.loading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          const backendMessage =
            (typeof error.error?.message === 'string' && error.error.message.trim().length > 0
              ? error.error.message
              : '') ||
            (typeof error.error === 'string' && error.error.trim().length > 0 ? error.error : '');

          this.errorMessage.set(backendMessage || 'No fue posible cargar el listado de clientes.');
          this.loading.set(false);
        },
      });
  }

  private hasActiveFilters(): boolean {
    const { nombre, correo, minVisitas, maxVisitas, desdeRegistro, hastaRegistro, estado, filtroRapido } =
      this.filtersForm.getRawValue();

    return Boolean(
      nombre.trim() ||
        correo.trim() ||
        minVisitas.toString().trim() ||
        maxVisitas.toString().trim() ||
        desdeRegistro ||
        hastaRegistro ||
        estado ||
        filtroRapido
    );
  }

  private toOptionalNumber(rawValue: string): number | null {
    const trimmed = rawValue?.toString().trim();
    if (!trimmed) {
      return null;
    }
    const parsed = Number(trimmed);
    return Number.isNaN(parsed) ? null : parsed;
  }
}
