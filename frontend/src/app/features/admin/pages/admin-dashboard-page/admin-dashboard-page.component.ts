import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DashboardMetric } from '../../../../core/models/domain.models';
import { DashboardService } from '../../../../core/services/dashboard.service';
import { StatCardComponent } from '../../../../shared/ui/stat-card/stat-card.component';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink, StatCardComponent],
  template: `
    <section class="page-grid">
      <section class="admin-header">
        <h1 class="admin-title">Administrador</h1>
        <a class="btn-primary history-btn" routerLink="/app/admin/cliente-historial">Historial de visitas</a>
      </section>

      <section style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: .8rem;">
        <app-stat-card *ngFor="let metric of metrics" [label]="metric.label" [value]="metric.value" [trend]="metric.trend ?? null"></app-stat-card>
      </section>

      <article class="card" style="padding: 1rem;">
        <h3 style="margin-top:0;">Estado operativo</h3>
        <p style="margin:0; color: var(--muted);">Base frontend lista para conectar fuentes reales de analitica y ventas.</p>
      </article>
    </section>
  `,
  styles: [
    `
      .admin-header {
        display: flex;
        flex-direction: column;
        gap: 0.6rem;
        margin-bottom: 1rem;
      }

      .admin-title {
        margin: 0;
        font-size: 1.5rem;
        font-weight: 700;
        color: #4d3323;
      }

      .history-btn {
        background: #6F4E37;
        color: #ffffff;
        border: 1px solid rgba(111, 78, 55, 0.7);
        padding: 0.5rem 0.8rem;
        border-radius: 8px;
        text-decoration: none;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-weight: 600;
        width: fit-content;
      }

      @media (max-width: 768px) {
        .admin-title {
          font-size: 1.2rem;
        }

        .history-btn {
          width: 100%;
          padding: 0.6rem 1rem;
          font-size: 0.9rem;
          box-shadow: 0 2px 8px rgba(111, 78, 55, 0.3);
        }
      }
    `
  ]
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
