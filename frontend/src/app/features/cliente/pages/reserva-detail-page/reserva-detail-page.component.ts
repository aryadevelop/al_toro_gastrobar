import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { combineLatest } from 'rxjs';
import { Comanda, ComandaItem, Pago, Reserva, Venta } from '../../../../core/models/domain.models';
import { AuthService } from '../../../../core/services/auth.service';
import { ComandaService } from '../../../../core/services/comanda.service';
import { ReservationService } from '../../../../core/services/reservation.service';
import { SalesService } from '../../../../core/services/sales.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-reserva-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent],
  template: `
    <section class="page-grid cliente-compact">
      <app-page-header title="Detalle de visita" subtitle="Informacion completa de la reserva y su consumo"></app-page-header>

      <article class="card detail-card" *ngIf="reservation(); else notFound">
        <p><strong>Fecha y hora:</strong> {{ formatReservationDateTime() }}</p>
        <p><strong>Número de personas:</strong> {{ reservation()!.guests }}</p>
        <p><strong>Estado de la visita:</strong> {{ getStatusLabel(reservation()!.status) }}</p>
        <p><strong>Mesa asignada:</strong> {{ reservation()!.tableCode || 'No aplica' }}</p>
        <p><strong>Zona seleccionada:</strong> {{ reservation()!.zoneName || 'No aplica' }}</p>
        <p><strong>Decoración seleccionada:</strong> {{ reservation()!.decorationName || 'No aplica' }}</p>

        <section class="detail-section">
          <h4>Productos de la comanda final</h4>

          <div *ngIf="comandaItems().length > 0; else noComanda">
            <article class="line-item" *ngFor="let item of comandaItems()">
              <span>{{ item.productName }} x {{ item.quantity }}</span>
              <span>
                {{ item.unitPrice | currency:'COP':'symbol':'1.0-0' }}
                ({{ item.quantity * item.unitPrice | currency:'COP':'symbol':'1.0-0' }})
              </span>
            </article>
          </div>

          <ng-template #noComanda>
            <p class="muted">No aplica</p>
          </ng-template>
        </section>

        <section class="detail-section">
          <h4>Historial de abonos</h4>

          <div *ngIf="payments().length > 0; else noPayments">
            <article class="line-item" *ngFor="let payment of payments()">
              <span>{{ payment.method }} - {{ formatDate(payment.paidAt) }}</span>
              <span>{{ payment.amount | currency:'COP':'symbol':'1.0-0' }}</span>
            </article>
          </div>

          <ng-template #noPayments>
            <p class="muted">No aplica</p>
          </ng-template>
        </section>

        <p class="total-row"><strong>Total de la cuenta:</strong> {{ totalSale() | currency:'COP':'symbol':'1.0-0' }}</p>

        <div class="detail-actions">
          <a class="btn-secondary" routerLink="/app/cliente">Volver al dashboard</a>
          <a class="btn-secondary" routerLink="/app/cliente/reservas/history">Ir a historial</a>
        </div>
      </article>

      <ng-template #notFound>
        <article class="card detail-card">
          <p class="muted">No se encontró el detalle de esta reserva.</p>
          <a class="btn-secondary" routerLink="/app/cliente">Volver al dashboard</a>
        </article>
      </ng-template>
    </section>
  `,
  styles: [
    `
      .detail-card {
        padding: 0.85rem;
        display: grid;
        gap: 0.35rem;
      }

      .detail-card p,
      .detail-card h4 {
        margin: 0;
      }

      .detail-section {
        display: grid;
        gap: 0.3rem;
        margin-top: 0.2rem;
      }

      .line-item {
        display: flex;
        justify-content: space-between;
        gap: 0.45rem;
        border: 1px dashed rgba(10, 10, 10, 0.2);
        border-radius: 8px;
        padding: 0.35rem 0.45rem;
        font-size: 0.82rem;
      }

      .muted {
        color: var(--muted);
      }

      .total-row {
        margin-top: 0.25rem;
      }

      .detail-actions {
        margin-top: 0.3rem;
        display: flex;
        flex-wrap: wrap;
        gap: 0.4rem;
      }

      .detail-actions .btn-secondary {
        padding: 0.42rem 0.62rem;
        font-size: 0.78rem;
        border-radius: 8px;
      }
    `
  ]
})
export class ReservaDetailPageComponent implements OnInit {
  readonly reservation = signal<Reserva | null>(null);
  readonly comanda = signal<Comanda | null>(null);
  readonly sale = signal<Venta | null>(null);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly authService: AuthService,
    private readonly reservationService: ReservationService,
    private readonly comandaService: ComandaService,
    private readonly salesService: SalesService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.currentUser();
    if (!currentUser) {
      void this.router.navigateByUrl('/auth/login');
      return;
    }

    const reservationId = this.activatedRoute.snapshot.paramMap.get('id') ?? '';

    combineLatest([
      this.reservationService.listByCliente(currentUser.id),
      this.comandaService.list(),
      this.salesService.list()
    ]).subscribe(([reservations, comandas, sales]) => {
      const reservation = reservations.find((item) => item.id === reservationId) ?? null;
      this.reservation.set(reservation);

      if (!reservation) {
        this.comanda.set(null);
        this.sale.set(null);
        return;
      }

      const comanda = comandas.find((item) => item.reservaId === reservation.id) ?? null;
      this.comanda.set(comanda);

      if (!comanda) {
        this.sale.set(null);
        return;
      }

      const sale = sales.find((item) => item.comandaId === comanda.id && item.clienteId === currentUser.id) ?? null;
      this.sale.set(sale);
    });
  }

  comandaItems(): ComandaItem[] {
    return this.comanda()?.items ?? [];
  }

  payments(): Pago[] {
    return this.sale()?.payments ?? [];
  }

  totalSale(): number {
    return this.sale()?.total ?? 0;
  }

  formatReservationDateTime(): string {
    const target = this.reservation();
    if (!target) {
      return 'No aplica';
    }

    return new Date(`${target.date}T${target.time}:00`).toLocaleString('es-CO', {
      dateStyle: 'short',
      timeStyle: 'short'
    });
  }

  formatDate(isoDate: string): string {
    return new Date(isoDate).toLocaleString('es-CO', {
      dateStyle: 'short',
      timeStyle: 'short'
    });
  }

  getStatusLabel(status: Reserva['status']): string {
    if (status === 'CONFIRMED') {
      return 'Confirmada';
    }

    if (status === 'PENDING') {
      return 'Pendiente';
    }

    if (status === 'CANCELLED') {
      return 'Cancelada';
    }

    if (status === 'COMPLETED') {
      return 'Completada';
    }

    if (status === 'ARRIVED') {
      return 'Asistio';
    }

    return status;
  }
}

