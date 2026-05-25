import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, OnDestroy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription, catchError, finalize, of, timer } from 'rxjs';
import {
  RegisterAbonoPayload,
  ReservationDetailData,
  ReservationListCajeroItem,
  ReservationPaymentSummary,
  ReservationService,
} from '../../../../core/services/reservation.service';
import { WebSocketService } from '../../../../core/services/websocket.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

interface ZonaGroup {
  zonaId?: string;
  zonaNombre: string;
  cantidadReservas: number;
  reservas: ReservationListCajeroItem[];
}

type AbonoTipo = 'ANTICIPO' | 'DEVOLUCION';

@Component({
  selector: 'app-reservas-cajero-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, ConfirmDialogComponent],
  template: `
    <section class="page-grid reservas-shell">
      <app-page-header title="Reservas" subtitle="Gestión de reservas y abonos"></app-page-header>

      <div class="search-bar card">
        <input
          class="input-field"
          type="text"
          placeholder="Buscar por identificador o fecha (YYYY-MM-DD)"
          [value]="searchQuery"
          (input)="onSearchInput($event)"
          (keyup.enter)="buscar()"
        />
        <button class="btn-primary" type="button" (click)="buscar()">Buscar</button>
      </div>

      <p class="empty-note" *ngIf="loading">Cargando reservas...</p>
      <p class="empty-note" *ngIf="!loading && message">{{ message }}</p>

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
              <span class="status-pill" [ngClass]="statusClass(r.rawEstado)">
                {{ estadoLabel(r.rawEstado) }}
              </span>
            </div>

            <div class="reserva-card-body">
              <p>Hora de llegada: {{ r.time }}</p>
              <p>Número de personas: {{ r.guests }}</p>
              <p *ngIf="r.type">Tipo: {{ r.type === 'SPECIAL' ? 'Especial' : 'Básica' }}</p>
            </div>

            <div class="reserva-card-actions">
              <button class="card-btn" type="button" (click)="verDetalle(r)">Ver</button>
              <button
                class="card-btn"
                type="button"
                *ngIf="r.mostrarAgregarAnticipo"
                (click)="openAbonoForm(r, 'ANTICIPO')"
              >
                Agregar anticipo
              </button>
              <button
                class="card-btn card-btn-danger"
                type="button"
                *ngIf="r.mostrarAgregarDevolucion"
                (click)="openAbonoForm(r, 'DEVOLUCION')"
              >
                Agregar devolución
              </button>
            </div>
          </article>
        </section>
      </ng-container>

      <div class="toast-bar" *ngIf="toastMessage" [class.toast-danger]="toastDanger">
        {{ toastMessage }}
      </div>

      <div *ngIf="showDetail" class="modal-backdrop" (click)="cerrarDetalle()">
        <div class="modal-card" (click)="$event.stopPropagation()">
          <h3>Detalle de la reserva</h3>

          <div *ngIf="detailData" class="modal-body">
            <p><strong>Cliente:</strong> {{ detailData.reservation.guestName }}</p>
            <p><strong>Fecha y hora:</strong> {{ detailData.reservation.date }} {{ detailData.reservation.time }}</p>
            <p><strong>Personas:</strong> {{ detailData.reservation.guests }}</p>
            <p><strong>Estado:</strong> {{ estadoLabel(detailRawEstado) }}</p>
            <p><strong>Total abonado:</strong> {{ formatMoney(detailData.totalPaid) }}</p>
          </div>

          <div class="modal-actions" *ngIf="selectedReservaForDetail as reserva">
            <button class="card-btn" type="button" (click)="cerrarDetalle()">Cerrar</button>
            <button
              class="btn-primary"
              type="button"
              *ngIf="reserva.mostrarAgregarAnticipo"
              (click)="openAbonoForm(reserva, 'ANTICIPO')"
            >
              Agregar anticipo
            </button>
            <button
              class="card-btn card-btn-danger"
              type="button"
              *ngIf="reserva.mostrarAgregarDevolucion"
              (click)="openAbonoForm(reserva, 'DEVOLUCION')"
            >
              Agregar devolución
            </button>
          </div>
        </div>
      </div>

      <div *ngIf="abonoModalOpen" class="modal-backdrop" (click)="onCancelAbonoRequested()">
        <div class="modal-card abono-modal" (click)="$event.stopPropagation()">
          <h3>{{ abonoModalType === 'ANTICIPO' ? 'Registrar anticipo' : 'Registrar devolución' }}</h3>

          <form [formGroup]="abonoForm" (ngSubmit)="submitAbono()">
            <label class="field-label">
              <span>Tipo</span>
              <input class="input-field" [value]="abonoModalType === 'ANTICIPO' ? 'Anticipo' : 'Devolución'" disabled />
            </label>

            <label class="field-label">
              <span>Monto</span>
              <input class="input-field" type="number" min="0" step="0.01" formControlName="monto" />
            </label>
            <small class="field-error" *ngIf="showMontoRequiredError()">El monto es obligatorio</small>
            <small class="field-error" *ngIf="showMontoMinError()">El monto debe ser mayor a cero</small>
            <small class="field-error" *ngIf="showMontoMaxError()">{{ montoMaxMessage }}</small>

            <label class="field-label">
              <span>Método de pago</span>
              <select class="input-field" formControlName="metodo">
                <option value="">Selecciona una opción</option>
                <option value="EFECTIVO">Efectivo</option>
                <option value="TARJETA">Tarjeta</option>
                <option value="TRANSFERENCIA">Transferencia</option>
                <option value="OTRO">Otro</option>
              </select>
            </label>
            <small class="field-error" *ngIf="showMetodoRequiredError()">Debe seleccionar un método de pago</small>

            <label class="field-label">
              <span>Fecha y hora</span>
              <input class="input-field" type="datetime-local" formControlName="fechaHora" />
            </label>
            <small class="field-error" *ngIf="showFechaRequiredError()">Debe seleccionar una fecha</small>
            <small class="field-error" *ngIf="showFechaInvalidaError()">
              La fecha no puede ser futura ni anterior a la fecha de creación de la reserva
            </small>

            <section class="summary-panel" *ngIf="abonoResumen as resumen">
              <h4>Información de referencia</h4>
              <p><strong>Cliente:</strong> {{ resumen.clienteNombre }}</p>
              <p><strong>Fecha y hora reserva:</strong> {{ formatDateTime(resumen.fechaHoraLlegada) }}</p>
              <p><strong>Número de personas:</strong> {{ resumen.numeroPersonas }}</p>
              <p><strong>Total estimado reserva:</strong> {{ formatMoney(resumen.totalReserva) }}</p>

              <ng-container *ngIf="abonoModalType === 'ANTICIPO'">
                <p><strong>Total abonado:</strong> {{ formatMoney(resumen.totalAnticipado) }}</p>
                <p><strong>Pendiente por abonar:</strong> {{ formatMoney(resumen.pendientePorAbonar ?? 0) }}</p>
              </ng-container>

              <ng-container *ngIf="abonoModalType === 'DEVOLUCION'">
                <p><strong>Total abonado:</strong> {{ formatMoney(resumen.netoAbonado) }}</p>
                <p><strong>Total devuelto:</strong> {{ formatMoney(resumen.totalDevuelto) }}</p>
                <p><strong>Pendiente por devolver:</strong> {{ formatMoney(resumen.pendientePorDevolver ?? 0) }}</p>
              </ng-container>
            </section>

            <small class="field-error" *ngIf="abonoErrorMessage">{{ abonoErrorMessage }}</small>

            <div class="modal-actions">
              <button class="card-btn" type="button" (click)="onCancelAbonoRequested()">Cancelar</button>
              <button class="btn-primary" type="submit" [disabled]="abonoSaving">
                {{ abonoSaving ? 'Guardando...' : 'Guardar' }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <app-confirm-dialog
        [open]="showCancelConfirmDialog"
        title="Cancelar registro"
        message="¿Cancelar el registro? Los datos ingresados se perderán."
        cancelLabel="Volver"
        confirmLabel="Confirmar"
        (cancel)="showCancelConfirmDialog = false"
        (confirm)="confirmCancelAbono()"
      ></app-confirm-dialog>
    </section>
  `,
  styles: [
    `
      :host { display: block; }
      .reservas-shell { gap: 1rem; }
      .search-bar { padding: 0.8rem; display: flex; gap: 0.5rem; }
      .search-bar .input-field { flex: 1; min-width: 0; }
      .empty-note { margin: 0; color: var(--muted); font-size: 0.9rem; }

      .zona-group { display: grid; gap: 0.55rem; }
      .zona-header { display: flex; justify-content: space-between; align-items: center; }
      .zona-count { color: var(--muted); font-size: 0.8rem; }

      .reserva-card { padding: 0.85rem; display: grid; gap: 0.5rem; }
      .reserva-card-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 0.5rem; }
      .reserva-name { margin: 0; font-weight: 700; }
      .reserva-meta { margin: 0.1rem 0 0; color: var(--muted); font-size: 0.78rem; }
      .reserva-card-body { display: grid; gap: 0.15rem; color: var(--muted); font-size: 0.84rem; }
      .reserva-card-body p { margin: 0; }

      .reserva-card-actions { display: flex; gap: 0.45rem; flex-wrap: wrap; }
      .card-btn {
        border: 1px solid rgba(92, 58, 33, 0.24);
        background: var(--bg);
        border-radius: 8px;
        padding: 0.42rem 0.65rem;
        cursor: pointer;
      }
      .card-btn-danger { color: #8a2a2a; border-color: rgba(138, 42, 42, 0.35); background: #fdf2f2; }
      .btn-primary { border: none; border-radius: 8px; padding: 0.44rem 0.8rem; }

      .status-pill { border-radius: 999px; padding: 0.18rem 0.55rem; font-size: 0.75rem; font-weight: 600; }
      .status-confirmed { background: #e9f5ee; color: #2d6a4f; }
      .status-cancelled { background: #fdf2f2; color: #8a2a2a; }
      .status-pending { background: #f5f0e8; color: #7a5a2b; }

      .toast-bar {
        position: fixed;
        bottom: 1.1rem;
        left: 50%;
        transform: translateX(-50%);
        background: var(--primary);
        color: #fff;
        padding: 0.6rem 1.25rem;
        border-radius: 10px;
        z-index: 60;
      }
      .toast-danger { background: #5b3f2c; }

      .modal-backdrop {
        position: fixed;
        inset: 0;
        display: grid;
        place-items: center;
        background: rgba(20, 12, 8, 0.45);
        padding: 1rem;
        z-index: 40;
      }
      .modal-card {
        width: min(640px, 96vw);
        background: #fff;
        border-radius: 14px;
        padding: 1rem;
        display: grid;
        gap: 0.6rem;
      }
      .modal-card h3 { margin: 0; }
      .modal-body p { margin: 0.2rem 0; font-size: 0.84rem; }
      .modal-actions { display: flex; justify-content: flex-end; gap: 0.45rem; flex-wrap: wrap; }

      .abono-modal form { display: grid; gap: 0.55rem; }
      .field-label { display: grid; gap: 0.2rem; font-size: 0.84rem; font-weight: 600; }
      .field-error { color: #8a2a2a; font-size: 0.78rem; }

      .summary-panel {
        border: 1px solid rgba(92, 58, 33, 0.16);
        border-radius: 10px;
        padding: 0.65rem;
        background: #fcfaf7;
      }
      .summary-panel h4 { margin: 0 0 0.45rem; font-size: 0.9rem; }
      .summary-panel p { margin: 0.15rem 0; font-size: 0.84rem; }

      @media (max-width: 720px) {
        .search-bar { flex-direction: column; }
        .search-bar .btn-primary { width: 100%; }
      }
    `,
  ],
})
export class ReservasCajeroPageComponent implements OnDestroy {
  searchQuery = '';
  zonaGroups: ZonaGroup[] = [];
  loading = false;
  message = '';

  toastMessage = '';
  toastDanger = false;
  private toastTimer?: ReturnType<typeof setTimeout>;

  showDetail = false;
  detailData?: ReservationDetailData;
  selectedReservaForDetail: ReservationListCajeroItem | null = null;
  detailRawEstado = '';

  abonoModalOpen = false;
  abonoModalType: AbonoTipo = 'ANTICIPO';
  abonoSaving = false;
  abonoErrorMessage = '';
  abonoTargetReserva: ReservationListCajeroItem | null = null;
  abonoResumen: ReservationPaymentSummary | null = null;
  showCancelConfirmDialog = false;
  montoMaxMessage = '';

  readonly abonoForm = this.fb.nonNullable.group({
    monto: ['', [Validators.required]],
    metodo: [''],
    fechaHora: [''],
  });

  private pollingSub?: Subscription;
  private wsSub?: Subscription;

  constructor(
    private readonly reservationService: ReservationService,
    private readonly wsService: WebSocketService,
    private readonly fb: FormBuilder,
  ) {
    this.buscar();
    this.pollingSub = timer(30000, 30000).subscribe(() => this.buscar(false));
    this.wsSub = this.wsService.subscribe('/topic/reservas/cambios').subscribe(() => this.buscar(false));
  }

  @HostListener('document:keydown.escape')
  onEscapePressed(): void {
    if (this.abonoModalOpen) {
      this.onCancelAbonoRequested();
      return;
    }
    if (this.showDetail) {
      this.cerrarDetalle();
    }
  }

  onSearchInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchQuery = target.value;
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

    this.reservationService
      .listForCajero(fechaParam, identificadorParam)
      .pipe(
        catchError((error: HttpErrorResponse) => {
          this.zonaGroups = [];
          if (error.status === 404) {
            this.message = identificadorParam
              ? 'No se encontraron reservas con ese identificador'
              : 'No hay reservas programadas para esta fecha';
          } else {
            this.message = 'Error cargando reservas';
          }
          return of({ reservas: [], resumenZonas: [] });
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((data) => {
        this.zonaGroups = this.groupByZona(data.reservas, data.resumenZonas);
        if (!data.reservas.length && !this.message) {
          this.message = 'No hay reservas programadas para esta fecha';
        }
      });
  }

  estadoLabel(rawEstado: string): string {
    const normalized = rawEstado.toUpperCase();
    if (normalized === 'CONFIRMADA') return 'Confirmada';
    if (normalized === 'CANCELADA') return 'Cancelada';
    if (normalized === 'PENDIENTE') return 'Pendiente';
    if (normalized === 'DEVUELTA') return 'Devuelta';
    return rawEstado;
  }

  statusClass(rawEstado: string): string {
    const normalized = rawEstado.toUpperCase();
    if (normalized === 'CONFIRMADA') return 'status-confirmed';
    if (normalized === 'CANCELADA' || normalized === 'DEVUELTA') return 'status-cancelled';
    return 'status-pending';
  }

  verDetalle(reserva: ReservationListCajeroItem): void {
    this.reservationService.getDetail(reserva.id).subscribe({
      next: (data) => {
        this.detailData = data;
        this.selectedReservaForDetail = reserva;
        this.detailRawEstado = reserva.rawEstado;
        this.showDetail = true;
      },
      error: () => {
        this.showToast('No fue posible cargar el detalle de la reserva.', true);
      },
    });
  }

  cerrarDetalle(): void {
    this.showDetail = false;
    this.detailData = undefined;
    this.selectedReservaForDetail = null;
    this.detailRawEstado = '';
  }

  openAbonoForm(reserva: ReservationListCajeroItem, tipo: AbonoTipo): void {
    this.abonoModalType = tipo;
    this.abonoTargetReserva = reserva;
    this.abonoModalOpen = true;
    this.abonoErrorMessage = '';
    this.montoMaxMessage = '';

    const now = this.toDateTimeLocalValue(new Date());
    this.abonoForm.reset({
      monto: '',
      metodo: '',
      fechaHora: now,
    });

    this.reservationService.getResumenPago(reserva.id).subscribe({
      next: (resumen) => {
        this.abonoResumen = resumen;
      },
      error: (err: HttpErrorResponse) => {
        const msg = this.extractBackendMessage(err);
        this.abonoErrorMessage = msg || 'No fue posible cargar el resumen de pago.';
        this.abonoResumen = null;
      },
    });
  }

  onCancelAbonoRequested(): void {
    if (!this.abonoModalOpen) return;
    if (this.abonoForm.dirty) {
      this.showCancelConfirmDialog = true;
      return;
    }
    this.closeAbonoModal();
  }

  confirmCancelAbono(): void {
    this.showCancelConfirmDialog = false;
    this.closeAbonoModal();
  }

  submitAbono(): void {
    this.abonoForm.markAllAsTouched();
    this.abonoErrorMessage = '';
    this.montoMaxMessage = '';

    if (this.abonoForm.controls.metodo.value === '') {
      return;
    }

    const monto = Number(this.abonoForm.controls.monto.value);
    const fechaHoraRaw = this.abonoForm.controls.fechaHora.value;
    const now = new Date();
    const fechaSeleccionada = new Date(fechaHoraRaw);

    if (Number.isNaN(monto)) {
      this.abonoForm.controls.monto.setErrors({ required: true });
      return;
    }

    if (monto <= 0) {
      this.abonoForm.controls.monto.setErrors({ min: true });
      return;
    }

    if (!fechaHoraRaw) {
      this.abonoForm.controls.fechaHora.setErrors({ required: true });
      return;
    }

    if (Number.isNaN(fechaSeleccionada.getTime()) || fechaSeleccionada.getTime() > now.getTime()) {
      this.abonoForm.controls.fechaHora.setErrors({ invalidDateRange: true });
      return;
    }

    if (!this.abonoTargetReserva || !this.abonoResumen) {
      this.abonoErrorMessage = 'No hay información de reserva para registrar el movimiento.';
      return;
    }

    const montoMaximo =
      this.abonoModalType === 'ANTICIPO'
        ? this.abonoResumen.pendientePorAbonar ?? 0
        : this.abonoResumen.pendientePorDevolver ?? 0;

    if (monto > montoMaximo) {
      this.abonoForm.controls.monto.setErrors({ maxExceeded: true });
      this.montoMaxMessage =
        this.abonoModalType === 'ANTICIPO'
          ? `El monto total abonado no puede exceder el valor de la reserva. Monto pendiente máximo: ${this.formatMoney(montoMaximo)}`
          : `El monto total devuelto no puede exceder el valor abonado. Monto pendiente máximo: ${this.formatMoney(montoMaximo)}`;
      return;
    }

    const payload: RegisterAbonoPayload = {
      tipo: this.abonoModalType,
      monto,
      metodo: this.abonoForm.controls.metodo.value as RegisterAbonoPayload['metodo'],
      fechaHora: this.toIsoFromLocal(fechaHoraRaw),
    };

    this.abonoSaving = true;
    this.reservationService.registrarAbono(this.abonoTargetReserva.id, payload).subscribe({
      next: (result) => {
        this.abonoSaving = false;
        this.showToast(result.message || (this.abonoModalType === 'ANTICIPO' ? 'Anticipo registrado correctamente' : 'Devolución registrada correctamente'));

        if (result.resumen) {
          this.abonoResumen = result.resumen;
        }

        this.closeAbonoModal();
        this.cerrarDetalle();
        this.buscar(false);
      },
      error: (err: HttpErrorResponse) => {
        this.abonoSaving = false;
        const msg = this.extractBackendMessage(err);
        this.abonoErrorMessage = msg || 'No fue posible registrar el movimiento.';
      },
    });
  }

  showMontoRequiredError(): boolean {
    const control = this.abonoForm.controls.monto;
    return control.touched && control.hasError('required');
  }

  showMontoMinError(): boolean {
    const control = this.abonoForm.controls.monto;
    return control.touched && control.hasError('min');
  }

  showMontoMaxError(): boolean {
    const control = this.abonoForm.controls.monto;
    return control.touched && control.hasError('maxExceeded');
  }

  showFechaRequiredError(): boolean {
    const control = this.abonoForm.controls.fechaHora;
    return control.touched && control.hasError('required');
  }

  showFechaInvalidaError(): boolean {
    const control = this.abonoForm.controls.fechaHora;
    return control.touched && control.hasError('invalidDateRange');
  }

  showMetodoRequiredError(): boolean {
    const control = this.abonoForm.controls.metodo;
    return control.touched && !control.value;
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0,
    }).format(value ?? 0);
  }

  formatDateTime(value: string): string {
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return value;
    return parsed.toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
  }

  ngOnDestroy(): void {
    this.pollingSub?.unsubscribe();
    this.wsSub?.unsubscribe();
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  private groupByZona(
    reservas: ReservationListCajeroItem[],
    resumenZonas: Array<{ zonaId?: string; zonaNombre: string; cantidadReservas: number }>
  ): ZonaGroup[] {
    const map = new Map<string, ZonaGroup>();

    for (const z of resumenZonas) {
      map.set(z.zonaNombre, { ...z, reservas: [] });
    }

    for (const reserva of reservas) {
      const key = reserva.zoneName || 'Sin asignar';
      let group = map.get(key);
      if (!group) {
        group = {
          zonaId: reserva.zoneId,
          zonaNombre: key,
          cantidadReservas: 0,
          reservas: [],
        };
        map.set(key, group);
      }
      group.reservas.push(reserva);
    }

    return Array.from(map.values()).filter((g) => g.reservas.length > 0);
  }

  private closeAbonoModal(): void {
    this.abonoModalOpen = false;
    this.abonoSaving = false;
    this.abonoErrorMessage = '';
    this.abonoTargetReserva = null;
    this.abonoResumen = null;
    this.montoMaxMessage = '';
    this.abonoForm.reset({ monto: '', metodo: '', fechaHora: '' });
  }

  private extractBackendMessage(err: HttpErrorResponse): string {
    if (typeof err.error?.message === 'string' && err.error.message.trim().length > 0) {
      return err.error.message;
    }
    if (typeof err.error === 'string' && err.error.trim().length > 0) {
      return err.error;
    }
    return '';
  }

  private toDateTimeLocalValue(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  private toIsoFromLocal(value: string): string {
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? value : parsed.toISOString();
  }

  private showToast(msg: string, danger = false): void {
    this.toastMessage = msg;
    this.toastDanger = danger;
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => {
      this.toastMessage = '';
    }, 4000);
  }
}
