import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  EmpleadoListado,
  EstadoEmpleadoFiltro,
  PersonalAdminService,
  RolEmpleadoFiltro,
} from '../../../../core/services/personal-admin.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-personal-list-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PageHeaderComponent, ConfirmDialogComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Personal" subtitle="Consulta y gestión del personal del sistema"></app-page-header>

      <article class="card filter-card">
        <form class="filters-grid" [formGroup]="filtersForm" (ngSubmit)="applyFilters()">
          <label>
            <span>Rol</span>
            <select class="input-field" formControlName="rol" (change)="applyFilters()">
              <option value="">Todos</option>
              <option value="MESERO">Mesero</option>
              <option value="CAJERO">Cajero</option>
              <option value="BARTENDER">Bartender</option>
            </select>
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
            <span>Buscar por nombre</span>
            <input class="input-field" type="text" formControlName="nombre" placeholder="Ej: Juan Pérez" />
          </label>

          <div class="actions-row">
            <button class="btn-secondary" type="button" (click)="clearFilters()">Limpiar</button>
            <button class="btn-primary" type="submit">Buscar</button>
          </div>
        </form>
      </article>

      <article class="card state-card" *ngIf="loading()">
        <p>Cargando personal...</p>
      </article>

      <article class="card state-card" *ngIf="errorMessage() && !loading()">
        <p>{{ errorMessage() }}</p>
      </article>

      <article class="card state-card" *ngIf="successMessage() && !loading()">
        <p class="success-message">{{ successMessage() }}</p>
      </article>

      <article class="card state-card" *ngIf="showFirstTimeEmptyState() && !loading() && !errorMessage()">
        <p>No hay empleados registrados. Comienza creando el primer empleado</p>
        <a class="btn-primary" routerLink="/app/admin/personal/new">Nuevo empleado</a>
      </article>

      <article class="card state-card" *ngIf="showFilteredEmptyState() && !loading() && !errorMessage()">
        <p>No se encontraron empleados con los filtros aplicados.</p>
      </article>

      <article class="card table-card" *ngIf="empleados().length > 0 && !loading()">
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Nombres</th>
                <th>Rol</th>
                <th>Correo electrónico</th>
                <th>Teléfono</th>
                <th>Estado</th>
                <th>Fecha de ingreso</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let empleado of empleados()">
                <td>{{ empleado.nombre }}</td>
                <td>{{ formatRoles(empleado.roles) }}</td>
                <td>{{ empleado.correoElectronico }}</td>
                <td>{{ empleado.telefono }}</td>
                <td>
                  <span class="badge" [class.badge-active]="isEstadoActivo(empleado.estado)">
                    {{ formatEstado(empleado.estado) }}
                  </span>
                </td>
                <td>{{ formatDate(empleado.fechaIngreso) }}</td>
                <td>
                  <button
                    class="btn-secondary action-btn"
                    type="button"
                    [disabled]="changingState()"
                    (click)="onRequestEstadoChange(empleado)"
                  >
                    {{ isEstadoActivo(empleado.estado) ? 'Cambiar estado' : 'Activar' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <app-confirm-dialog
        [open]="showEstadoConfirmDialog()"
        title="Confirmar cambio de estado"
        [message]="estadoConfirmMessage()"
        cancelLabel="Cancelar"
        [confirmLabel]="changingState() ? 'Procesando...' : 'Confirmar'"
        (cancel)="cancelEstadoChange()"
        (confirm)="confirmEstadoChange()"
      ></app-confirm-dialog>
    </section>
  `,
  styles: [
    `
      .filter-card {
        padding: 0.9rem;
      }

      .filters-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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
        display: grid;
        gap: 0.55rem;
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
        min-width: 820px;
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

      .badge {
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

      .action-btn {
        padding: 0.35rem 0.55rem;
        font-size: 0.76rem;
      }

      .success-message {
        color: #137333;
      }
    `,
  ]
})
export class PersonalListPageComponent implements OnInit {
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly empleados = signal<EmpleadoListado[]>([]);
  readonly showEstadoConfirmDialog = signal(false);
  readonly changingState = signal(false);
  readonly estadoConfirmMessage = signal('');

  private empleadoPendingEstadoChange: EmpleadoListado | null = null;

  readonly filtersForm = this.formBuilder.nonNullable.group({
    rol: ['' as RolEmpleadoFiltro | ''],
    estado: ['' as EstadoEmpleadoFiltro | ''],
    nombre: [''],
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly personalAdminService: PersonalAdminService
  ) {}

  ngOnInit(): void {
    this.loadEmpleados();
  }

  applyFilters(): void {
    this.successMessage.set('');
    this.loadEmpleados();
  }

  clearFilters(): void {
    this.filtersForm.setValue({
      rol: '',
      estado: '',
      nombre: '',
    });
    this.successMessage.set('');
    this.loadEmpleados();
  }

  onRequestEstadoChange(empleado: EmpleadoListado): void {
    const isActive = this.isEstadoActivo(empleado.estado);
    this.empleadoPendingEstadoChange = empleado;

    this.estadoConfirmMessage.set(
      isActive
        ? `¿Estás seguro de deshabilitar a ${empleado.nombre}? No podrá acceder al sistema`
        : `¿Estás seguro de activar a ${empleado.nombre}? Recuperará el acceso al sistema con sus permisos anteriores`
    );
    this.showEstadoConfirmDialog.set(true);
  }

  cancelEstadoChange(): void {
    if (this.changingState()) {
      return;
    }

    this.showEstadoConfirmDialog.set(false);
    this.empleadoPendingEstadoChange = null;
    this.estadoConfirmMessage.set('');
  }

  confirmEstadoChange(): void {
    const empleado = this.empleadoPendingEstadoChange;
    if (!empleado || this.changingState()) {
      return;
    }

    this.changingState.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const nextEstado: EstadoEmpleadoFiltro = this.isEstadoActivo(empleado.estado) ? 'INACTIVO' : 'ACTIVO';

    this.personalAdminService.cambiarEstadoEmpleado(empleado.empleadoId, nextEstado).subscribe({
      next: (message) => {
        this.changingState.set(false);
        this.showEstadoConfirmDialog.set(false);
        this.empleadoPendingEstadoChange = null;
        this.estadoConfirmMessage.set('');

        this.successMessage.set(message || (nextEstado === 'INACTIVO' ? 'Empleado deshabilitado correctamente' : 'Empleado habilitado correctamente'));
        this.loadEmpleados();
      },
      error: (error: HttpErrorResponse) => {
        const backendMessage =
          (typeof error.error?.message === 'string' && error.error.message.trim().length > 0
            ? error.error.message
            : '') ||
          (typeof error.error === 'string' && error.error.trim().length > 0 ? error.error : '');

        this.changingState.set(false);
        this.errorMessage.set(backendMessage || 'No fue posible cambiar el estado del empleado.');
      },
    });
  }

  showFirstTimeEmptyState(): boolean {
    return this.empleados().length === 0 && !this.hasActiveFilters();
  }

  showFilteredEmptyState(): boolean {
    return this.empleados().length === 0 && this.hasActiveFilters();
  }

  formatRoles(roles: string[]): string {
    if (!roles.length) {
      return 'Sin rol';
    }

    const labels = roles.map((role) => {
      const normalized = role.toUpperCase();
      if (normalized === 'MESERO') {
        return 'Mesero';
      }
      if (normalized === 'CAJERO') {
        return 'Cajero';
      }
      if (normalized === 'BARTENDER') {
        return 'Bartender';
      }
      if (normalized === 'COCINERO') {
        return 'Cocinero';
      }
      if (normalized === 'ADMIN') {
        return 'Administrador';
      }
      return role;
    });

    return labels.join(', ');
  }

  formatEstado(estado: string): string {
    return this.isEstadoActivo(estado) ? 'Activo' : 'Inactivo';
  }

  isEstadoActivo(estado: string): boolean {
    return estado.trim().toLowerCase() === 'activo';
  }

  formatDate(date: Date | null): string {
    if (!date) {
      return 'No registrada';
    }
    return date.toLocaleDateString('es-CO', { dateStyle: 'short' });
  }

  private loadEmpleados(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    const filters = this.filtersForm.getRawValue();

    this.personalAdminService
      .listarEmpleados({
        rol: filters.rol,
        estado: filters.estado,
        nombre: filters.nombre,
      })
      .subscribe({
        next: (result) => {
          this.empleados.set(result);
          this.loading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          const backendMessage =
            (typeof error.error?.message === 'string' && error.error.message.trim().length > 0
              ? error.error.message
              : '') ||
            (typeof error.error === 'string' && error.error.trim().length > 0 ? error.error : '');

          this.errorMessage.set(backendMessage || 'No fue posible cargar el listado de personal.');
          this.loading.set(false);
        },
      });
  }

  private hasActiveFilters(): boolean {
    const { rol, estado, nombre } = this.filtersForm.getRawValue();
    return Boolean(rol || estado || nombre.trim());
  }
}
