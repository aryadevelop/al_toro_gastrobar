import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { DecoracionAdminService } from '../../../../core/services/decoracion-admin.service';
import { BackendDecoracionAdminResponse } from '../../../../core/models/api.models';
import { DecoracionFormModalComponent } from '../../components/decoracion-form-modal/decoracion-form-modal.component';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-decoraciones-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent, DecoracionFormModalComponent, ConfirmDialogComponent],
  template: `
    <section class="page-grid">
      <app-page-header 
        title="Gestión de Decoraciones" 
        subtitle="Administra las decoraciones y sus imágenes para las reservas">
      </app-page-header>
      
      <div class="actions-bar">
        <button class="btn-primary" (click)="abrirModalNueva()">Añadir decoración</button>
      </div>

      <div class="card p-3">
        <div class="table-responsive" *ngIf="decoraciones().length > 0; else noData">
          <table class="table">
            <thead>
              <tr>
                <th>Imagen</th>
                <th>Nombre</th>
                <th>Costo Adicional</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let dec of decoraciones()">
                <td data-label="Imagen">
                  <img *ngIf="dec.decoracionImagenUrl" [src]="getImageUrl(dec.decoracionImagenUrl)" alt="Decoración" class="thumb-img">
                  <span *ngIf="!dec.decoracionImagenUrl" class="no-img">Sin imagen</span>
                </td>
                <td data-label="Nombre">{{ dec.decoracionNombre }}</td>
                <td data-label="Costo Adicional">{{ dec.decoracionCostoAdicional !== null ? (dec.decoracionCostoAdicional | currency:'COP') : 'Gratis' }}</td>
                <td data-label="Estado">
                  <span class="badge" [class.badge-success]="dec.decoracionEstado === 'ACTIVO'" [class.badge-danger]="dec.decoracionEstado !== 'ACTIVO'">
                    {{ dec.decoracionEstado }}
                  </span>
                </td>
                <td data-label="Acciones">
                  <div class="btn-group">
                    <button class="btn-secondary btn-sm" (click)="abrirModalEditar(dec)">Editar / Imagen</button>
                    <button class="btn-warning btn-sm" (click)="cambiarEstado(dec)">Cambiar Estado</button>
                    <button class="btn-danger btn-sm" (click)="eliminar(dec)">Eliminar</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <ng-template #noData>
          <div class="empty-state">
            <p>No hay decoraciones configuradas en el sistema.</p>
          </div>
        </ng-template>
      </div>
    </section>

    <app-decoracion-form-modal
      [isOpen]="modalAbierto()"
      [decoracionToEdit]="decoracionEditando()"
      (close)="cerrarModal()"
      (saved)="cargarDecoraciones()">
    </app-decoracion-form-modal>

    <app-confirm-dialog
      [open]="dialogAbierto()"
      [title]="dialogTitulo()"
      [message]="dialogMensaje()"
      (cancel)="dialogAbierto.set(false)"
      (confirm)="ejecutarAccionConfirmada()">
    </app-confirm-dialog>
  `,
  styles: [`
    .actions-bar {
      margin-bottom: 1rem;
      display: flex;
      justify-content: flex-end;
    }
    .thumb-img {
      width: 60px;
      height: 60px;
      object-fit: cover;
      border-radius: var(--radius);
    }
    .no-img {
      color: var(--text-muted);
      font-size: 0.85rem;
      font-style: italic;
    }
    .btn-group {
      display: flex;
      gap: 0.5rem;
    }
    .btn-sm {
      padding: 0.25rem 0.5rem;
      font-size: 0.85rem;
    }
    .badge {
      padding: 0.25rem 0.5rem;
      border-radius: 12px;
      font-size: 0.85rem;
      font-weight: 600;
    }
    .badge-success {
      background-color: var(--success-color);
      color: white;
    }
    .badge-danger {
      background-color: var(--error-color);
      color: white;
    }
    .empty-state {
      text-align: center;
      padding: 2rem;
      color: var(--text-muted);
    }
    table {
      width: 100%;
      border-collapse: collapse;
    }
    th, td {
      text-align: left;
      padding: 0.6rem;
      border-bottom: 1px solid rgba(10,10,10,0.1);
    }
    @media (max-width: 768px) {
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
        padding: 0.5rem 0;
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
      .btn-group {
        flex-direction: column;
        align-items: flex-end;
      }
    }
  `]
})
export class DecoracionesPageComponent implements OnInit {
  private readonly decoracionService = inject(DecoracionAdminService);

  readonly decoraciones = signal<BackendDecoracionAdminResponse[]>([]);
  readonly modalAbierto = signal(false);
  readonly decoracionEditando = signal<BackendDecoracionAdminResponse | null>(null);

  readonly dialogAbierto = signal(false);
  readonly dialogTitulo = signal('');
  readonly dialogMensaje = signal('');
  private accionConfirmacion: (() => void) | null = null;

  ngOnInit(): void {
    this.cargarDecoraciones();
  }

  getImageUrl(path: string | null): string {
    if (!path) return '';
    if (path.startsWith('http')) return path;
    const base = environment.apiBaseUrl.replace(/\/api\/?$/, '');
    const cleanPath = path.startsWith('/') ? path : '/' + path;
    return `${base}${cleanPath}`;
  }

  cargarDecoraciones(): void {
    this.decoracionService.listarDecoraciones().subscribe({
      next: (res) => {
        if (res.data) {
          this.decoraciones.set(res.data);
        }
      },
      error: (err) => console.error('Error al cargar decoraciones', err)
    });
  }

  abrirModalNueva(): void {
    this.decoracionEditando.set(null);
    this.modalAbierto.set(true);
  }

  abrirModalEditar(dec: BackendDecoracionAdminResponse): void {
    this.decoracionEditando.set(dec);
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    this.decoracionEditando.set(null);
  }

  eliminar(dec: BackendDecoracionAdminResponse): void {
    this.dialogTitulo.set('Eliminar Decoración');
    this.dialogMensaje.set('¿Está seguro de que desea eliminar la decoración "' + dec.decoracionNombre + '"?');
    this.accionConfirmacion = () => {
      this.decoracionService.eliminarDecoracion(dec.decoracionId).subscribe({
        next: () => {
          this.cargarDecoraciones();
          this.dialogAbierto.set(false);
        },
        error: (err) => {
          alert(err.error?.message || 'Error al eliminar la decoración');
          this.dialogAbierto.set(false);
        }
      });
    };
    this.dialogAbierto.set(true);
  }

  cambiarEstado(dec: BackendDecoracionAdminResponse): void {
    const nuevoEstado = dec.decoracionEstado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    this.dialogTitulo.set('Cambiar Estado');
    this.dialogMensaje.set('¿Desea cambiar el estado de "' + dec.decoracionNombre + '" a ' + nuevoEstado + '?');
    this.accionConfirmacion = () => {
      this.decoracionService.cambiarEstado(dec.decoracionId, nuevoEstado).subscribe({
        next: () => {
          this.cargarDecoraciones();
          this.dialogAbierto.set(false);
        },
        error: (err: any) => {
          alert(err.error?.message || 'Error al cambiar estado');
          this.dialogAbierto.set(false);
        }
      });
    };
    this.dialogAbierto.set(true);
  }

  ejecutarAccionConfirmada(): void {
    if (this.accionConfirmacion) {
      this.accionConfirmacion();
    }
  }
}
