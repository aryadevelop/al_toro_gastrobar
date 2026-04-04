import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
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
      <app-page-header title="Panel de cliente" subtitle="Resumen de tus reservas y actividad"></app-page-header>

      <section style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: .8rem;">
        <app-stat-card *ngFor="let metric of metrics" [label]="metric.label" [value]="metric.value" [trend]="metric.trend ?? null"></app-stat-card>
      </section>

      <section class="page-grid">
        <h2 class="section-title">Proximas reservas</h2>
        <app-reservation-card *ngFor="let reservation of reservas" [reservation]="reservation"></app-reservation-card>
      </section>
    </section>
  `
})
export class ClienteDashboardComponent implements OnInit {
  metrics: DashboardMetric[] = [];
  reservas: Reserva[] = [];

  constructor(
    private readonly dashboardService: DashboardService,
    private readonly reservationService: ReservationService
  ) {}

  ngOnInit(): void {
    this.dashboardService.getMetrics().subscribe((metrics) => {
      this.metrics = metrics;
    });

    this.reservationService.list().subscribe((reservas) => {
      this.reservas = reservas;
    });
  }
}
