import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Comanda } from '../../../../core/models/domain.models';
import { ComandaService } from '../../../../core/services/comanda.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { OrderCardComponent } from '../../../../shared/ui/order-card/order-card.component';

@Component({
  selector: 'app-produccion-dashboard',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent, OrderCardComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Dashboard de produccion" subtitle="Comandas por preparar y listas"></app-page-header>

      <section style="display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: .8rem;">
        <app-order-card *ngFor="let order of comandas" [order]="order"></app-order-card>
      </section>
    </section>
  `
})
export class ProduccionDashboardComponent implements OnInit {
  comandas: Comanda[] = [];

  constructor(private readonly comandaService: ComandaService) {}

  ngOnInit(): void {
    this.comandaService.list().subscribe((comandas) => {
      this.comandas = comandas;
    });
  }
}
