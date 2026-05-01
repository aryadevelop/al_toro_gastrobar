import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { ReservationService, ReservationDetailData } from '../../../../core/services/reservation.service';
import { Subscription, catchError, finalize, of, timer } from 'rxjs';

@Component({
  selector: 'app-reservas-list-page',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid reservas-shell">
      <app-page-header title="Reservas"></app-page-header>

      <article class="card card-tight filters-card">
        <div class="filters-row">
          <label class="filter-field">
            <span>Fecha</span>
            <input type="date" [(ngModel)]="fecha" (keyup.enter)="buscar()" />
          </label>

          <label class="filter-field">
            <span>Identificador</span>
            <input type="text" placeholder="ID reserva" [(ngModel)]="identificador" (keyup.enter)="buscar()" />
          </label>

          <div class="filters-actions">
            <button class="action-btn primary" (click)="buscar()">Buscar</button>
            <button class="action-btn ghost" (click)="limpiar()">Limpiar</button>
          </div>
        </div>
      </article>

      <article class="card card-tight summary-card">
        <div class="section-head">
          <h3 class="section-title">Resumen por zona</h3>
          <span class="section-meta" *ngIf="resumenZonas.length">{{ resumenZonas.length }} zonas</span>
        </div>
        <div class="summary-grid" *ngIf="resumenZonas.length; else noSummary">
          <div class="summary-pill" *ngFor="let z of resumenZonas">
            <span>{{ z.zonaNombre }}</span>
            <strong>{{ z.cantidadReservas }}</strong>
          </div>
        </div>
        <ng-template #noSummary><p class="empty-note">No hay resumen disponible.</p></ng-template>
      </article>

      <article class="card card-tight reservas-card">
        <div class="section-head">
          <h3 class="section-title">Reservas</h3>
          <span class="section-meta" *ngIf="!loading && reservas.length">{{ reservas.length }} reservas</span>
        </div>
        <div class="loading-note" *ngIf="loading">Cargando reservas...</div>
        <div *ngIf="!loading && reservas.length; else empty">
          <table class="reservas-table">
            <thead>
              <tr>
                <th>Hora</th>
                <th>Cliente</th>
                <th>Teléfono</th>
                <th>Personas</th>
                <th>Zona / Decoración</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let r of reservas">
                <td data-label="Hora">{{ r.time }}</td>
                <td data-label="Cliente">{{ r.guestName }}</td>
                <td data-label="Teléfono">{{ r.phone || '-' }}</td>
                <td data-label="Personas">{{ r.guests }}</td>
                <td data-label="Zona / Decoración">{{ r.zoneName || r.decorationName || 'Sin asignar' }}</td>
                <td data-label="Estado">
                  <span
                    class="status-pill"
                    [class.is-confirmed]="r.status === 'CONFIRMED'"
                    [class.is-pending]="r.status === 'PENDING'"
                    [class.is-arrived]="r.status === 'ARRIVED'"
                    [class.is-cancelled]="r.status === 'CANCELLED'"
                  >
                    {{ r.status }}
                  </span>
                </td>
                <td data-label="Acciones">
                  <div class="row-actions">
                    <button class="action-btn ghost" (click)="verDetalle(r.id)">Ver</button>
                    <button class="action-btn primary" (click)="marcarLlegada(r.id)">Marcar llegada</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <ng-template #empty>
          <p class="empty-note" *ngIf="message">{{ message }}</p>
          <p class="empty-note" *ngIf="!message">No se encontraron reservas para los parámetros especificados.</p>
        </ng-template>
      </article>

      <!-- Detail modal -->
      <div *ngIf="showDetail" class="modal-backdrop">
        <div class="modal-card">
          <h3>Detalle de la reserva</h3>
          <div *ngIf="detailData" class="modal-body">
            <p><strong>Cliente:</strong> {{ detailData.reservation.guestName }}</p>
            <p *ngIf="detailData.reservation.phone"><strong>Teléfono:</strong> {{ detailData.reservation.phone }}</p>
            <p><strong>Fecha y hora:</strong> {{ detailData.reservation.date }} {{ detailData.reservation.time }}</p>
            <p><strong>Personas:</strong> {{ detailData.reservation.guests }}</p>
            <p *ngIf="detailData.reservation.decorationName"><strong>Decoración:</strong> {{ detailData.reservation.decorationName }}</p>
            <p *ngIf="detailData.reservation.zoneName"><strong>Zona:</strong> {{ detailData.reservation.zoneName }}</p>

            <div *ngIf="(detailData.reservation.preorderItems || []).length">
              <h4>Pre-orden</h4>
              <ul>
                  <li *ngFor="let item of detailData.reservation.preorderItems">{{ item.productName }} — {{ item.quantity }}</li>
              </ul>
            </div>

            <div *ngIf="detailData.payments.length">
              <h4>Abonos</h4>
              <ul>
                <li *ngFor="let p of detailData.payments">{{ p.method }} — {{ p.amount }} — {{ p.paidAt }}</li>
              </ul>
            </div>

            <p *ngIf="detailData.reservation.notes"><strong>Notas:</strong> {{ detailData.reservation.notes }}</p>
            <p><strong>Estado:</strong> {{ detailData.reservation.status }}</p>
          </div>

          <div class="modal-actions">
            <button class="action-btn primary" *ngIf="detailData" (click)="marcarLlegada(detailData.reservation.id)">Marcar llegada</button>
            <button class="action-btn ghost" (click)="cerrarDetalle()">Cerrar</button>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .reservas-shell {
        font-family: 'Manrope', 'Montserrat', sans-serif;
        gap: 0.75rem;
      }

      .card.card-tight {
        padding: 0.75rem;
        border-radius: 12px;
        border: 1px solid rgba(44, 24, 16, 0.12);
        background: #ffffff;
      }

      .filters-row {
        display: flex;
        flex-wrap: wrap;
        gap: 0.6rem;
        align-items: flex-end;
      }

      .filter-field {
        display: grid;
        gap: 0.3rem;
        font-size: 0.78rem;
        color: #3b2a1f;
      }

      .filter-field input {
        min-width: 180px;
        padding: 0.4rem 0.55rem;
        border: 1px solid rgba(44, 24, 16, 0.2);
        border-radius: 8px;
        font-size: 0.82rem;
      }

      .filters-actions {
        display: flex;
        gap: 0.4rem;
      }

      .action-btn {
        border: 1px solid rgba(44, 24, 16, 0.2);
        background: #ffffff;
        color: #3b2a1f;
        border-radius: 8px;
        padding: 0.38rem 0.7rem;
        font-size: 0.78rem;
        cursor: pointer;
      }

      .action-btn.primary {
        background: #2c1810;
        color: #ffffff;
        border-color: #2c1810;
      }

      .action-btn.ghost {
        background: #fff7ef;
      }

      .section-head {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 0.5rem;
      }

      .section-title {
        margin: 0;
        font-size: 0.95rem;
      }

      .section-meta {
        font-size: 0.76rem;
        color: #6b4a3a;
      }

      .summary-grid {
        display: grid;
        gap: 0.4rem;
        grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
      }

      .summary-pill {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.4rem 0.55rem;
        border-radius: 9px;
        background: #f7efe7;
        font-size: 0.78rem;
      }

      .summary-pill strong {
        font-size: 0.82rem;
      }

      .loading-note,
      .empty-note {
        font-size: 0.8rem;
        color: #6b4a3a;
        margin: 0.4rem 0 0;
      }

      .reservas-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.8rem;
      }

      .reservas-table th,
      .reservas-table td {
        text-align: left;
        padding: 0.45rem 0.35rem;
        border-bottom: 1px solid rgba(44, 24, 16, 0.08);
        vertical-align: top;
      }

      .reservas-table th {
        font-size: 0.74rem;
        color: #6b4a3a;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.03em;
      }

      .status-pill {
        display: inline-flex;
        align-items: center;
        border-radius: 999px;
        padding: 0.12rem 0.5rem;
        font-size: 0.7rem;
        background: #f1e9e1;
        color: #4d3323;
      }

      .status-pill.is-confirmed {
        background: #e4f3ec;
        color: #1f5c42;
      }

      .status-pill.is-pending {
        background: #f7efe3;
        color: #8a5a2b;
      }

      .status-pill.is-arrived {
        background: #e5edf7;
        color: #2a4f8a;
      }

      .status-pill.is-cancelled {
        background: #f7e2e2;
        color: #8a2a2a;
      }

      .row-actions {
        display: flex;
        gap: 0.35rem;
        flex-wrap: wrap;
      }

      .modal-backdrop {
        position: fixed;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 1rem;
        background: rgba(20, 12, 8, 0.45);
        z-index: 30;
      }

      .modal-card {
        width: min(640px, 92vw);
        background: #ffffff;
        border-radius: 12px;
        padding: 0.85rem;
        display: grid;
        gap: 0.6rem;
      }

      .modal-body p {
        margin: 0.2rem 0;
        font-size: 0.82rem;
      }

      .modal-body ul {
        margin: 0.3rem 0 0;
        padding-left: 1.1rem;
        font-size: 0.78rem;
      }

      .modal-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.4rem;
        flex-wrap: wrap;
      }

      @media (max-width: 720px) {
        .filters-row {
          display: grid;
          gap: 0.5rem;
        }

        .filter-field input {
          min-width: 0;
          width: 100%;
        }

        .filters-actions {
          width: 100%;
        }

        .filters-actions .action-btn {
          flex: 1;
        }

        .reservas-table thead {
          display: none;
        }

        .reservas-table,
        .reservas-table tbody,
        .reservas-table tr,
        .reservas-table td {
          display: block;
          width: 100%;
        }

        .reservas-table tr {
          border: 1px solid rgba(44, 24, 16, 0.12);
          border-radius: 12px;
          padding: 0.55rem;
          margin-bottom: 0.55rem;
          background: #ffffff;
        }

        .reservas-table td {
          border: none;
          padding: 0.2rem 0;
          display: flex;
          justify-content: space-between;
          gap: 0.6rem;
        }

        .reservas-table td::before {
          content: attr(data-label);
          font-weight: 600;
          color: #6b4a3a;
        }

        .row-actions {
          justify-content: flex-start;
        }
      }
    `
  ],
})
export class ReservasListPageComponent implements OnDestroy {
  fecha = new Date().toISOString().slice(0, 10);
  identificador = '';

  reservas: Array<{
    id: string;
    guestName: string;
    guests: number;
    time: string;
    status: string;
    zoneName?: string;
    decorationName?: string;
    phone?: string;
  }> = [];
  resumenZonas: Array<{ zonaId?: string; zonaNombre: string; cantidadReservas: number }> = [];
  loading = false;
  message = '';

  // Detail modal
  showDetail = false;
  detailData?: ReservationDetailData;

  private pollingSub?: Subscription;

  constructor(private reservationService: ReservationService) {
    this.buscar();
    // Poll each 30s for real-time-ish updates
    this.pollingSub = timer(30000, 30000).subscribe(() => this.buscar(false));
  }

  buscar(showLoading = true): void {
    this.message = '';
    if (showLoading) this.loading = true;

    const fechaParam = this.fecha || undefined;
    const identificadorParam = this.identificador ? this.identificador.trim() : undefined;

    this.reservationService.listForMesero(fechaParam, identificadorParam).pipe(
      catchError((error: HttpErrorResponse) => {
        this.reservas = [];
        this.resumenZonas = [];

        if (error.status === 404) {
          this.message = identificadorParam
            ? 'No se encontraron reservas activas con ese identificador'
            : 'No hay reservas programadas para esta fecha';
        } else if (error.status === 401) {
          this.message = 'Tu sesión no tiene permisos para ver reservas';
        } else {
          this.message = 'Error cargando reservas';
        }

        return of({ reservas: [], resumenZonas: [] });
      }),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (data) => {
        this.reservas = data.reservas.map((r) => ({
          id: r.id,
          guestName: r.guestName,
          guests: r.guests,
          time: r.time,
          status: r.status,
          zoneName: r.zoneName,
          decorationName: r.decorationName,
          phone: r.phone,
        }));
        this.resumenZonas = data.resumenZonas;

        if (identificadorParam && this.reservas.length === 0) {
          this.message = 'No se encontraron reservas activas con ese identificador';
        } else if (!identificadorParam && this.reservas.length === 0) {
          this.message = 'No hay reservas programadas para esta fecha';
        }
      },
    });
  }

  limpiar(): void {
    this.fecha = new Date().toISOString().slice(0, 10);
    this.identificador = '';
    this.buscar();
  }

  verDetalle(reservaId: string): void {
    this.reservationService.getDetail(reservaId).subscribe({
      next: (data) => {
        this.detailData = data;
        this.showDetail = true;
      },
      error: () => {
        this.detailData = undefined;
        this.showDetail = false;
      },
    });
  }

  cerrarDetalle(): void {
    this.showDetail = false;
    this.detailData = undefined;
  }

  marcarLlegada(reservaId: string | undefined): void {
    if (!reservaId) return;
    this.reservationService.update(reservaId, { status: 'ARRIVED' }).subscribe({
      next: () => {
        this.buscar();
        this.cerrarDetalle();
      },
      error: () => {
        // ignore for now; could show toast
      },
    });
  }

  ngOnDestroy(): void {
    this.pollingSub?.unsubscribe();
  }
}
