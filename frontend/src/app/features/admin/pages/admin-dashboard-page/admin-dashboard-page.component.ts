import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  AdminDashboardData,
  AdminDashboardMetodoPago,
  AdminDashboardPedidoProduccion,
} from '../../../../core/models/domain.models';
import { DashboardService } from '../../../../core/services/dashboard.service';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page-grid">
      <section class="admin-header">
        <h1 class="admin-title">Administrador</h1>
        <a class="btn-primary history-btn" routerLink="/app/admin/cliente-historial">Historial de visitas</a>
      </section>

      <article class="card state-card" *ngIf="loading()">
        <p>Cargando panel de control...</p>
      </article>

      <article class="card state-card" *ngIf="errorMessage() && !loading()">
        <p>{{ errorMessage() }}</p>
      </article>

      <ng-container *ngIf="dashboard() as data">
          <article class="card state-card" *ngIf="!hasVentas()">
          <p>No hay ventas registradas para el día de hoy</p>
        </article>

        <section class="summary-grid">
          <article class="card summary-card">
            <p class="muted">Total de ventas del dia</p>
            <strong>{{ data.ventasDelDia.totalVentas | currency:'COP':'symbol':'1.0-0' }}</strong>
          </article>

          <article class="card summary-card">
            <p class="muted">Reservas concretadas</p>
            <strong>{{ data.ventasDelDia.reservasConcretadas }}</strong>
          </article>

          <article
            class="card summary-card"
            [class.trend-up]="data.variacionVsAyer >= 0"
            [class.trend-down]="data.variacionVsAyer < 0"
          >
            <p class="muted">Comparativa</p>
            <strong>{{ formatVariacion(data.variacionVsAyer) }}</strong>
          </article>
        </section>

        <section class="grid-2">
          <article class="card data-card">
            <div class="section-head">
              <h3>Desglose por metodo de pago</h3>
            </div>
            <ng-container *ngIf="hasVentas(); else ventasEmpty">
              <div class="list-grid">
                <div class="list-row" *ngFor="let metodo of data.ventasPorMetodo">
                  <span>{{ formatMetodoPago(metodo.metodo) }}</span>
                  <span>{{ metodo.total | currency:'COP':'symbol':'1.0-0' }}</span>
                  <span class="muted">{{ formatPercent(metodo.total, data.ventasDelDia.totalVentas) }}</span>
                </div>
              </div>
            </ng-container>
          </article>

          <article class="card data-card">
            <div class="section-head">
              <h3>Ventas por zona</h3>
            </div>
            <ng-container *ngIf="hasVentas(); else ventasEmpty">
              <div class="list-grid">
                <div class="list-row" *ngFor="let zona of data.ventasPorZona">
                  <span>{{ zona.zona }}</span>
                  <span>{{ zona.total | currency:'COP':'symbol':'1.0-0' }}</span>
                  <span class="muted">{{ formatPercent(zona.total, data.ventasDelDia.totalVentas) }}</span>
                </div>
              </div>
            </ng-container>
          </article>
        </section>

        <section class="grid-2">
          <article class="card data-card">
            <div class="section-head">
              <h3>Top 3 platos mas vendidos</h3>
            </div>
            <ng-container *ngIf="hasVentas(); else ventasEmpty">
              <div class="list-grid">
                <div class="list-row" *ngFor="let plato of data.topPlatos">
                  <span>{{ plato.nombre }}</span>
                  <span>{{ plato.cantidad }} uds</span>
                  <span>{{ plato.total | currency:'COP':'symbol':'1.0-0' }}</span>
                </div>
              </div>
            </ng-container>
          </article>

          <article class="card data-card">
            <div class="section-head">
              <h3>Ingresos menu especial vs carta</h3>
            </div>
            <ng-container *ngIf="hasVentas(); else ventasEmpty">
              <div class="list-grid">
                <div class="list-row">
                  <span>Menu especial</span>
                  <span>{{ data.menuEspecialVsCarta.menuEspecial | currency:'COP':'symbol':'1.0-0' }}</span>
                  <span class="muted">
                    {{ formatPercent(data.menuEspecialVsCarta.menuEspecial, data.ventasDelDia.totalVentas) }}
                  </span>
                </div>
                <div class="list-row">
                  <span>Carta</span>
                  <span>{{ data.menuEspecialVsCarta.carta | currency:'COP':'symbol':'1.0-0' }}</span>
                  <span class="muted">{{ formatPercent(data.menuEspecialVsCarta.carta, data.ventasDelDia.totalVentas) }}</span>
                </div>
              </div>
            </ng-container>
          </article>
        </section>

        <article class="card data-card">
          <div class="section-head">
            <h3>Rendimiento por mesero</h3>
          </div>
          <div class="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Mesero</th>
                  <th>Mesas atendidas</th>
                  <th>Total facturado</th>
                  <th>Promedio por mesa</th>
                  <th>Mesas activas</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let mesero of data.rendimientoMeseros">
                  <td>{{ mesero.mesero }}</td>
                  <td>{{ mesero.mesasAtendidas }}</td>
                  <td>{{ mesero.totalFacturado | currency:'COP':'symbol':'1.0-0' }}</td>
                  <td>{{ mesero.promedioPorMesa | currency:'COP':'symbol':'1.0-0' }}</td>
                  <td>{{ mesero.mesasActivas }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <section class="grid-2">
          <article class="card data-card">
            <div class="section-head">
              <h3>Pedidos en produccion</h3>
              <div class="section-meta">
                <span>{{ data.pedidosProduccion.totalActivos }} pedidos en cocina</span>
                <span>Tiempo promedio: {{ data.pedidosProduccion.promedioMinutos }} min</span>
              </div>
            </div>
            <div class="list-stack">
              <article
                class="pedido-card"
                *ngFor="let pedido of data.pedidosProduccion.pedidos"
                [class.pedido-warn]="getUrgencyClass(pedido, data.pedidosProduccion.promedioMinutos) === 'warn'"
                [class.pedido-urgent]="getUrgencyClass(pedido, data.pedidosProduccion.promedioMinutos) === 'urgent'"
              >
                <div>
                  <strong>{{ pedido.cliente }}</strong>
                  <p class="muted">Mesa {{ pedido.mesa }} · {{ pedido.minutosTranscurridos }} min</p>
                </div>
                <p class="muted">{{ pedido.items.join(', ') }}</p>
              </article>
            </div>
          </article>

          <article class="card data-card">
            <div class="section-head">
              <h3>Pedidos listos para servir</h3>
            </div>
            <div class="list-stack">
              <article class="pedido-card" *ngFor="let pedido of data.pedidosListos">
                <div>
                  <strong>{{ pedido.cliente }}</strong>
                  <p class="muted">Mesa {{ pedido.mesa }}</p>
                </div>
                <p class="muted">{{ pedido.items.join(', ') }}</p>
              </article>
            </div>
          </article>
        </section>

        <section class="grid-2">
          <article class="card data-card">
            <div class="section-head">
              <h3>Personal en turno</h3>
            </div>
            <p class="muted" style="margin-top: 0;">{{ data.personalTurno.resumen }}</p>
            <div class="list-stack">
              <article class="personal-group" *ngFor="let grupo of data.personalTurno.grupos">
                <div class="personal-head">
                  <strong>{{ grupo.rol }}</strong>
                  <span class="badge">{{ grupo.total }}</span>
                </div>
                <div class="list-grid">
                  <div class="list-row" *ngFor="let persona of grupo.personal">
                    <span>{{ persona.nombre }}</span>
                    <span class="muted" *ngIf="persona.mesasActivas !== undefined">Mesas activas: {{ persona.mesasActivas }}</span>
                  </div>
                </div>
              </article>
            </div>
          </article>

          <article class="card data-card">
            <div class="section-head">
              <h3>Ocupacion actual</h3>
            </div>
            <div class="list-grid">
              <div class="list-row">
                <span>Ocupacion actual (con check-in)</span>
                <span>{{ data.ocupacion.ocupadas }} mesas</span>
              </div>
              <div class="list-row">
                <span>Reservas confirmadas sin llegar</span>
                <span>{{ data.ocupacion.reservasPendientes }} mesas</span>
              </div>
            </div>
          </article>
        </section>
      </ng-container>

      <ng-template #ventasEmpty>
        <p class="muted">Sin datos de ventas para mostrar.</p>
      </ng-template>
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

      .state-card {
        padding: 0.9rem;
      }

      .state-card p {
        margin: 0;
      }

      .summary-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 0.8rem;
      }

      .summary-card {
        padding: 0.9rem;
        display: grid;
        gap: 0.35rem;
      }

      .summary-card strong {
        font-size: 1.2rem;
      }

      .trend-up strong {
        color: #137333;
      }

      .trend-down strong {
        color: #b42318;
      }

      .grid-2 {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
        gap: 0.8rem;
      }

      .data-card {
        padding: 0.9rem;
      }

      .section-head {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 0.6rem;
        margin-bottom: 0.6rem;
      }

      .section-head h3 {
        margin: 0;
      }

      .section-meta {
        display: grid;
        gap: 0.2rem;
        font-size: 0.82rem;
        color: var(--muted);
        text-align: right;
      }

      .list-grid {
        display: grid;
        gap: 0.4rem;
      }

      .list-row {
        display: flex;
        justify-content: space-between;
        gap: 0.6rem;
        font-size: 0.9rem;
      }

      .list-stack {
        display: grid;
        gap: 0.6rem;
      }

      .pedido-card {
        padding: 0.7rem;
        border-radius: 10px;
        border: 1px solid rgba(15, 23, 42, 0.08);
        background: rgba(255, 255, 255, 0.8);
        display: grid;
        gap: 0.25rem;
      }

      .pedido-warn {
        border-color: rgba(217, 119, 6, 0.4);
        background: rgba(254, 243, 199, 0.65);
      }

      .pedido-urgent {
        border-color: rgba(220, 38, 38, 0.45);
        background: rgba(254, 226, 226, 0.7);
      }

      .table-wrapper {
        overflow: auto;
      }

      table {
        width: 100%;
        border-collapse: collapse;
        min-width: 720px;
      }

      th,
      td {
        text-align: left;
        padding: 0.5rem;
        border-bottom: 1px solid rgba(10, 10, 10, 0.1);
        font-size: 0.84rem;
      }

      th {
        font-size: 0.78rem;
        color: var(--muted);
      }

      .personal-group {
        display: grid;
        gap: 0.4rem;
      }

      .personal-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.6rem;
      }

      .badge {
        border: 1px solid rgba(111, 78, 55, 0.35);
        color: #6f4e37;
        border-radius: 999px;
        padding: 0.1rem 0.5rem;
        font-size: 0.75rem;
        font-weight: 600;
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

        table {
          min-width: 640px;
        }
      }
    `
  ]
})
export class AdminDashboardPageComponent implements OnInit {
    readonly dashboard = signal<AdminDashboardData | null>(null);
    readonly loading = signal(true);
    readonly errorMessage = signal('');

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
      this.dashboardService.getAdminDashboard().subscribe({
        next: (data) => {
          this.dashboard.set(data);
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('No fue posible cargar el panel de control.');
          this.loading.set(false);
        },
      });
    }

    hasVentas(): boolean {
      return (this.dashboard()?.ventasDelDia.totalVentas ?? 0) > 0;
    }

    formatMetodoPago(metodo: AdminDashboardMetodoPago['metodo']): string {
      if (metodo === 'CASH') {
        return 'Efectivo';
      }
      if (metodo === 'CARD') {
        return 'Tarjeta';
      }
      return 'Nequi';
    }

    formatPercent(value: number, total: number): string {
      if (!total) {
        return '0%';
      }
      const percent = Math.round((value / total) * 100);
      return `${percent}%`;
    }

    formatVariacion(variacion: number): string {
      const sign = variacion >= 0 ? '+' : '';
      return `vs. ayer: ${sign}${variacion}%`;
    }

    getUrgencyClass(pedido: AdminDashboardPedidoProduccion, promedio: number): 'normal' | 'warn' | 'urgent' {
      if (pedido.minutosTranscurridos > promedio + 10) {
        return 'urgent';
      }
      if (pedido.minutosTranscurridos > promedio) {
        return 'warn';
      }
      return 'normal';
  }
}
