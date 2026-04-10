import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { combineLatest } from 'rxjs';
import { Comanda, Reserva, Venta } from '../../../../core/models/domain.models';
import { AuthService } from '../../../../core/services/auth.service';
import { ComandaService } from '../../../../core/services/comanda.service';
import { ReservationService } from '../../../../core/services/reservation.service';
import { SalesService } from '../../../../core/services/sales.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

interface VisitHistoryItem {
  reservationId?: string;
  dateTime: Date;
  guests: number;
  statusLabel: string;
  total: number;
  hasDetail: boolean;
}

@Component({
  selector: 'app-reservas-history-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent],
  template: `
    <section class="page-grid cliente-compact">
      <app-page-header title="Historial de visitas" subtitle="Tus visitas reales cerradas por caja"></app-page-header>

      <div class="history-tabs">
        <a class="tab-link" routerLink="/app/cliente">Reservas futuras</a>
        <span class="tab-link active">Historial</span>
      </div>

      <article class="card points-card">
        <h3>Puntos acumulados: {{ points() }}</h3>
      </article>

      <section class="page-grid">
        <article class="card visit-card" *ngFor="let visit of visitHistory">
          <p><strong>Fecha y hora:</strong> {{ formatDateTime(visit.dateTime) }}</p>
          <p><strong>Número de personas:</strong> {{ visit.guests }}</p>
          <p><strong>Estado de la visita:</strong> {{ visit.statusLabel }}</p>
          <p><strong>Total:</strong> {{ visit.total | currency:'COP':'symbol':'1.0-0' }}</p>

          <div class="visit-actions">
            <button type="button" class="btn-secondary" [disabled]="!visit.hasDetail" (click)="onViewDetail(visit)">Ver detalle</button>
          </div>
        </article>

        <article class="card empty-state-box" *ngIf="visitHistory.length === 0">
          <p class="empty-state">Aún no tienes visitas registradas.</p>
          <a class="btn-primary" routerLink="/app/cliente/reserva/create">Nueva reserva</a>
        </article>
      </section>
    </section>
  `,
  styles: [
    `
      .history-tabs {
        display: flex;
        gap: 0.45rem;
        align-items: center;
      }

      .tab-link {
        border: 1px solid rgba(168, 24, 47, 0.7);
        border-radius: 8px;
        padding: 0.34rem 0.6rem;
        font-size: 0.8rem;
        color: #ffffff;
        background: #A8182F;
      }

      .tab-link.active {
        background: #A8182F;
        color: #ffffff;
      }

      .points-card {
        padding: 0.75rem 0.9rem;
        display: grid;
        gap: 0.32rem;
      }

      .points-card h3 {
        margin: 0;
      }

      .visit-card {
        padding: 0.72rem 0.84rem;
        display: grid;
        gap: 0.25rem;
      }

      .visit-card p {
        margin: 0;
        font-size: 0.84rem;
      }

      .visit-actions {
        margin-top: 0.2rem;
      }

      .visit-actions .btn-secondary {
        padding: 0.42rem 0.62rem;
        font-size: 0.78rem;
        border-radius: 8px;
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
    `
  ]
})
export class ReservasHistoryPageComponent implements OnInit {
  readonly points = signal(0);

  visitHistory: VisitHistoryItem[] = [];

  constructor(
    private readonly authService: AuthService,
    private readonly reservationService: ReservationService,
    private readonly comandaService: ComandaService,
    private readonly salesService: SalesService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.currentUser();
    if (!currentUser) {
      this.points.set(0);
      this.visitHistory = [];
      return;
    }

    combineLatest([
      this.reservationService.listByCliente(currentUser.id),
      this.comandaService.list(),
      this.salesService.list()
    ]).subscribe(([reservas, comandas, ventas]) => {
      const paidSales = ventas.filter((item) => item.clienteId === currentUser.id && item.paid);
      this.points.set(paidSales.length);
      this.visitHistory = this.mapVisitHistory(paidSales, reservas, comandas);
    });
  }

  onViewDetail(visit: VisitHistoryItem): void {
    if (!visit.reservationId) {
      return;
    }

    void this.router.navigate(['/app/cliente/reserva/detail', visit.reservationId]);
  }

  formatDateTime(date: Date): string {
    return date.toLocaleString('es-CO', {
      dateStyle: 'short',
      timeStyle: 'short'
    });
  }

  private mapVisitHistory(sales: Venta[], reservas: Reserva[], comandas: Comanda[]): VisitHistoryItem[] {
    const reservaById = new Map(reservas.map((item) => [item.id, item]));
    const comandaById = new Map(comandas.map((item) => [item.id, item]));

    return sales
      .map((sale) => {
        const comanda = comandaById.get(sale.comandaId);
        const reserva = comanda?.reservaId ? reservaById.get(comanda.reservaId) : undefined;

        const dateTime = reserva ? new Date(`${reserva.date}T${reserva.time}:00`) : new Date(sale.createdAt);

        return {
          reservationId: reserva?.id,
          dateTime,
          guests: reserva?.guests ?? 0,
          statusLabel: this.getVisitStatusLabel(reserva?.status),
          total: sale.total,
          hasDetail: Boolean(reserva?.id)
        } satisfies VisitHistoryItem;
      })
      .sort((a, b) => b.dateTime.getTime() - a.dateTime.getTime());
  }

  private getVisitStatusLabel(status?: Reserva['status']): string {
    if (!status) {
      return 'Completada';
    }

    if (status === 'ARRIVED') {
      return 'Asistio';
    }

    if (status === 'COMPLETED') {
      return 'Completada';
    }

    if (status === 'CONFIRMED') {
      return 'Confirmada';
    }

    if (status === 'PENDING') {
      return 'Pendiente';
    }

    if (status === 'CANCELLED') {
      return 'Cancelada';
    }

    return status;
  }
}


