import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { DashboardMetric } from '../../../../core/models/domain.models';
import { DashboardService } from '../../../../core/services/dashboard.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { StatCardComponent } from '../../../../shared/ui/stat-card/stat-card.component';

@Component({
  selector: 'app-mesero-dashboard',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent, StatCardComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Dashboard de mesero" subtitle="Turno actual y servicio en sala"></app-page-header>
      <section style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: .8rem;">
        <app-stat-card *ngFor="let metric of metrics" [label]="metric.label" [value]="metric.value" [trend]="metric.trend ?? null"></app-stat-card>
      </section>
    </section>
  `
})
export class MeseroDashboardComponent implements OnInit {
  metrics: DashboardMetric[] = [];

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.dashboardService.getMetrics().subscribe((metrics) => {
      this.metrics = metrics;
    });
  }
}
