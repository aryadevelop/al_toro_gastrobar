import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { combineLatest } from 'rxjs';
import { Pago, Reserva, ReservaPreorderItem } from '../../../../core/models/domain.models';
import { AuthService } from '../../../../core/services/auth.service';
import { ClientePointsService } from '../../../../core/services/cliente-points.service';
import { ReservationDetailData, ReservationService } from '../../../../core/services/reservation.service';
import { VisitHistoryEntry, VisitService } from '../../../../core/services/visit.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

const WHATSAPP_COMPANY_NUMBER = '573001112233';

@Component({
  selector: 'app-reservas-history-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent, ConfirmDialogComponent],
  template: `
    <section class="page-grid cliente-compact">
      <article class="flash-toast card" *ngIf="showFlash()">
        {{ flashMessage() }}
      </article>

      <app-page-header title="Historial de visitas" subtitle="Tus visitas y reservas futuras"></app-page-header>

      <div class="history-tabs">
        <a class="tab-link" routerLink="/app/cliente">Dashboard</a>
        <span class="tab-link active">Historial</span>
      </div>

      <article class="card points-card">
        <h3>Puntos acumulados: {{ points() }}</h3>
        <p class="points-info">
          Los puntos son acumulables y pueden ser canjeados por recompensas especiales que el restaurante determine.
        </p>
      </article>

      <section class="page-grid">
        <h2 class="section-title">Reservas futuras</h2>

        <article class="card visit-card" *ngFor="let reservation of reservasFuturas">
          <p><strong>Fecha y hora:</strong> {{ formatDateTime(toDateTime(reservation)) }}</p>
          <p><strong>Número de personas:</strong> {{ reservation.guests }}</p>
          <p><strong>Estado:</strong> {{ getReservationStatusLabel(reservation.status) }}</p>

          <div class="visit-actions">
            <button type="button" class="btn-secondary" (click)="onViewFutureDetail(reservation.id)">Ver detalle</button>
            <button type="button" class="btn-secondary" *ngIf="canModifyReservation(reservation)" (click)="onModifyReservation(reservation)">
              Modificar
            </button>
            <button
              type="button"
              class="btn-danger"
              [disabled]="!canCancelReservation(reservation)"
              (click)="onCancelReservation(reservation)"
            >
              Cancelar
            </button>
          </div>
        </article>

        <article class="card empty-state-box" *ngIf="reservasFuturas.length === 0">
          <p class="empty-state">No tienes reservas futuras.</p>
        </article>
      </section>

      <section class="page-grid">
        <h2 class="section-title">Visitas registradas</h2>

        <article class="card visit-card" *ngFor="let visit of visitHistory">
          <p><strong>Fecha y hora:</strong> {{ formatDateTime(visit.dateTime) }}</p>
          <p><strong>Número de personas:</strong> {{ visit.guests }}</p>
          <p><strong>Estado de la visita:</strong> {{ visit.statusLabel }}</p>
          <p><strong>Total:</strong> {{ visit.total | currency:'COP':'symbol':'1.0-0' }}</p>

          <div class="visit-actions">
            <button type="button" class="btn-secondary" [disabled]="!visit.hasDetail" (click)="onViewVisitDetail(visit)">Ver detalle</button>
          </div>
        </article>

        <article class="card empty-state-box" *ngIf="visitHistory.length === 0">
          <p class="empty-state">Aún no tienes visitas registradas.</p>
          <a class="btn-secondary" routerLink="/app/cliente/reserva/create">Nueva reserva</a>
        </article>
      </section>

      <app-confirm-dialog
        [open]="showCancelDialog()"
        title="Cancelar reserva"
        message="¿Deseas cancelar esta reserva?"
        [confirmLabel]="cancelingReservation() ? 'Cancelando...' : 'Sí, cancelar'"
        (confirm)="onConfirmCancelReservation()"
        (cancel)="onCancelDialog()"
      ></app-confirm-dialog>

      <!-- CA-08: Modal de detalle inline -->
      <section class="overlay" *ngIf="showDetailModal()">
        <article class="card detail-modal">
          <div class="detail-modal-header">
            <h3>Detalle de la reserva</h3>
            <button type="button" class="btn-close" (click)="closeDetailModal()">✕</button>
          </div>

          <div class="detail-modal-body" *ngIf="detailReservation()">
            <p><strong>Fecha y hora:</strong> {{ formatDetailDateTime() }}</p>
            <p><strong>Número de personas:</strong> {{ detailReservation()!.guests }}</p>
            <p><strong>Estado de la visita:</strong> {{ getReservationStatusLabel(detailReservation()!.status) }}</p>
            <p><strong>Mesa asignada:</strong> {{ detailReservation()!.tableCode || 'No aplica' }}</p>
            <p><strong>Zona seleccionada:</strong> {{ detailReservation()!.zoneName || 'No aplica' }}</p>
            <p><strong>Decoración seleccionada:</strong> {{ detailReservation()!.decorationName || 'No aplica' }}</p>

            <section class="detail-section">
              <h4>Productos de la comanda</h4>
              <div *ngIf="detailPreorderItems().length > 0; else noComanda">
                <article class="line-item" *ngFor="let item of detailPreorderItems()">
                  <span>{{ item.productName }} x {{ item.quantity }}</span>
                  <span>{{ item.description || 'Sin observaciones' }}</span>
                </article>
              </div>
              <ng-template #noComanda>
                <p class="muted">No aplica</p>
              </ng-template>
            </section>

            <section class="detail-section">
              <h4>Historial de abonos</h4>
              <div *ngIf="detailPayments().length > 0; else noPayments">
                <article class="line-item" *ngFor="let payment of detailPayments()">
                  <span>{{ payment.method }} - {{ formatDate(payment.paidAt) }}</span>
                  <span>{{ payment.amount | currency:'COP':'symbol':'1.0-0' }}</span>
                </article>
              </div>
              <ng-template #noPayments>
                <p class="muted">No aplica</p>
              </ng-template>
            </section>

            <p class="total-row"><strong>Total pre-orden:</strong> {{ detailPreOrderTotal() | currency:'COP':'symbol':'1.0-0' }}</p>
            <p class="total-row"><strong>Total abonado:</strong> {{ detailTotalPaid() | currency:'COP':'symbol':'1.0-0' }}</p>
          </div>

          <div class="detail-modal-body" *ngIf="detailLoading()">
            <p class="muted">Cargando detalle...</p>
          </div>

          <div class="detail-modal-body" *ngIf="!detailReservation() && !detailLoading()">
            <p class="muted">No se encontró el detalle de esta reserva.</p>
          </div>
        </article>
      </section>
    </section>
  `,
  styles: [
    `
      .flash-toast {
        border: 1px solid #6F4E37;
        background: rgba(111, 78, 55, 0.1);
        color: #4d3323;
        padding: 0.7rem 0.9rem;
        font-weight: 700;
      }

      .history-tabs {
        display: flex;
        gap: 0.45rem;
        align-items: center;
      }

      .tab-link {
        border: 1px solid rgba(111, 78, 55, 0.7);
        border-radius: 8px;
        padding: 0.34rem 0.6rem;
        font-size: 0.8rem;
        color: #ffffff;
        background: #6F4E37;
      }

      .tab-link.active {
        background: #5b3f2c;
        color: #ffffff;
      }

      .points-card {
        padding: 0.75rem 0.9rem;
        display: grid;
        gap: 0.32rem;
      }

      .points-card h3 {
        margin: 0;
      }

      .points-info {
        margin: 0;
        font-size: 0.78rem;
        color: var(--muted);
        opacity: 0.75;
        line-height: 1.35;
      }

      .visit-card {
        padding: 0.72rem 0.84rem;
        display: grid;
        gap: 0.25rem;
      }

      .visit-card p {
        margin: 0;
        font-size: 0.84rem;
      }

      .modify-warning {
        color: #5b3f2c;
        font-size: 0.78rem;
      }

      .visit-actions {
        margin-top: 0.2rem;
        display: flex;
        flex-wrap: wrap;
        gap: 0.4rem;
      }

      .visit-actions .btn-secondary,
      .visit-actions .btn-danger {
        padding: 0.42rem 0.62rem;
        font-size: 0.78rem;
        border-radius: 8px;
      }

      .empty-state-box {
        padding: 0.75rem 0.85rem;
        display: grid;
        gap: 0.5rem;
      }

      .empty-state {
        margin: 0;
        color: var(--muted);
        font-size: 0.86rem;
      }

      /* ── Modal de detalle (CA-08) ── */
      .overlay {
        position: fixed;
        inset: 0;
        display: grid;
        place-items: center;
        background: rgba(24, 29, 27, 0.45);
        z-index: 1000;
        padding: 1rem;
        overflow-y: auto;
      }

      .detail-modal {
        width: 100%;
        max-width: 520px;
        max-height: 85vh;
        overflow-y: auto;
        padding: 0.85rem;
        display: grid;
        gap: 0.35rem;
      }

      .detail-modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .detail-modal-header h3 {
        margin: 0;
      }

      .btn-close {
        background: none;
        border: none;
        font-size: 1.1rem;
        cursor: pointer;
        padding: 0.25rem 0.45rem;
        border-radius: 6px;
        color: var(--text);
      }

      .btn-close:hover {
        background: rgba(111, 78, 55, 0.15);
      }

      .detail-modal-body {
        display: grid;
        gap: 0.3rem;
      }

      .detail-modal-body p {
        margin: 0;
        font-size: 0.84rem;
      }

      .detail-section {
        display: grid;
        gap: 0.3rem;
        margin-top: 0.2rem;
      }

      .detail-section h4 {
        margin: 0;
        font-size: 0.88rem;
      }

      .line-item {
        display: flex;
        justify-content: space-between;
        gap: 0.45rem;
        border: 1px dashed rgba(10, 10, 10, 0.2);
        border-radius: 8px;
        padding: 0.35rem 0.45rem;
        font-size: 0.82rem;
      }

      .muted {
        color: var(--muted);
        opacity: 0.65;
      }

      .total-row {
        margin-top: 0.25rem;
      }
    `
  ]
})
export class ReservasHistoryPageComponent implements OnInit {
  readonly points = signal(0);
  readonly flashMessage = signal('');
  readonly showFlash = signal(false);
  readonly showCancelDialog = signal(false);
  readonly cancelingReservation = signal(false);
  readonly reservationPendingCancel = signal<Reserva | null>(null);

  // CA-08: Detail modal state
  readonly showDetailModal = signal(false);
  readonly detailLoading = signal(false);
  readonly detailReservation = signal<Reserva | null>(null);
  readonly detailData = signal<ReservationDetailData | null>(null);

  visitHistory: VisitHistoryEntry[] = [];
  reservasFuturas: Reserva[] = [];

  constructor(
    private readonly authService: AuthService,
    private readonly reservationService: ReservationService,
    private readonly visitService: VisitService,
    private readonly clientePointsService: ClientePointsService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadHistoryData();
  }

  // ── CA-08: Detail modal methods ──

  onViewVisitDetail(visit: VisitHistoryEntry): void {
    if (!visit.reservationId) {
      return;
    }

    this.openDetailModal(visit.reservationId);
  }

  onViewFutureDetail(reservationId: string): void {
    this.openDetailModal(reservationId);
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.detailReservation.set(null);
    this.detailData.set(null);
  }

  formatDetailDateTime(): string {
    const target = this.detailReservation();
    if (!target) {
      return 'No aplica';
    }

    return new Date(`${target.date}T${target.time}:00`).toLocaleString('es-CO', {
      dateStyle: 'short',
      timeStyle: 'short'
    });
  }

  detailPreorderItems(): ReservaPreorderItem[] {
    return this.detailReservation()?.preorderItems ?? [];
  }

  detailPayments(): Pago[] {
    return this.detailData()?.payments ?? [];
  }

  detailPreOrderTotal(): number {
    return this.detailData()?.preOrderTotal ?? 0;
  }

  detailTotalPaid(): number {
    return this.detailData()?.totalPaid ?? 0;
  }

  formatDate(isoDate: string): string {
    return new Date(isoDate).toLocaleString('es-CO', {
      dateStyle: 'short',
      timeStyle: 'short'
    });
  }

  // ── Reservation action methods ──

  onModifyReservation(reservation: Reserva): void {
    if (!this.canModifyReservation(reservation)) {
      this.flashMessage.set(this.getModificationCutoffMessage(reservation));
      this.showFlash.set(true);
      setTimeout(() => this.showFlash.set(false), 3500);
      return;
    }

    void this.router.navigate(['/app/cliente/reserva/edit', reservation.id]);
  }

  onCancelReservation(reservation: Reserva): void {
    if (!this.canCancelReservation(reservation)) {
      return;
    }

    this.reservationPendingCancel.set(reservation);
    this.showCancelDialog.set(true);
  }

  onConfirmCancelReservation(): void {
    const reservation = this.reservationPendingCancel();
    if (!reservation || this.cancelingReservation()) {
      return;
    }

    this.cancelingReservation.set(true);

    this.reservationService.cancel(reservation.id).subscribe({
      next: (result) => {
        this.cancelingReservation.set(false);
        this.showCancelDialog.set(false);
        this.reservationPendingCancel.set(null);

        if (result.requiresWhatsApp) {
          this.redirectToWhatsapp(result.whatsappMessage);
          return;
        }

        this.flashMessage.set('Reserva cancelada correctamente.');
        this.showFlash.set(true);
        setTimeout(() => this.showFlash.set(false), 3500);
        this.loadHistoryData();
      },
      error: (err: HttpErrorResponse) => {
        this.cancelingReservation.set(false);
        this.showCancelDialog.set(false);
        this.reservationPendingCancel.set(null);

        const backendMessage =
          (typeof err.error?.message === 'string' && err.error.message.trim().length > 0
            ? err.error.message
            : '') ||
          (typeof err.error === 'string' && err.error.trim().length > 0 ? err.error : '');

        this.flashMessage.set(backendMessage || 'No fue posible cancelar la reserva. Intenta nuevamente.');
        this.showFlash.set(true);
        setTimeout(() => this.showFlash.set(false), 3500);
      }
    });
  }

  onCancelDialog(): void {
    if (this.cancelingReservation()) {
      return;
    }

    this.showCancelDialog.set(false);
    this.reservationPendingCancel.set(null);
  }

  formatDateTime(date: Date): string {
    return date.toLocaleString('es-CO', {
      dateStyle: 'short',
      timeStyle: 'short'
    });
  }

  getReservationStatusLabel(status: Reserva['status']): string {
    if (status === 'CONFIRMED') {
      return 'Confirmada';
    }

    if (status === 'PENDING') {
      return 'Pendiente';
    }

    if (status === 'CANCELLED') {
      return 'Cancelada';
    }

    if (status === 'COMPLETED') {
      return 'Completada';
    }

    if (status === 'ARRIVED') {
      return 'Asistió';
    }

    return status;
  }

  toDateTime(reservation: Reserva): Date {
    return new Date(`${reservation.date}T${reservation.time}:00`);
  }

  canModifyReservation(reservation: Reserva): boolean {
    return (
      this.toDateTime(reservation).getTime() > Date.now() &&
      !this.isModificationCutoffReached(reservation) &&
      (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED')
    );
  }

  canCancelReservation(reservation: Reserva): boolean {
    return this.toDateTime(reservation).getTime() > Date.now() && (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED');
  }

  private openDetailModal(reservationId: string): void {
    this.showDetailModal.set(true);
    this.detailLoading.set(true);
    this.detailReservation.set(null);
    this.detailData.set(null);

    this.reservationService.getDetail(reservationId).subscribe({
      next: (detail) => {
        this.detailData.set(detail);
        this.detailReservation.set(detail.reservation);
        this.detailLoading.set(false);
      },
      error: () => {
        this.detailLoading.set(false);
      }
    });
  }

  private isModificationCutoffReached(reserva: Reserva): boolean {
    const cutoff = this.getModificationCutoffDate(reserva);
    return Date.now() >= cutoff.getTime();
  }

  private getModificationCutoffMessage(reserva: Reserva): string {
    if (reserva.type === 'SPECIAL') {
      return 'Esta reserva especial solo se puede modificar hasta las 11:00 p.m. del día anterior.';
    }

    return 'Esta reserva básica solo se puede modificar hasta la 1:00 p.m. del mismo día.';
  }

  private getModificationCutoffDate(reserva: Reserva): Date {
    const dayStart = new Date(`${reserva.date}T00:00:00`);

    if (reserva.type === 'SPECIAL') {
      dayStart.setDate(dayStart.getDate() - 1);
      dayStart.setHours(23, 0, 0, 0);
      return dayStart;
    }

    dayStart.setHours(13, 0, 0, 0);
    return dayStart;
  }

  private redirectToWhatsapp(customMessage?: string): void {
    const message = customMessage?.trim()
      ? customMessage
      : 'Hola, deseo gestionar la cancelación y posible reembolso de mi reserva en Al Toro Gastrobar.';

    const url = `https://wa.me/${WHATSAPP_COMPANY_NUMBER}?text=${encodeURIComponent(message)}`;
    window.location.href = url;
  }

  private loadHistoryData(): void {
    const currentUser = this.authService.currentUser();
    if (!currentUser) {
      this.points.set(0);
      this.visitHistory = [];
      this.reservasFuturas = [];
      return;
    }

    combineLatest([
      this.reservationService.listFuture(),
      this.visitService.getHistory(currentUser.email),
      this.clientePointsService.getMyPoints(currentUser.email)
    ]).subscribe(([futureReservations, visits, currentPoints]) => {
      this.reservasFuturas = [...futureReservations]
        .filter((item) => item.status === 'PENDING' || item.status === 'CONFIRMED')
        .sort((a, b) => this.toDateTime(a).getTime() - this.toDateTime(b).getTime());

      this.visitHistory = visits;
      this.points.set(currentPoints);
    });
  }
}
