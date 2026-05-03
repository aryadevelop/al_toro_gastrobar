import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { combineLatest, Subscription } from 'rxjs';
import { DashboardMetric, Pago, Reserva, ReservaPreorderItem } from '../../../../core/models/domain.models';
import { ActiveVisitService, ActiveVisitState, OrderItem } from '../../../../core/services/active-visit.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ClientePointsService } from '../../../../core/services/cliente-points.service';
import { ReservationDetailData, ReservationService } from '../../../../core/services/reservation.service';
import { WebSocketService } from '../../../../core/services/websocket.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

const WHATSAPP_COMPANY_NUMBER = '573001112233';

@Component({
  selector: 'app-cliente-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent, ConfirmDialogComponent],
  template: `
    <section class="page-grid cliente-compact">
      <article class="flash-toast card" *ngIf="showFlash()">
        {{ flashMessage() }}
      </article>

      <section class="dashboard-header-row">
        <app-page-header title="Panel de cliente" subtitle="Resumen de tus reservas y actividad"></app-page-header>
        <div class="header-actions">
          <a class="btn-secondary" routerLink="/app/cliente/reserva/create">Nueva reserva</a>
          <a class="btn-secondary profile-shortcut" routerLink="/app/profile">Mi perfil</a>
        </div>
      </section>

      <article class="card points-card">
        <h3>Puntos acumulados: {{ points() }}</h3>
      </article>

      <section class="metrics-grid">
        <article class="card metric-card" *ngFor="let metric of metrics">
          <p>{{ metric.label }}</p>
          <h3>{{ metric.value }}</h3>
        </article>
      </section>

      <section class="page-grid">
        <div class="reservas-head">
          <h2 class="section-title">Reservas futuras</h2>
          <a class="history-tab" routerLink="/app/cliente/reservas/history">Historial</a>
        </div>

        <article class="card future-card" *ngFor="let reservation of reservasFuturas">
          <p><strong>Fecha y hora:</strong> {{ formatDateTime(reservation) }}</p>
          <p><strong>Número de personas:</strong> {{ reservation.guests }}</p>
          <p><strong>Estado:</strong> {{ getStatusLabel(reservation.status) }}</p>
          <p class="modify-warning" *ngIf="isModificationCutoffReached(reservation)">
            {{ getModificationCutoffMessage(reservation) }}
          </p>

          <div class="reservation-actions">
            <button type="button" class="btn-secondary" (click)="onViewDetail(reservation.id)">Ver detalle</button>
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
          <p class="empty-state">No tienes reservas futuras. ¡Crea una nueva reserva!</p>
          <a class="btn-secondary" routerLink="/app/cliente/reserva/create">Nueva reserva</a>
        </article>
      </section>

      <!-- HU-06: Estado de tu orden -->
      <section class="page-grid" *ngIf="activeVisit()">
        <h2 class="section-title">Estado de tu orden</h2>

        <article class="card order-card" [class.order-closed]="activeVisit()!.closed">
          <div *ngIf="activeVisit()!.closed" class="closed-banner">
            La cuenta ya está cerrada. ¡Gracias por tu visita!
          </div>

          <div *ngIf="orderItems().length > 0; else noProducts">
            <article class="order-item" *ngFor="let item of orderItems()">
              <div class="order-item-info">
                <span class="order-item-name">{{ item.productName }}</span>
                <span class="order-item-status" [class.servido]="item.status === 'Servido'">{{ item.status }}</span>
              </div>
              <div class="order-item-numbers">
                <span>{{ item.quantity }} x {{ item.unitPrice | currency:'COP':'symbol':'1.0-0' }}</span>
                <span class="order-item-subtotal">{{ item.subtotal | currency:'COP':'symbol':'1.0-0' }}</span>
              </div>
            </article>
          </div>

          <ng-template #noProducts>
            <p class="empty-state" *ngIf="!activeVisit()!.closed">Aún no tienes productos en tu cuenta. ¡Pide algo del menú!</p>
          </ng-template>

          <p class="order-total" *ngIf="orderItems().length > 0">
            <strong>Total acumulado:</strong> {{ activeVisit()!.total | currency:'COP':'symbol':'1.0-0' }}
          </p>

          <div class="order-actions" *ngIf="!activeVisit()!.closed">
            <button
              type="button"
              class="btn-secondary"
              [disabled]="assistanceRequested()"
              (click)="onRequestAssistance()"
            >
              {{ assistanceRequested() ? 'Solicitud enviada' : 'Solicitar asistencia' }}
            </button>
          </div>
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

      <app-confirm-dialog
        [open]="showAssistanceDialog()"
        title="Solicitar asistencia"
        message="¿Solicitar atención? El mesero se acercará lo más pronto posible."
        cancelLabel="Cancelar"
        confirmLabel="Solicitar"
        (confirm)="onConfirmAssistance()"
        (cancel)="showAssistanceDialog.set(false)"
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
            <p><strong>Estado de la visita:</strong> {{ getStatusLabel(detailReservation()!.status) }}</p>
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

      .dashboard-header-row {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 0.8rem;
        flex-wrap: wrap;
      }

      .profile-shortcut {
        text-align: center;
        white-space: nowrap;
      }

      .header-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.6rem;
        align-items: center;
      }

      .header-actions .btn-primary,
      .header-actions .btn-secondary {
        padding: 0.5rem 0.72rem;
        font-size: 0.82rem;
        border-radius: 8px;
      }

      .points-card {
        padding: 0.4rem 0.6rem;
        display: grid;
        gap: 0.15rem;
      }

      .points-card h3 {
        margin: 0;
          font-size: 0.95rem;
      }

      .points-info {
        margin: 0;
        font-size: 0.78rem;
        color: var(--muted);
        opacity: 0.75;
        line-height: 1.35;
      }

      .metrics-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
        gap: 0.55rem;
      }

      .metric-card {
        padding: 0.28rem 0.36rem;
        min-height: 44px;
        display: flex;
        flex-direction: column;
        justify-content: center;
        gap: 0.08rem;
        align-items: flex-start;
      }

      .metric-card p,
      .metric-card h3 {
        margin: 0;
      }

      .metric-card p {
        color: var(--muted);
        font-size: 0.7rem;
        opacity: 0.95;
      }

      .metric-card h3 {
        font-size: 0.95rem;
        line-height: 1;
        font-weight: 700;
      }

      .reservas-head {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 0.5rem;
      }

      .history-tab {
        border: 1px solid rgba(111, 78, 55, 0.7);
        border-radius: 8px;
        padding: 0.35rem 0.6rem;
        font-size: 0.8rem;
        color: #ffffff;
        background: #6F4E37;
      }

      .future-card {
        padding: 0.72rem 0.84rem;
        display: grid;
        gap: 0.28rem;
      }

      .future-card p {
        margin: 0;
        font-size: 0.84rem;
      }

      .modify-warning {
        color: #5b3f2c;
        font-size: 0.78rem;
      }

      .reservation-actions {
        margin-top: 0.2rem;
        display: flex;
        flex-wrap: wrap;
        gap: 0.4rem;
      }

      .reservation-actions .btn-secondary,
      .reservation-actions .btn-danger {
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

      /* ── HU-06: Estado de tu orden ── */
      .order-card {
        padding: 0.72rem 0.84rem;
        display: grid;
        gap: 0.35rem;
      }

      .order-card.order-closed {
        opacity: 0.7;
        pointer-events: none;
      }

      .closed-banner {
        background: rgba(111, 78, 55, 0.15);
        border: 1px solid #6F4E37;
        border-radius: 8px;
        padding: 0.55rem 0.7rem;
        font-weight: 700;
        font-size: 0.84rem;
        color: #4d3323;
        text-align: center;
      }

      .order-item {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 0.3rem;
        border: 1px dashed rgba(10, 10, 10, 0.2);
        border-radius: 8px;
        padding: 0.4rem 0.5rem;
        font-size: 0.82rem;
      }

      .order-item-info {
        display: flex;
        flex-direction: column;
        gap: 0.1rem;
      }

      .order-item-name {
        font-weight: 600;
      }

      .order-item-status {
        font-size: 0.75rem;
        color: #5b3f2c;
      }

      .order-item-status.servido {
        color: #333333;
        font-weight: 600;
      }

      .order-item-numbers {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        gap: 0.1rem;
        font-size: 0.8rem;
      }

      .order-item-subtotal {
        font-weight: 600;
      }

      .order-total {
        margin: 0.3rem 0 0;
        font-size: 0.9rem;
      }

      .order-actions {
        margin-top: 0.2rem;
        display: flex;
        gap: 0.4rem;
      }

      .order-actions .btn-secondary {
        padding: 0.42rem 0.62rem;
        font-size: 0.78rem;
        border-radius: 8px;
      }
    `
  ]
})
export class ClienteDashboardComponent implements OnInit, OnDestroy {
  readonly flashMessage = signal('');
  readonly showFlash = signal(false);
  readonly points = signal(0);
  readonly showCancelDialog = signal(false);
  readonly cancelingReservation = signal(false);
  readonly reservationPendingCancel = signal<Reserva | null>(null);

  // CA-08: Detail modal state
  readonly showDetailModal = signal(false);
  readonly detailLoading = signal(false);
  readonly detailReservation = signal<Reserva | null>(null);
  readonly detailData = signal<ReservationDetailData | null>(null);

  // HU-06: Active visit / order state
  readonly activeVisit = signal<ActiveVisitState | null>(null);
  readonly assistanceRequested = signal(false);
  readonly showAssistanceDialog = signal(false);

  metrics: DashboardMetric[] = [];
  reservasFuturas: Reserva[] = [];

  private wsSubscriptions: Subscription[] = [];

  constructor(
    private readonly authService: AuthService,
    private readonly reservationService: ReservationService,
    private readonly clientePointsService: ClientePointsService,
    private readonly activeVisitService: ActiveVisitService,
    private readonly webSocketService: WebSocketService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const state = history.state as { flashMessage?: string };
    if (state.flashMessage) {
      this.flashMessage.set(state.flashMessage);
      this.showFlash.set(true);
      setTimeout(() => this.showFlash.set(false), 3500);
      history.replaceState({}, document.title, this.router.url);
    }

    this.loadDashboardData();
    // Only attempt to load active visit for clients to avoid 404 noise in console
    const current = this.authService.currentUser();
    if (current?.role === 'CLIENTE') {
      this.loadActiveVisit();
    }
  }

  ngOnDestroy(): void {
    this.wsSubscriptions.forEach((sub) => sub.unsubscribe());
  }

  // ── CA-08: Detail modal methods ──

  onViewDetail(reservationId: string): void {
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
      if (this.isModificationCutoffReached(reservation)) {
        this.flashMessage.set(this.getModificationCutoffMessage(reservation));
        this.showFlash.set(true);
        setTimeout(() => this.showFlash.set(false), 3500);
      }
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
        this.loadDashboardData();
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

  canModifyReservation(reservation: Reserva): boolean {
    return (
      this.isFutureReservation(reservation) &&
      (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED') &&
      !this.isModificationCutoffReached(reservation)
    );
  }

  canCancelReservation(reservation: Reserva): boolean {
    return this.isFutureReservation(reservation) && (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED');
  }

  getStatusLabel(status: Reserva['status']): string {
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
      return 'Asistio';
    }

    return status;
  }

  formatDateTime(reserva: Reserva): string {
    return this.toDateTime(reserva).toLocaleString('es-CO', {
      dateStyle: 'short',
      timeStyle: 'short'
    });
  }

  private loadDashboardData(): void {
    const currentUser = this.authService.currentUser();
    if (!currentUser) {
      this.metrics = [];
      this.reservasFuturas = [];
      this.points.set(0);
      return;
    }

    combineLatest([
      this.reservationService.listFuture(),
      this.clientePointsService.getMyPoints(currentUser.email)
    ]).subscribe(([futureReservations, currentPoints]) => {
      const orderedFuture = [...futureReservations]
        .filter((item) => item.status === 'PENDING' || item.status === 'CONFIRMED')
        .sort((a, b) => this.toDateTime(a).getTime() - this.toDateTime(b).getTime());

      const pending = orderedFuture.filter((item) => item.status === 'PENDING').length;
      const confirmed = orderedFuture.filter((item) => item.status === 'CONFIRMED').length;

      this.points.set(currentPoints);
      this.reservasFuturas = orderedFuture;

      this.metrics = [
        { id: 'cm-1', label: 'Reservas futuras', value: orderedFuture.length, tone: 'neutral' },
        { id: 'cm-2', label: 'Pendientes', value: pending, tone: pending > 0 ? 'success' : 'neutral' },
        { id: 'cm-3', label: 'Confirmadas', value: confirmed, tone: 'neutral' }
      ];
    });
  }

  private isFutureReservation(reserva: Reserva): boolean {
    return this.toDateTime(reserva).getTime() > Date.now();
  }

  isModificationCutoffReached(reserva: Reserva): boolean {
    const cutoff = this.getModificationCutoffDate(reserva);
    return Date.now() >= cutoff.getTime();
  }

  getModificationCutoffMessage(reserva: Reserva): string {
    if (reserva.type === 'SPECIAL') {
      return 'Esta reserva especial solo se puede modificar hasta las 11:00 p.m. del día anterior.';
    }

    return 'Esta reserva básica solo se puede modificar hasta la 1:00 p.m. del mismo día.';
  }

  private toDateTime(reserva: Reserva): Date {
    return new Date(`${reserva.date}T${reserva.time}:00`);
  }

  private redirectToWhatsapp(customMessage?: string): void {
    const message = customMessage?.trim()
      ? customMessage
      : 'Hola, deseo gestionar la cancelación y posible reembolso de mi reserva en Al Toro Gastrobar.';

    const url = `https://wa.me/${WHATSAPP_COMPANY_NUMBER}?text=${encodeURIComponent(message)}`;
    window.location.href = url;
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

  // ── HU-06: Active visit / order methods ──

  orderItems(): OrderItem[] {
    return this.activeVisit()?.items ?? [];
  }

  onRequestAssistance(): void {
    this.showAssistanceDialog.set(true);
  }

  onConfirmAssistance(): void {
    const visit = this.activeVisit();
    if (!visit) {
      return;
    }

    this.showAssistanceDialog.set(false);

    this.activeVisitService.requestAssistance(visit.visitaId).subscribe({
      next: () => {
        this.assistanceRequested.set(true);
        this.flashMessage.set('Solicitud enviada. El mesero te atenderá en breve.');
        this.showFlash.set(true);
        setTimeout(() => this.showFlash.set(false), 3500);
      },
      error: (err: HttpErrorResponse) => {
        const msg = err.error?.message || 'No fue posible enviar la solicitud.';
        this.flashMessage.set(msg);
        this.showFlash.set(true);
        setTimeout(() => this.showFlash.set(false), 3500);
      }
    });
  }

  private loadActiveVisit(): void {
    this.activeVisitService.getActiveVisit().subscribe({
      next: (visit) => {
        this.activeVisit.set(visit);
        if (visit) {
          this.assistanceRequested.set(visit.assistanceRequested);
          this.subscribeToVisitWebSocket(visit.visitaId);
        }
      },
      error: () => {
        this.activeVisit.set(null);
      }
    });
  }

  private subscribeToVisitWebSocket(visitaId: string): void {
    // CA-02 of HU-06: Real-time order updates
    const orderSub = this.webSocketService
      .subscribe<{ visitaId: number; items: Array<{ comandaItemId: number; nombreProducto: string; cantidad: number; estadoItem: string; precioUnitario: number; subtotal: number }>; total: number }>(
        `/topic/visita/${visitaId}/orden`
      )
      .subscribe((msg) => {
        const current = this.activeVisit();
        if (current) {
          this.activeVisit.set({
            ...current,
            items: msg.items.map((i) => ({
              comandaItemId: String(i.comandaItemId),
              productName: i.nombreProducto,
              quantity: i.cantidad,
              status: i.estadoItem,
              unitPrice: i.precioUnitario,
              subtotal: i.subtotal,
            })),
            total: msg.total,
          });
        }
      });
    this.wsSubscriptions.push(orderSub);

    // CA-04 of HU-06: Account closed
    const closedSub = this.webSocketService
      .subscribe<{ visitaId: number; mensaje: string; puntosActuales: number }>(
        `/topic/visita/${visitaId}/cuenta`
      )
      .subscribe((msg) => {
        const current = this.activeVisit();
        if (current) {
          this.activeVisit.set({ ...current, closed: true });
        }
        this.points.set(msg.puntosActuales);
        this.flashMessage.set(msg.mensaje || 'La cuenta ya está cerrada. ¡Gracias por tu visita!');
        this.showFlash.set(true);
        setTimeout(() => this.showFlash.set(false), 5000);
      });
    this.wsSubscriptions.push(closedSub);

    // CA-06 of HU-06: Assistance attended
    const assistSub = this.webSocketService
      .subscribe<{ visitaId: number; asistenciaAtendida: boolean }>(
        `/topic/visita/${visitaId}/asistencia`
      )
      .subscribe((msg) => {
        if (msg.asistenciaAtendida) {
          this.assistanceRequested.set(false);
        }
      });
    this.wsSubscriptions.push(assistSub);
  }
}
