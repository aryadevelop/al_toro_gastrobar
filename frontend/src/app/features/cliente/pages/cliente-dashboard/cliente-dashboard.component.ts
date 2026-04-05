import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DashboardMetric, Reserva } from '../../../../core/models/domain.models';
import { DashboardService } from '../../../../core/services/dashboard.service';
import { ReservationService } from '../../../../core/services/reservation.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { ReservationCardComponent } from '../../../../shared/ui/reservation-card/reservation-card.component';
import { StatCardComponent } from '../../../../shared/ui/stat-card/stat-card.component';

@Component({
  selector: 'app-cliente-dashboard',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent, StatCardComponent, ReservationCardComponent],
  template: `
    <section class="page-grid">
      <article class="flash-toast card" *ngIf="showFlash()">
        {{ flashMessage() }}
      </article>

      <app-page-header title="Panel de cliente" subtitle="Resumen de tus reservas y actividad"></app-page-header>

      <section style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: .8rem;">
        <app-stat-card *ngFor="let metric of metrics" [label]="metric.label" [value]="metric.value" [trend]="metric.trend ?? null"></app-stat-card>
      </section>

      <section class="page-grid">
        <h2 class="section-title">Proximas reservas</h2>
        <app-reservation-card *ngFor="let reservation of reservas" [reservation]="reservation"></app-reservation-card>
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
    `
  ]
})
export class ClienteDashboardComponent implements OnInit {
  readonly flashMessage = signal('');
  readonly showFlash = signal(false);

  metrics: DashboardMetric[] = [];
  reservas: Reserva[] = [];

  constructor(
    private readonly dashboardService: DashboardService,
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

    this.dashboardService.getMetrics().subscribe((metrics) => {
      this.metrics = metrics;
    });

    this.reservationService.list().subscribe((reservas) => {
      this.reservas = reservas;
    });
  }
}

