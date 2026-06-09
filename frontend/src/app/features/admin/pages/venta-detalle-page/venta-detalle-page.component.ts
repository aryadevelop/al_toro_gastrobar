import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { VentaDetalleAdmin, VentaDetalleAdminService } from '../../../../core/services/venta-detalle-admin.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-venta-detalle-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent],
  template: `
    <section class="page-grid cliente-compact">
      <app-page-header title="Detalle de venta" subtitle="Consulta completa de la venta registrada"></app-page-header>

      <article class="card" style="padding: 0.8rem;" *ngIf="loading()">
        <p style="margin: 0;">Cargando detalle de la venta...</p>
      </article>

      <article class="card error-box" *ngIf="errorMessage() && !loading()">
        <p>{{ errorMessage() }}</p>
        <a class="btn-secondary" routerLink="/app/admin/ventas">Volver al listado</a>
      </article>

      <ng-container *ngIf="detalle() as venta">
        <article class="card section-box">
          <div class="section-head">
            <h3>Información general</h3>
            <span class="badge">Reserva: {{ formatReservationStatus(venta.estadoReserva) }}</span>
          </div>

          <div class="kv-grid">
            <p><strong>ID venta:</strong> {{ venta.ventaId }}</p>
            <p><strong>Fecha/hora:</strong> {{ venta.fechaHora | date:'short':'':'es-CO' }}</p>
            <p><strong>Método de pago:</strong> {{ formatPaymentMethod(venta.metodoPago) }}</p>
            <p><strong>Mesero:</strong> {{ venta.meseroNombre || 'No registrado' }}</p>
          </div>

          <p class="alert-box" *ngIf="venta.alertaReservaCancelada">{{ venta.alertaReservaCancelada }}</p>
        </article>

        <article class="card section-box">
          <h3>Cliente y mesa</h3>
          <div class="kv-grid">
            <p><strong>Cliente:</strong> {{ venta.cliente.nombre }}</p>
            <p><strong>Teléfono:</strong> {{ venta.cliente.telefono || 'No registrado' }}</p>
            <p><strong>Mesa:</strong> {{ venta.mesa?.identificador || 'No asignada' }}</p>
            <p><strong>Zona:</strong> {{ venta.mesa?.zona || 'No registrada' }}</p>
          </div>
        </article>

        <article class="card section-box" *ngIf="venta.menuEspecial; else itemsBlock">
          <h3>Menú especial</h3>
          <div class="kv-grid">
            <p><strong>Nombre:</strong> {{ venta.menuEspecial!.nombreMenu }}</p>
            <p><strong>Valor por persona:</strong> {{ venta.menuEspecial!.valorPorPersona | currency:'COP':'symbol':'1.0-0' }}</p>
            <p><strong>Número de personas:</strong> {{ venta.menuEspecial!.numeroPersonas }}</p>
            <p><strong>Total calculado:</strong> {{ venta.menuEspecial!.totalCalculado | currency:'COP':'symbol':'1.0-0' }}</p>
          </div>
        </article>

        <ng-template #itemsBlock>
          <article class="card section-box">
            <h3>Detalle de platos</h3>
            <div class="list-grid" *ngIf="venta.items.length > 0; else noItems">
              <article class="line-item" *ngFor="let item of venta.items">
                <div class="line-main">
                  <strong>{{ item.nombre }}</strong>
                  <small *ngIf="item.especificaciones">{{ item.nombre }} - {{ item.especificaciones }}</small>
                </div>
                <div class="line-values">
                  <span>{{ item.cantidad }} x {{ item.precioUnitario | currency:'COP':'symbol':'1.0-0' }}</span>
                  <strong>{{ item.subtotal | currency:'COP':'symbol':'1.0-0' }}</strong>
                </div>
              </article>
            </div>
            <ng-template #noItems>
              <p class="muted">No hay platos registrados para esta venta.</p>
            </ng-template>
          </article>
        </ng-template>

        <article class="card section-box" *ngIf="venta.serviciosAdicionales.length > 0">
          <h3>Servicios adicionales</h3>
          <div class="list-grid">
            <article class="line-item" *ngFor="let servicio of venta.serviciosAdicionales">
              <div class="line-main">
                <strong>{{ servicio.nombre }}</strong>
              </div>
              <div class="line-values">
                <strong>{{ servicio.costo | currency:'COP':'symbol':'1.0-0' }}</strong>
              </div>
            </article>
          </div>
        </article>

        <article class="card section-box" *ngIf="venta.notaReserva">
          <h3>Nota adicional de la reserva</h3>
          <p class="note-box">{{ venta.notaReserva }}</p>
        </article>

        <article class="card section-box totals-box">
          <h3>Resumen de valores</h3>
          <div class="totals-grid">
            <p><span>Subtotal</span><strong>{{ venta.subtotal | currency:'COP':'symbol':'1.0-0' }}</strong></p>
            <p><span>Total</span><strong>{{ venta.total | currency:'COP':'symbol':'1.0-0' }}</strong></p>
          </div>
        </article>
      </ng-container>
    </section>
  `,
  styles: [
    `
      .section-box {
        padding: 0.8rem;
        display: grid;
        gap: 0.6rem;
      }

      .section-box h3 {
        margin: 0;
        font-size: 0.95rem;
      }

      .section-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.5rem;
        flex-wrap: wrap;
      }

      .badge {
        border: 1px solid rgba(211, 47, 47, 0.45);
        border-radius: 999px;
        padding: 0.2rem 0.6rem;
        font-size: 0.76rem;
        font-weight: 700;
        color: #4d3323;
      }

      .kv-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 0.35rem 0.7rem;
      }

      .kv-grid p {
        margin: 0;
        font-size: 0.84rem;
      }

      .list-grid {
        display: grid;
        gap: 0.45rem;
      }

      .line-item {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 0.4rem;
        border: 1px dashed rgba(10, 10, 10, 0.2);
        border-radius: 8px;
        padding: 0.5rem;
      }

      .line-main {
        display: grid;
        gap: 0.18rem;
      }

      .line-main strong {
        font-size: 0.84rem;
      }

      .line-main small {
        font-size: 0.76rem;
        color: var(--muted);
      }

      .line-values {
        display: grid;
        justify-items: end;
        gap: 0.1rem;
        font-size: 0.78rem;
      }

      .note-box {
        margin: 0;
        border: 1px solid rgba(211, 47, 47, 0.35);
        border-radius: 8px;
        background: rgba(211, 47, 47, 0.08);
        color: #4d3323;
        padding: 0.55rem 0.65rem;
        font-size: 0.84rem;
      }

      .alert-box {
        margin: 0;
        border: 1px solid rgba(196, 30, 58, 0.42);
        border-radius: 8px;
        background: rgba(196, 30, 58, 0.08);
        color: #7a1122;
        padding: 0.55rem 0.65rem;
        font-size: 0.84rem;
        font-weight: 600;
      }

      .totals-grid {
        display: grid;
        gap: 0.3rem;
      }

      .totals-grid p {
        margin: 0;
        display: flex;
        justify-content: space-between;
        gap: 0.6rem;
        font-size: 0.86rem;
      }

      .error-box {
        padding: 0.8rem;
        display: grid;
        gap: 0.6rem;
      }

      .error-box p {
        margin: 0;
      }

      .muted {
        margin: 0;
        color: var(--muted);
        opacity: 0.7;
      }
    `,
  ]
})
export class VentaDetallePageComponent implements OnInit {
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly detalle = signal<VentaDetalleAdmin | null>(null);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly ventaDetalleAdminService: VentaDetalleAdminService
  ) {}

  ngOnInit(): void {
    const visitId = this.activatedRoute.snapshot.paramMap.get('id');
    if (!visitId) {
      this.loading.set(false);
      this.errorMessage.set('No se recibió el identificador de la venta.');
      return;
    }

    this.ventaDetalleAdminService.getDetalle(visitId).subscribe({
      next: (detail) => {
        this.detalle.set(detail);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const backendMessage =
          (typeof err.error?.message === 'string' && err.error.message.trim().length > 0
            ? err.error.message
            : '') ||
          (typeof err.error === 'string' && err.error.trim().length > 0 ? err.error : '');

        this.errorMessage.set(backendMessage || 'No fue posible cargar el detalle de la venta.');
        this.loading.set(false);
      }
    });
  }

  formatPaymentMethod(method?: string): string {
    const normalized = (method ?? '').toUpperCase();
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
    return method || 'No registrado';
  }

  formatReservationStatus(status?: string): string {
    const normalized = (status ?? '').toUpperCase();
    if (normalized === 'COMPLETADA' || normalized === 'COMPLETED') {
      return 'Completada';
    }
    if (normalized === 'CANCELADA' || normalized === 'CANCELLED') {
      return 'Cancelada';
    }
    if (normalized === 'PENDIENTE' || normalized === 'PENDING') {
      return 'Pendiente';
    }
    if (normalized === 'CONFIRMADA' || normalized === 'CONFIRMED') {
      return 'Confirmada';
    }
    return status || 'No aplica';
  }
}
