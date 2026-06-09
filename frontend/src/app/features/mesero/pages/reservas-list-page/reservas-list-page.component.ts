import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { ReservationService, ReservationDetailData } from '../../../../core/services/reservation.service';
import { MesaMapService } from '../../../../core/services/mesa-map.service';
import { Subscription, catchError, finalize, of, timer } from 'rxjs';
import { Reserva } from '../../../../core/models/domain.models';

interface ReservaCard extends Reserva {
  mostrarBotonInasistencia: boolean;
}

interface ZonaGroup {
  zonaId?: string;
  zonaNombre: string;
  cantidadReservas: number;
  reservas: ReservaCard[];
}

@Component({
  selector: 'app-reservas-list-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid reservas-shell">
      <app-page-header title="Reservas"></app-page-header>

      <!-- Search bar -->
      <div class="search-bar">
        <input
          class="search-input input-field"
          type="date"
          [(ngModel)]="searchDate"
          (ngModelChange)="onDateChange()"
        />
        <input
          class="search-input input-field"
          type="text"
          placeholder="Buscar por nombre del cliente o ID..."
          [(ngModel)]="searchQuery"
          (ngModelChange)="onSearchChange()"
        />
      </div>

      <!-- Loading -->
      <p class="empty-note" *ngIf="loading">Cargando reservas...</p>

      <!-- Empty state -->
      <p class="empty-note" *ngIf="!loading && message">{{ message }}</p>

      <!-- Zone groups -->
      <ng-container *ngIf="!loading && zonaGroups.length">
        <section class="zona-group" *ngFor="let zona of zonaGroups">
          <div class="zona-header">
            <strong>Zona: {{ zona.zonaNombre }}</strong>
            <span class="zona-count">{{ zona.cantidadReservas }} reserva(s)</span>
          </div>

          <article class="reserva-card card" *ngFor="let r of zona.reservas">
            <div class="reserva-card-head">
              <div>
                <p class="reserva-name">{{ r.guestName }}</p>
                <p class="reserva-meta">
                  <span *ngIf="r.phone">Tel: {{ r.phone }}</span>
                  <span *ngIf="r.zoneName"> | Zona: {{ r.zoneName }}</span>
                  <span *ngIf="r.decorationName"> | Decoración: {{ r.decorationName }}</span>
                </p>
              </div>
              <span
                class="status-pill"
                [class.is-confirmed]="r.status === 'CONFIRMED'"
                [class.is-pending]="r.status === 'PENDING'"
              >
                <span class="status-icon" *ngIf="r.status === 'CONFIRMED'">&#10004;</span>
                <span class="status-icon" *ngIf="r.status === 'PENDING'">&#9201;</span>
                {{ r.status === 'CONFIRMED' ? 'Confirmada' : 'Pendiente' }}
              </span>
            </div>
            <div class="reserva-card-body">
              <p>Hora de llegada: {{ r.time }}</p>
              <p>Número de personas: {{ r.guests }}</p>
            </div>
            <div class="reserva-card-actions">
              <button class="card-btn" (click)="verDetalle(r.id)">&#128065; Ver</button>
              <button
                class="card-btn card-btn-white"
                [class.card-btn-disabled]="r.status !== 'CONFIRMED'"
                (click)="onMarcarLlegada(r)"
              >
                &#9881; Marcar llegada
              </button>
              <button
                class="card-btn card-btn-danger"
                *ngIf="shouldShowInasistencia(r)"
                (click)="onConfirmarInasistencia(r)"
              >
                &#9888; Marcar inasistencia
              </button>
            </div>
          </article>
        </section>
      </ng-container>

      <!-- Toast -->
      <div class="toast-bar" *ngIf="toastMessage" [class.toast-danger]="toastDanger">
        {{ toastMessage }}
      </div>

      <!-- Detail modal -->
      <div *ngIf="showDetail" class="modal-backdrop" (click)="cerrarDetalle()">
        <div class="modal-card" (click)="$event.stopPropagation()">
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
                <li *ngFor="let item of detailData.reservation.preorderItems">{{ item.productName }} - {{ item.quantity }}</li>
              </ul>
            </div>

            <div *ngIf="detailData.payments.length">
              <h4>Abonos</h4>
              <ul>
                <li *ngFor="let p of detailData.payments">{{ p.method }} - {{ p.amount }} - {{ p.paidAt }}</li>
              </ul>
            </div>

            <p *ngIf="detailData.reservation.notes"><strong>Notas:</strong> {{ detailData.reservation.notes }}</p>
            <p><strong>Estado:</strong> {{ detailData.reservation.status }}</p>
          </div>
          <div class="modal-actions">
            <button class="card-btn" (click)="cerrarDetalle()">Cerrar</button>
          </div>
        </div>
      </div>

      <!-- Confirmation Inasistencia modal -->
      <div *ngIf="confirmInasistenciaOpen" class="modal-backdrop" (click)="cancelarInasistencia()">
        <div class="modal-card" (click)="$event.stopPropagation()">
          <h3>Confirmar Inasistencia</h3>
          <p class="modal-body">¿Está seguro que desea cancelar la reserva de <strong>{{ inasistenciaReserva?.guestName }}</strong> por inasistencia? Esto liberará la zona y decoración asociadas.</p>
          <div class="modal-actions">
            <button class="card-btn" (click)="cancelarInasistencia()">Cancelar</button>
            <button class="btn-primary card-btn-danger" (click)="ejecutarInasistencia()">Confirmar</button>
          </div>
        </div>
      </div>


    </section>
  `,
  styles: [
    `
      :host { display: block; }

      .reservas-shell { gap: 1rem; }

      .search-bar {
        display: flex;
        gap: 0.5rem;
        align-items: center;
      }
      .search-input {
        flex: 1;
        min-width: 0;
      }
      .search-btn {
        white-space: nowrap;
        padding: 0.55rem 1.2rem;
        border-radius: 9px;
        font-size: 0.9rem;
      }

      .empty-note {
        font-size: 0.88rem;
        color: var(--muted);
        margin: 0;
      }

      /* Zone group */
      .zona-group { display: grid; gap: 0.6rem; }
      .zona-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 0.92rem;
      }
      .zona-count {
        font-size: 0.82rem;
        color: var(--muted);
      }

      /* Reserva card */
      .reserva-card {
        padding: 0.85rem;
        display: grid;
        gap: 0.5rem;
      }
      .reserva-card-head {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 0.5rem;
      }
      .reserva-name {
        margin: 0;
        font-weight: 700;
        font-size: 0.95rem;
      }
      .reserva-meta {
        margin: 0.1rem 0 0;
        font-size: 0.78rem;
        color: var(--muted);
      }
      .reserva-card-body {
        display: grid;
        gap: 0.15rem;
        font-size: 0.84rem;
        color: var(--muted);
      }
      .reserva-card-body p { margin: 0; }
      .reserva-card-actions {
        display: flex;
        gap: 0.4rem;
        flex-wrap: wrap;
        padding-top: 0.3rem;
        border-top: 1px solid rgba(92, 58, 33, 0.12);
      }

      /* Card buttons */
      .card-btn {
        flex: 1;
        min-width: 100px;
        border: 1px solid rgba(92, 58, 33, 0.24);
        background: var(--bg);
        color: var(--text);
        border-radius: 9px;
        padding: 0.42rem 0.6rem;
        font-size: 0.82rem;
        cursor: pointer;
        text-align: center;
        white-space: nowrap;
      }
      .card-btn:hover { background: var(--surface); }
      .card-btn-white {
        background: #ffffff;
        color: #000000;
        border-color: #ffffff;
      }
      .card-btn-white:hover {
        background: #e0e0e0;
        color: #000000;
      }
      .card-btn.card-btn-disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
      .card-btn-danger {
        color: #8a2a2a;
        border-color: rgba(138, 42, 42, 0.35);
        background: #fdf2f2;
      }
      .card-btn-danger:hover { background: #f7e2e2; }

      /* Status pill */
      .status-pill {
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
        border-radius: 999px;
        padding: 0.18rem 0.6rem;
        font-size: 0.74rem;
        font-weight: 600;
        white-space: nowrap;
      }
      .status-icon { font-size: 0.72rem; }
      .status-pill.is-confirmed {
        background: #e9f5ee;
        color: #2d6a4f;
      }
      .status-pill.is-pending {
        background: #f5f0e8;
        color: #7a5a2b;
      }

      /* Toast */
      .toast-bar {
        position: fixed;
        bottom: 1.2rem;
        left: 50%;
        transform: translateX(-50%);
        background: var(--primary);
        color: #ffffff;
        padding: 0.6rem 1.4rem;
        border-radius: 10px;
        font-size: 0.86rem;
        z-index: 50;
        box-shadow: 0 4px 18px rgba(0,0,0,0.22);
        max-width: 90vw;
        text-align: center;
      }
      .toast-bar.toast-danger {
        background: #5B3F2C;
      }

      /* Modal shared */
      .modal-backdrop {
        position: fixed;
        inset: 0;
        display: grid;
        place-items: center;
        padding: 1rem;
        background: rgba(20, 12, 8, 0.45);
        z-index: 40;
      }
      .modal-card {
        width: min(560px, 94vw);
        background: #ffffff;
        border-radius: 14px;
        padding: 1rem;
        display: grid;
        gap: 0.6rem;
      }
      .modal-card h3 { margin: 0; font-size: 1.05rem; }
      .modal-subtitle {
        margin: 0;
        font-size: 0.82rem;
        color: var(--muted);
      }
      .modal-body p, .modal-body {
        margin: 0.2rem 0;
        font-size: 0.84rem;
      }
      .modal-body ul {
        margin: 0.3rem 0 0;
        padding-left: 1.1rem;
        font-size: 0.8rem;
      }
      .modal-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
        flex-wrap: wrap;
        margin-top: 0.3rem;
      }



      @media (max-width: 640px) {
        .search-bar { flex-direction: column; }
        .search-btn { width: 100%; }
        .reserva-card-actions { flex-direction: column; }
        .card-btn { min-width: 0; }
        .modal-actions { flex-direction: column; }
        .modal-actions button { width: 100%; }
      }
    `
  ],
})
export class ReservasListPageComponent implements OnDestroy {
  searchDate = this.getTodayIsoDate();
  searchQuery = '';
  
  todasLasReservas: ReservaCard[] = [];
  resumenZonasBase: Array<{ zonaId?: string; zonaNombre: string; cantidadReservas: number }> = [];
  
  zonaGroups: ZonaGroup[] = [];
  loading = false;
  message = '';

  // Toast
  toastMessage = '';
  toastDanger = false;
  private toastTimer?: ReturnType<typeof setTimeout>;

  // Detail modal
  showDetail = false;
  detailData?: ReservationDetailData;

  // Inasistencia modal
  confirmInasistenciaOpen = false;
  inasistenciaReserva: ReservaCard | null = null;



  private pollingSub?: Subscription;

  constructor(
    private reservationService: ReservationService,
    private mesaService: MesaMapService,
    private fb: FormBuilder,
    private router: Router,
  ) {
    this.buscar();
    this.pollingSub = timer(10000, 10000).subscribe(() => this.buscar(false));
  }

  onDateChange(): void {
    this.buscar(true);
  }

  onSearchChange(): void {
    this.filtrarEnMemoria();
  }

  buscar(showLoading = true): void {
    this.message = '';
    if (showLoading) this.loading = true;

    this.reservationService.listForMesero(this.searchDate).pipe(
      catchError((error: HttpErrorResponse) => {
        this.todasLasReservas = [];
        this.resumenZonasBase = [];
        if (error.status === 404) {
          this.message = 'No hay reservas programadas para esta fecha';
        } else if (error.status === 401) {
          this.message = 'Tu sesión no tiene permisos para ver reservas';
        } else {
          this.message = 'Error cargando reservas';
        }
        return of({ reservas: [], resumenZonas: [] });
      }),
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: (data) => {
        this.todasLasReservas = data.reservas.map((r: any) => ({
          ...r,
          mostrarBotonInasistencia: r.mostrarBotonInasistencia ?? undefined,
        }));
        this.resumenZonasBase = data.resumenZonas || [];
        
        this.filtrarEnMemoria();

        if (this.todasLasReservas.length === 0 && !this.message) {
          this.message = 'No hay reservas programadas para esta fecha';
        }
      },
    });
  }

  private filtrarEnMemoria(): void {
    let filtradas = this.todasLasReservas;
    const q = this.searchQuery.trim().toLowerCase();
    
    if (q) {
      filtradas = filtradas.filter(r => 
        r.guestName?.toLowerCase().includes(q) || 
        r.phone?.includes(q) ||
        r.id?.includes(q)
      );
    }
    
    this.zonaGroups = this.groupByZona(filtradas, this.resumenZonasBase);
    
    if (this.todasLasReservas.length > 0 && filtradas.length === 0) {
      this.message = 'No se encontraron reservas con ese nombre o identificador';
    } else if (this.todasLasReservas.length > 0) {
      this.message = '';
    }
  }

  verDetalle(reservaId: string): void {
    this.reservationService.getDetail(reservaId).subscribe({
      next: (data) => { this.detailData = data; this.showDetail = true; },
      error: () => { this.detailData = undefined; this.showDetail = false; },
    });
  }

  cerrarDetalle(): void {
    this.showDetail = false;
    this.detailData = undefined;
  }

  onMarcarLlegada(reserva: ReservaCard): void {
    if (reserva.status !== 'CONFIRMED') {
      this.showToast('La reserva no ha sido confirmada por el administrador. No es posible marcar llegada', true);
      return;
    }

    this.reservationService.getDetail(reserva.id).subscribe({
      next: (detail) => {
        const reservationDate = detail.reservation.date;
        const today = this.getTodayIsoDate();

        if (reservationDate < today) {
          this.showToast('No es posible marcar llegada para reservas que ya pasaron');
          return;
        }

        if (reservationDate > today) {
          this.showToast('No es posible marcar llegada para reservas futuras');
          return;
        }

        this.router.navigate(['/app/mesero/llegada-reserva'], {
          state: {
            openAsignacion: true,
            origen: 'reservas',
            reservaId: reserva.id,
            numeroPersonas: reserva.guests,
            zonaId: reserva.zoneId
          }
        });
      },
      error: () => {
        this.showToast('No se pudo validar la fecha de la reserva', true);
      },
    });
  }



  onConfirmarInasistencia(reserva: ReservaCard): void {
    this.inasistenciaReserva = reserva;
    this.confirmInasistenciaOpen = true;
  }

  cancelarInasistencia(): void {
    this.confirmInasistenciaOpen = false;
    this.inasistenciaReserva = null;
  }

  ejecutarInasistencia(): void {
    if (!this.inasistenciaReserva) return;
    
    this.reservationService.marcarInasistencia(this.inasistenciaReserva.id).subscribe({
      next: () => {
        this.showToast('Reserva cancelada por inasistencia');
        this.confirmInasistenciaOpen = false;
        this.inasistenciaReserva = null;
        this.buscar(false);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as any)?.message || 'No se pudo marcar inasistencia';
        this.showToast(msg, true);
        this.confirmInasistenciaOpen = false;
      },
    });
  }

  ngOnDestroy(): void {
    this.pollingSub?.unsubscribe();
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  private groupByZona(
    reservas: ReservaCard[],
    resumenZonas: Array<{ zonaId?: string; zonaNombre: string; cantidadReservas: number }>
  ): ZonaGroup[] {
    const map = new Map<string, ZonaGroup>();
    for (const z of resumenZonas) {
      map.set(z.zonaNombre, { ...z, reservas: [] });
    }
    for (const r of reservas) {
      const key = r.zoneName || 'Sin asignar';
      let group = map.get(key);
      if (!group) {
        group = { zonaId: r.zoneId, zonaNombre: key, cantidadReservas: 0, reservas: [] };
        map.set(key, group);
      }
      group.reservas.push(r);
    }
    // Update the counts based on the filtered list
    const result = Array.from(map.values()).filter(g => g.reservas.length > 0);
    result.forEach(g => {
      g.cantidadReservas = g.reservas.length;
    });
    return result;
  }



  shouldShowInasistencia(reserva: ReservaCard): boolean {
    // 1. If backend explicitly says yes, show it
    if (reserva.mostrarBotonInasistencia) {
      return true;
    }

    // 2. Must be confirmed to have an absence marked
    if (reserva.status !== 'CONFIRMED') {
      return false;
    }

    // 3. Client-side fallback check
    const dateStr = reserva.date || this.getTodayIsoDate();
    let timeStr = reserva.time || '00:00';
    if (timeStr.length > 5) {
      timeStr = timeStr.substring(0, 5);
    }
    
    // Parse manually to avoid timezone weirdness
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      const year = parseInt(parts[0], 10);
      const month = parseInt(parts[1], 10) - 1;
      const day = parseInt(parts[2], 10);
      
      const timeParts = timeStr.split(':');
      const hours = parseInt(timeParts[0] || '0', 10);
      const mins = parseInt(timeParts[1] || '0', 10);
      
      const target = new Date(year, month, day, hours, mins, 0);
      const minutesElapsed = (Date.now() - target.getTime()) / 60000;
      
      return minutesElapsed > 30;
    }

    return false;
  }



  private getTodayIsoDate(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private showToast(msg: string, danger = false): void {
    this.toastMessage = msg;
    this.toastDanger = danger;
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => { this.toastMessage = ''; }, 4000);
  }
}
