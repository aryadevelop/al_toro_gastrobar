import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { combineLatest } from 'rxjs';
import { Reserva } from '../../../../core/models/domain.models';
import { AuthService } from '../../../../core/services/auth.service';
import { ClientePointsService } from '../../../../core/services/cliente-points.service';
import { ReservationService } from '../../../../core/services/reservation.service';
import { VisitHistoryEntry, VisitService } from '../../../../core/services/visit.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-reservas-history-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent],
  template: `
    <section class="page-grid cliente-compact">
      <article class="flash-toast card" *ngIf="showFlash()">
        {{ flashMessage() }}
      </article>

      <app-page-header title="Historial de visitas" subtitle="Tus visitas y reservas futuras"></app-page-header>

      <div class="history-tabs">
        <a class="tab-link" routerLink="/app/cliente">Dashboard</a>
        <span class="tab-link active">Historial</span>
      </div>

      <article class="card points-card">
        <h3>Puntos acumulados: {{ points() }}</h3>
      </article>

      <section class="page-grid">
        <h2 class="section-title">Reservas futuras</h2>

        <article class="card visit-card" *ngFor="let reservation of reservasFuturas">
          <p><strong>Fecha y hora:</strong> {{ formatDateTime(toDateTime(reservation)) }}</p>
          <p><strong>Número de personas:</strong> {{ reservation.guests }}</p>
          <p><strong>Estado:</strong> {{ getReservationStatusLabel(reservation.status) }}</p>

          <div class="visit-actions">
            <button type="button" class="btn-secondary" (click)="onViewFutureDetail(reservation.id)">Ver detalle</button>
          </div>
        </article>

        <article class="card empty-state-box" *ngIf="reservasFuturas.length === 0">
          <p class="empty-state">No tienes reservas futuras.</p>
        </article>
      </section>

      <section class="page-grid">
        <h2 class="section-title">Visitas registradas</h2>

        <article class="card visit-card" *ngFor="let visit of visitHistory">
          <p><strong>Fecha y hora:</strong> {{ formatDateTime(visit.dateTime) }}</p>
          <p><strong>Número de personas:</strong> {{ visit.guests }}</p>
          <p><strong>Estado de la visita:</strong> {{ visit.statusLabel }}</p>
          <p><strong>Total:</strong> {{ visit.total | currency:'COP':'symbol':'1.0-0' }}</p>

          <div class="visit-actions">
            <button type="button" class="btn-secondary" [disabled]="!visit.hasDetail" (click)="onViewVisitDetail(visit)">Ver detalle</button>
          </div>
        </article>

        <article class="card empty-state-box" *ngIf="visitHistory.length === 0">
          <p class="empty-state">Aún no tienes visitas registradas.</p>
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

      .history-tabs {
        display: flex;
        gap: 0.45rem;
        align-items: center;
      }

      .tab-link {
        border: 1px solid rgba(111, 78, 55, 0.7);
        border-radius: 8px;
        padding: 0.34rem 0.6rem;
        font-size: 0.8rem;
        color: #ffffff;
        background: #6F4E37;
      }

      .tab-link.active {
        background: #5b3f2c;
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

      .modify-warning {
        color: #5b3f2c;
        font-size: 0.78rem;
      }

      .visit-actions {
        margin-top: 0.2rem;
        display: flex;
        flex-wrap: wrap;
        gap: 0.4rem;
      }

      .visit-actions .btn-secondary,
      .visit-actions .btn-danger {
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
  readonly flashMessage = signal('');
  readonly showFlash = signal(false);

  visitHistory: VisitHistoryEntry[] = [];
  reservasFuturas: Reserva[] = [];

  constructor(
    private readonly authService: AuthService,
    private readonly reservationService: ReservationService,
    private readonly visitService: VisitService,
    private readonly clientePointsService: ClientePointsService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadHistoryData();
  }

  onViewVisitDetail(visit: VisitHistoryEntry): void {
    if (!visit.reservationId) {
      return;
    }

    void this.router.navigate(['/app/cliente/reserva/detail', visit.reservationId]);
  }

  onViewFutureDetail(reservationId: string): void {
    void this.router.navigate(['/app/cliente/reserva/detail', reservationId]);
  }

  formatDateTime(date: Date): string {
    return date.toLocaleString('es-CO', {
      dateStyle: 'short',
      timeStyle: 'short'
    });
  }

  getReservationStatusLabel(status: Reserva['status']): string {
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
      return 'Asistió';
    }

    return status;
  }

  toDateTime(reservation: Reserva): Date {
    return new Date(`${reservation.date}T${reservation.time}:00`);
  }

  private loadHistoryData(): void {
    const currentUser = this.authService.currentUser();
    if (!currentUser) {
      this.points.set(0);
      this.visitHistory = [];
      this.reservasFuturas = [];
      return;
    }

    combineLatest([
      this.reservationService.listFuture(),
      this.visitService.getHistory(currentUser.email),
      this.clientePointsService.getMyPoints(currentUser.email)
    ]).subscribe(([futureReservations, visits, currentPoints]) => {
      this.reservasFuturas = [...futureReservations]
        .filter((item) => item.status === 'PENDING' || item.status === 'CONFIRMED')
        .sort((a, b) => this.toDateTime(a).getTime() - this.toDateTime(b).getTime());

      this.visitHistory = visits;
      this.points.set(currentPoints);
    });
  }
}
