import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { ReservationService, ReservationDetailData } from '../../../../core/services/reservation.service';
import { MesaMapService, MesaZonaDisponible, MesaAsignacionPayload } from '../../../../core/services/mesa-map.service';
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
          type="text"
          placeholder="Buscar por identificador o fecha (YYYY-MM-DD)"
          [(ngModel)]="searchQuery"
          (keyup.enter)="buscar()"
        />
        <button class="btn-primary search-btn" (click)="buscar()">Buscar</button>
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
                <p class="reserva-meta">ID: R-{{ r.id }}</p>
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
              <p>Mesa: Sin asignar mesa</p>
              <p>Hora de llegada: {{ r.time }}</p>
              <p>Numero de personas: {{ r.guests }}</p>
            </div>
            <div class="reserva-card-actions">
              <button class="card-btn" (click)="verDetalle(r.id)">&#128065; Ver</button>
              <button class="card-btn" (click)="onMarcarLlegada(r)">&#9881; Marcar llegada</button>
              <button
                class="card-btn card-btn-danger"
                *ngIf="r.mostrarBotonInasistencia"
                (click)="onMarcarInasistencia(r)"
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
            <p *ngIf="detailData.reservation.phone"><strong>Telefono:</strong> {{ detailData.reservation.phone }}</p>
            <p><strong>Fecha y hora:</strong> {{ detailData.reservation.date }} {{ detailData.reservation.time }}</p>
            <p><strong>Personas:</strong> {{ detailData.reservation.guests }}</p>
            <p *ngIf="detailData.reservation.decorationName"><strong>Decoracion:</strong> {{ detailData.reservation.decorationName }}</p>
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

      <!-- Asignacion sub-modal -->
      <div *ngIf="asignacionOpen" class="modal-backdrop" (click)="cancelarAsignacion()">
        <div class="modal-card asignacion-modal" (click)="$event.stopPropagation()">
          <h3>Asignar identificador de mesa</h3>
          <p class="modal-subtitle">Sub-modal de asignacion para check-in o agregar mesa.</p>

          <form [formGroup]="asignarForm" (ngSubmit)="submitAsignacion()">
            <label class="field-label">
              <span>Identificador</span>
              <input class="input-field" formControlName="mesaIdentificador" placeholder="Ej: MESA-12" maxlength="20" />
            </label>
            <small class="field-error" *ngIf="asignarForm.controls.mesaIdentificador.touched && asignarForm.controls.mesaIdentificador.hasError('required')">
              El identificador de mesa es obligatorio
            </small>
            <small class="field-error" *ngIf="asignacionError">{{ asignacionError }}</small>

            <div class="modal-actions">
              <button class="card-btn" type="button" (click)="cancelarAsignacion()">Cancelar</button>
              <button class="btn-primary" type="submit" [disabled]="asignacionSaving">
                {{ asignacionSaving ? 'Guardando...' : 'Guardar' }}
              </button>
            </div>
          </form>
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
      .modal-body p {
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

      /* Asignacion modal form */
      .asignacion-modal form {
        display: grid;
        gap: 0.6rem;
      }
      .field-label {
        display: grid;
        gap: 0.25rem;
        font-size: 0.84rem;
        font-weight: 600;
      }
      .field-error {
        color: #8a2a2a;
        font-size: 0.78rem;
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
  searchQuery = '';
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

  // Asignacion sub-modal
  asignacionOpen = false;
  asignacionSaving = false;
  asignacionError = '';
  private asignacionReserva: ReservaCard | null = null;
  private zonas: MesaZonaDisponible[] = [];

  readonly asignarForm = this.fb.nonNullable.group({
    mesaIdentificador: ['', [Validators.required, Validators.maxLength(20)]],
  });

  private pollingSub?: Subscription;

  constructor(
    private reservationService: ReservationService,
    private mesaService: MesaMapService,
    private fb: FormBuilder,
    private router: Router,
  ) {
    this.buscar();
    this.pollingSub = timer(30000, 30000).subscribe(() => this.buscar(false));
  }

  buscar(showLoading = true): void {
    this.message = '';
    if (showLoading) this.loading = true;

    let fechaParam: string | undefined;
    let identificadorParam: string | undefined;

    const q = this.searchQuery.trim();
    if (q) {
      if (/^\d{4}-\d{2}-\d{2}$/.test(q)) {
        fechaParam = q;
      } else if (/^\d+$/.test(q)) {
        identificadorParam = q;
      } else {
        fechaParam = q;
      }
    }

    this.reservationService.listForMesero(fechaParam, identificadorParam).pipe(
      catchError((error: HttpErrorResponse) => {
        this.zonaGroups = [];
        if (error.status === 404) {
          this.message = identificadorParam
            ? 'No se encontraron reservas activas con ese identificador'
            : 'No hay reservas programadas para esta fecha';
        } else if (error.status === 401) {
          this.message = 'Tu sesion no tiene permisos para ver reservas';
        } else {
          this.message = 'Error cargando reservas';
        }
        return of({ reservas: [], resumenZonas: [] });
      }),
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: (data) => {
        const reservas: ReservaCard[] = data.reservas.map((r: any) => ({
          ...r,
          mostrarBotonInasistencia: r.mostrarBotonInasistencia ?? false,
        }));

        this.zonaGroups = this.groupByZona(reservas, data.resumenZonas);

        if (reservas.length === 0 && !this.message) {
          this.message = 'No hay reservas programadas para esta fecha';
        }
      },
    });
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
    if (reserva.status === 'PENDING') {
      this.showToast('La reserva no ha sido confirmada por el administrador. No es posible marcar llegada');
      return;
    }
    this.asignacionReserva = reserva;
    this.asignacionError = '';
    this.asignarForm.reset({ mesaIdentificador: '' });
    this.asignacionOpen = true;
    this.cargarZonas();
  }

  cancelarAsignacion(): void {
    this.asignacionOpen = false;
    this.asignacionReserva = null;
  }

  submitAsignacion(): void {
    if (this.asignarForm.invalid) {
      this.asignarForm.markAllAsTouched();
      return;
    }
    const reserva = this.asignacionReserva;
    if (!reserva) return;

    const zonaId = this.resolveZonaId(reserva);
    if (!zonaId) {
      this.asignacionError = 'No se pudo determinar la zona para esta reserva.';
      return;
    }

    const payload: MesaAsignacionPayload = {
      mesaIdentificador: this.asignarForm.getRawValue().mesaIdentificador.trim(),
      zonaId,
      numeroPersonas: reserva.guests,
      reservaId: reserva.id,
    };

    this.asignacionSaving = true;
    this.asignacionError = '';
    this.mesaService.asignarMesa(payload).subscribe({
      next: () => {
        this.asignacionSaving = false;
        this.asignacionOpen = false;
        this.asignacionReserva = null;
        this.showToast('Mesa asignada correctamente');
        this.buscar(false);
      },
      error: (err: HttpErrorResponse) => {
        this.asignacionSaving = false;
        const msg = (err.error as any)?.message;
        this.asignacionError = msg || 'No se pudo asignar la mesa.';
      },
    });
  }

  onMarcarInasistencia(reserva: ReservaCard): void {
    this.reservationService.marcarInasistencia(reserva.id).subscribe({
      next: () => {
        this.showToast('Reserva cancelada por inasistencia');
        this.buscar(false);
      },
      error: (err: HttpErrorResponse) => {
        const msg = (err.error as any)?.message || 'No se pudo marcar inasistencia';
        this.showToast(msg, true);
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
    return Array.from(map.values()).filter(g => g.reservas.length > 0);
  }

  private cargarZonas(): void {
    this.mesaService.getZonasDisponibles().subscribe({
      next: (z) => { this.zonas = z; },
      error: () => { this.zonas = []; },
    });
  }

  private resolveZonaId(reserva: ReservaCard): string | null {
    if (reserva.zoneId) return reserva.zoneId;
    if (this.zonas.length === 1) return this.zonas[0].id;
    return this.zonas.length > 0 ? this.zonas[0].id : null;
  }

  private showToast(msg: string, danger = false): void {
    this.toastMessage = msg;
    this.toastDanger = danger;
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => { this.toastMessage = ''; }, 4000);
  }
}
