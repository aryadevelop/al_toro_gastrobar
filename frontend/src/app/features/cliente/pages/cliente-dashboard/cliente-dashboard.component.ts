import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { combineLatest } from 'rxjs';
import { DashboardMetric, Reserva } from '../../../../core/models/domain.models';
import { AuthService } from '../../../../core/services/auth.service';
import { ReservationService } from '../../../../core/services/reservation.service';
import { SalesService } from '../../../../core/services/sales.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-cliente-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent],
  template: `
    <section class="page-grid cliente-compact">
      <article class="flash-toast card" *ngIf="showFlash()">
        {{ flashMessage() }}
      </article>

      <section class="dashboard-header-row">
        <app-page-header title="Panel de cliente" subtitle="Resumen de tus reservas y actividad"></app-page-header>
        <div class="header-actions">
          <a class="btn-secondary" routerLink="/app/cliente/reserva/create">Nueva reserva</a>
          <a class="btn-secondary profile-shortcut" routerLink="/app/profile">Mi perfil</a>
        </div>
      </section>

      <article class="card points-card">
        <h3>Puntos acumulados: {{ points() }}</h3>
      </article>

      <section class="metrics-grid">
        <article class="card metric-card" *ngFor="let metric of metrics">
          <p>{{ metric.label }}</p>
          <h3>{{ metric.value }}</h3>
        </article>
      </section>

      <section class="page-grid">
        <div class="reservas-head">
          <h2 class="section-title">Reservas futuras</h2>
          <a class="history-tab" routerLink="/app/cliente/reservas/history">Historial</a>
        </div>

        <article class="card future-card" *ngFor="let reservation of reservasFuturas">
          <p><strong>Fecha y hora:</strong> {{ formatDateTime(reservation) }}</p>
          <p><strong>Número de personas:</strong> {{ reservation.guests }}</p>
          <p><strong>Estado:</strong> {{ getStatusLabel(reservation.status) }}</p>
          <p class="modify-warning" *ngIf="isModificationCutoffReached(reservation)">
            Ya no es posible modificar esta reserva. Solo puedes cancelarla.
          </p>

          <div class="reservation-actions">
            <button type="button" class="btn-secondary" (click)="onViewDetail(reservation.id)">Ver detalle</button>
            <button type="button" class="btn-secondary" *ngIf="canModifyReservation(reservation)" (click)="onModifyReservation(reservation)">
              Modificar
            </button>
            <button
              type="button"
              class="btn-danger"
              [disabled]="!canCancelReservation(reservation)"
              (click)="onCancelReservation(reservation)"
            >
              Cancelar
            </button>
          </div>
        </article>

        <article class="card empty-state-box" *ngIf="reservasFuturas.length === 0">
          <p class="empty-state">No tienes reservas futuras. ¡Crea una nueva reserva!</p>
          <a class="btn-secondary" routerLink="/app/cliente/reserva/create">Nueva reserva</a>
        </article>
      </section>
    </section>
  `,
  styles: [
    `
      .flash-toast {
        border: 1px solid #6F4E37;
        background: rgba(111, 78, 55, 0.1);
        color: #4d3323;
        padding: 0.7rem 0.9rem;
        font-weight: 700;
      }

      .dashboard-header-row {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 0.8rem;
        flex-wrap: wrap;
      }

      .profile-shortcut {
        text-align: center;
        white-space: nowrap;
      }

      .header-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.6rem;
        align-items: center;
      }

      .header-actions .btn-primary,
      .header-actions .btn-secondary {
        padding: 0.5rem 0.72rem;
        font-size: 0.82rem;
        border-radius: 8px;
      }

      .points-card {
        padding: 0.75rem 0.9rem;
        display: grid;
        gap: 0.32rem;
      }

      .metrics-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
        gap: 0.55rem;
      }

      .metric-card {
        padding: 0.45rem 0.65rem;
        min-height: 66px;
        display: grid;
        align-content: center;
        gap: 0.1rem;
      }

      .metric-card p,
      .metric-card h3 {
        margin: 0;
      }

      .metric-card p {
        color: var(--muted);
        font-size: 0.78rem;
      }

      .metric-card h3 {
        font-size: 0.98rem;
        line-height: 1.05;
      }

      .points-card h3 {
        margin: 0;
      }

      .reservas-head {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 0.5rem;
      }

      .history-tab {
        border: 1px solid rgba(111, 78, 55, 0.7);
        border-radius: 8px;
        padding: 0.35rem 0.6rem;
        font-size: 0.8rem;
        color: #ffffff;
        background: #6F4E37;
      }

      .future-card {
        padding: 0.72rem 0.84rem;
        display: grid;
        gap: 0.28rem;
      }

      .future-card p {
        margin: 0;
        font-size: 0.84rem;
      }

      .modify-warning {
        color: #5b3f2c;
        font-size: 0.78rem;
      }

      .reservation-actions {
        margin-top: 0.2rem;
        display: flex;
        flex-wrap: wrap;
        gap: 0.4rem;
      }

      .reservation-actions .btn-secondary,
      .reservation-actions .btn-danger {
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
export class ClienteDashboardComponent implements OnInit {
  readonly flashMessage = signal('');
  readonly showFlash = signal(false);
  readonly points = signal(0);

  metrics: DashboardMetric[] = [];
  reservasFuturas: Reserva[] = [];

  constructor(
    private readonly authService: AuthService,
    private readonly reservationService: ReservationService,
    private readonly salesService: SalesService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const state = history.state as { flashMessage?: string };
    if (state.flashMessage) {
      this.flashMessage.set(state.flashMessage);
      this.showFlash.set(true);
      setTimeout(() => this.showFlash.set(false), 3500);
      history.replaceState({}, document.title, this.router.url);
    }

    this.loadDashboardData();
  }

  onViewDetail(reservationId: string): void {
    void this.router.navigate(['/app/cliente/reserva/detail', reservationId]);
  }

  onModifyReservation(reservation: Reserva): void {
    if (!this.canModifyReservation(reservation)) {
      if (this.isModificationCutoffReached(reservation)) {
        this.flashMessage.set('Ya no es posible modificar esta reserva. Solo puedes cancelarla.');
        this.showFlash.set(true);
        setTimeout(() => this.showFlash.set(false), 3500);
      }
      return;
    }

    const reservationId = reservation.id;
    void this.router.navigate(['/app/cliente/reserva/edit', reservationId]);
  }

  onCancelReservation(reservation: Reserva): void {
    if (!this.canCancelReservation(reservation)) {
      return;
    }

    if (!window.confirm('¿Deseas cancelar esta reserva?')) {
      return;
    }

    this.reservationService.update(reservation.id, { status: 'CANCELLED' }).subscribe(() => {
      this.flashMessage.set('Reserva cancelada correctamente.');
      this.showFlash.set(true);
      setTimeout(() => this.showFlash.set(false), 3500);
      this.loadDashboardData();
    });
  }

  canModifyReservation(reservation: Reserva): boolean {
    return (
      this.isFutureReservation(reservation) &&
      (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED') &&
      !this.isModificationCutoffReached(reservation)
    );
  }

  canCancelReservation(reservation: Reserva): boolean {
    return this.isFutureReservation(reservation) && (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED');
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

  formatDateTime(reserva: Reserva): string {
    return this.toDateTime(reserva).toLocaleString('es-CO', {
      dateStyle: 'short',
      timeStyle: 'short'
    });
  }

  private loadDashboardData(): void {
    const currentUser = this.authService.currentUser();
    if (!currentUser) {
      this.metrics = [];
      this.reservasFuturas = [];
      this.points.set(0);
      return;
    }

    combineLatest([this.reservationService.listByCliente(currentUser.id), this.salesService.list()]).subscribe(([reservas, ventas]) => {
      const orderedFuture = reservas
        .filter((item) => this.isFutureReservation(item) && (item.status === 'PENDING' || item.status === 'CONFIRMED'))
        .sort((a, b) => this.toDateTime(a).getTime() - this.toDateTime(b).getTime());

      const pending = orderedFuture.filter((item) => item.status === 'PENDING').length;
      const confirmed = orderedFuture.filter((item) => item.status === 'CONFIRMED').length;

      const points = ventas.filter((item) => item.clienteId === currentUser.id && item.paid).length;
      this.points.set(points);
      this.reservasFuturas = orderedFuture;

      this.metrics = [
        { id: 'cm-1', label: 'Reservas futuras', value: orderedFuture.length, tone: 'neutral' },
        { id: 'cm-2', label: 'Pendientes', value: pending, tone: pending > 0 ? 'success' : 'neutral' },
        { id: 'cm-3', label: 'Confirmadas', value: confirmed, tone: 'neutral' }
      ];
    });
  }

  private isFutureReservation(reserva: Reserva): boolean {
    return this.toDateTime(reserva).getTime() > Date.now();
  }

  isModificationCutoffReached(reserva: Reserva): boolean {
    const sameDate = new Date(`${reserva.date}T00:00:00`).toDateString() === new Date().toDateString();
    if (!sameDate) {
      return false;
    }

    const cutoff = new Date(`${reserva.date}T16:00:00`).getTime();
    return Date.now() >= cutoff;
  }

  private toDateTime(reserva: Reserva): Date {
    return new Date(`${reserva.date}T${reserva.time}:00`);
  }
}


