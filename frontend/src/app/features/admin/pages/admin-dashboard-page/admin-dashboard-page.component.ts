import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { DashboardMetric } from '../../../../core/models/domain.models';
import { DashboardService } from '../../../../core/services/dashboard.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { StatCardComponent } from '../../../../shared/ui/stat-card/stat-card.component';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent, StatCardComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Dashboard administrativo" subtitle="Indicadores diarios del restaurante"></app-page-header>

      <section style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: .8rem;">
        <app-stat-card *ngFor="let metric of metrics" [label]="metric.label" [value]="metric.value" [trend]="metric.trend ?? null"></app-stat-card>
      </section>

      <article class="card" style="padding: 1rem;">
        <h3 style="margin-top:0;">Estado operativo</h3>
        <p style="margin:0; color: var(--muted);">Base frontend lista para conectar fuentes reales de analitica y ventas.</p>
      </article>
    </section>
  `
})
export class AdminDashboardPageComponent implements OnInit {
  metrics: DashboardMetric[] = [];

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.dashboardService.getMetrics().subscribe((metrics) => {
      this.metrics = metrics;
    });
  }
}
