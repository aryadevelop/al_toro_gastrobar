import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DashboardMetric, Reserva } from '../../../../core/models/domain.models';
import { AuthService } from '../../../../core/services/auth.service';
import { ReservationService } from '../../../../core/services/reservation.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { ReservationCardComponent } from '../../../../shared/ui/reservation-card/reservation-card.component';
import { StatCardComponent } from '../../../../shared/ui/stat-card/stat-card.component';

@Component({
  selector: 'app-cliente-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent, StatCardComponent, ReservationCardComponent],
  template: `
    <section class="page-grid cliente-compact">
      <article class="flash-toast card" *ngIf="showFlash()">
        {{ flashMessage() }}
      </article>

      <section class="dashboard-header-row">
        <app-page-header title="Panel de cliente" subtitle="Resumen de tus reservas y actividad"></app-page-header>
        <div class="header-actions">
          <a class="btn-primary" routerLink="/app/cliente/reserva/create">Nueva reserva</a>
          <a class="btn-secondary profile-shortcut" routerLink="/app/profile">Modificar mis datos</a>
        </div>
      </section>

      <section style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: .8rem;">
        <app-stat-card *ngFor="let metric of metrics" [label]="metric.label" [value]="metric.value" [trend]="metric.trend ?? null" [compact]="true"></app-stat-card>
      </section>

      <section class="page-grid">
        <h2 class="section-title">Próximas reservas</h2>
        <app-reservation-card *ngFor="let reservation of reservas" [reservation]="reservation" [compact]="true"></app-reservation-card>
        <p *ngIf="reservas.length === 0" class="empty-state">No tienes reservas programadas.</p>
      </section>
    </section>
  `,
  styles: [
    `
      .flash-toast {
        border: 1px solid #A8182F;
        background: rgba(168, 24, 47, 0.1);
        color: #6b1111;
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
        padding: 0.64rem 0.94rem;
        font-size: 0.92rem;
        border-radius: 10px;
      }

      .empty-state {
        margin: 0;
        color: var(--muted);
        font-size: 0.9rem;
      }
    `
  ]
})
export class ClienteDashboardComponent implements OnInit {
  readonly flashMessage = signal('');
  readonly showFlash = signal(false);

  metrics: DashboardMetric[] = [];
  reservas: Reserva[] = [];

  constructor(
    private readonly authService: AuthService,
    private readonly reservationService: ReservationService,
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

    const currentUser = this.authService.currentUser();
    if (!currentUser) {
      this.metrics = [];
      this.reservas = [];
      return;
    }

    this.reservationService.listByCliente(currentUser.id).subscribe((reservas) => {
      const sorted = [...reservas].sort((a, b) => this.toDateTime(a).getTime() - this.toDateTime(b).getTime());
      this.reservas = sorted;
      this.metrics = this.buildMetrics(sorted);
    });
  }

  private buildMetrics(reservas: Reserva[]): DashboardMetric[] {
    const pending = reservas.filter((item) => item.status === 'PENDING').length;
    const confirmed = reservas.filter((item) => item.status === 'CONFIRMED').length;
    const attended = reservas.filter((item) => item.status === 'ARRIVED' || item.status === 'COMPLETED').length;

    return [
      { id: 'cm-1', label: 'Mis reservas', value: reservas.length, tone: 'neutral' },
      { id: 'cm-2', label: 'Pendientes', value: pending, tone: pending > 0 ? 'success' : 'neutral' },
      { id: 'cm-3', label: 'Confirmadas / atendidas', value: confirmed + attended, tone: 'neutral' }
    ];
  }

  private toDateTime(reserva: Reserva): Date {
    return new Date(`${reserva.date}T${reserva.time}:00`);
  }
}

