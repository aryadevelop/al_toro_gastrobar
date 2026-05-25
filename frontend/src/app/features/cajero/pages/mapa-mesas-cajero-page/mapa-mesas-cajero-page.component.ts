import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, timer } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  MesaDetalle,
  MesaMapService,
  MesaMapaItem,
  MesaNotificacionActiva,
  MesaZonaMapa,
} from '../../../../core/services/mesa-map.service';
import { MesaNotificacionService } from '../../../../core/services/mesa-notificacion.service';
import { WebSocketService } from '../../../../core/services/websocket.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-mapa-mesas-cajero-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent],
  template: `
    <section class="page-grid mapa-shell">
      <app-page-header title="Mapa de mesas" subtitle="Vista operativa por zonas"></app-page-header>

      <article class="card mapa-panel">
        <div class="mapa-state" *ngIf="loading()">Cargando mesas...</div>

        <div class="mapa-state error" *ngIf="!loading() && errorMessage()">
          <p>{{ errorMessage() }}</p>
          <button class="btn-outline" type="button" (click)="loadMapa()">Reintentar</button>
        </div>

        <div class="mapa-message" *ngIf="actionMessage()" [ngClass]="actionTone()">
          {{ actionMessage() }}
        </div>

        <ng-container *ngIf="!loading() && !errorMessage()">
          <div class="tabs-row" *ngIf="mapa().zonas.length > 0">
            <button
              class="tab-btn"
              type="button"
              [class.tab-active]="selectedZonaId() === 'ALL'"
              (click)="selectZona('ALL')"
            >
              Todas las zonas ({{ mapa().totalMesas }})
            </button>

            <button
              class="tab-btn"
              type="button"
              *ngFor="let zona of mapa().zonas"
              [class.tab-active]="selectedZonaId() === zona.id"
              (click)="selectZona(zona.id)"
            >
              {{ zona.name }} ({{ zona.count }})
            </button>
          </div>

          <div class="mapa-empty" *ngIf="visibleZonas().length === 0">
            <p>No hay mesas registradas en esta zona</p>
          </div>

          <section class="zone-section" *ngFor="let zona of visibleZonas()">
            <header class="zone-header">
              <h3>{{ zona.name }}</h3>
              <p>{{ zona.count }} mesas</p>
            </header>

            <div class="mapa-empty" *ngIf="zona.mesas.length === 0">
              <p>No hay mesas registradas en esta zona</p>
            </div>

            <div class="mesa-grid" *ngIf="zona.mesas.length > 0">
              <article class="mesa-card" *ngFor="let mesa of zona.mesas" [ngClass]="statusClass(mesa.estado)">
                <div class="mesa-top">
                  <div>
                    <p class="mesa-id">{{ mesa.identificador }}</p>
                    <p class="mesa-meta">{{ mesa.numeroPersonas }} personas</p>
                    <p class="mesa-meta" *ngIf="mesa.nombreMesero">Mesero: {{ mesa.nombreMesero }}</p>
                  </div>
                  <span class="mesa-status">{{ statusLabel(mesa.estado) }}</span>
                </div>

                <div class="mesa-tags" *ngIf="mesa.tieneBorrador || mesa.notificaciones.length > 0">
                  <span class="mesa-draft" *ngIf="mesa.tieneBorrador" title="Comanda en borrador">📝</span>

                  <button
                    class="notif-icon"
                    type="button"
                    *ngFor="let notificacion of mesa.notificaciones"
                    [ngClass]="notificationClass(notificacion.tipo)"
                    [disabled]="isNotificationPending(notificacion.id)"
                    [attr.title]="notificationTitle(notificacion.tipo)"
                    [attr.aria-label]="notificationTitle(notificacion.tipo)"
                    (click)="handleNotification(mesa, notificacion)"
                  >
                    {{ notificationShort(notificacion.tipo) }}
                  </button>
                </div>

                <div class="mesa-actions">
                  <button class="btn-link" type="button" (click)="openDetalle(mesa)">Ver</button>
                  <button class="btn-primary" type="button" disabled title="Solo mesero puede modificar">
                    Modificar
                  </button>
                </div>
              </article>
            </div>
          </section>
        </ng-container>
      </article>

      <div class="modal-backdrop" *ngIf="selectedMesa() as detail" (click)="closeDetalle()">
        <div class="modal-card" (click)="$event.stopPropagation()">
          <header class="modal-head">
            <div>
              <p class="modal-eyebrow">Detalle de mesa</p>
              <h3>Mesa {{ detail.identificador }}</h3>
            </div>
            <button class="btn-link" type="button" (click)="closeDetalle()">Cerrar</button>
          </header>

          <div class="modal-body">
            <div class="detail-grid">
              <div>
                <p class="detail-label">Cliente ID</p>
                <p class="detail-value">{{ detail.clienteId ?? 'No aplica' }}</p>
              </div>
              <div>
                <p class="detail-label">Puntos / Cumpleaños</p>
                <p class="detail-value">
                  {{ detail.esCumpleanos ? 'Cumpleaños hoy' : detail.puntosFidelizacion ?? 'No aplica' }}
                </p>
              </div>
              <div>
                <p class="detail-label">Nombre cliente</p>
                <p class="detail-value">{{ detail.nombreCliente ?? 'Sin asignar' }}</p>
              </div>
              <div>
                <p class="detail-label">Mesa</p>
                <p class="detail-value">{{ detail.identificador }}</p>
              </div>
              <div>
                <p class="detail-label">Hora de llegada</p>
                <p class="detail-value">{{ formatDateTime(detail.horaLlegada) }}</p>
              </div>
              <div>
                <p class="detail-label">Número de personas</p>
                <p class="detail-value">{{ detail.numeroPersonas ?? 0 }}</p>
              </div>
              <div>
                <p class="detail-label">Estado</p>
                <p class="detail-value">{{ statusLabel(detail.estado ?? '') }}</p>
              </div>
            </div>

            <div class="detail-notes" *ngIf="detail.notasReserva || detail.notasMesa || detail.notasComandas">
              <p class="detail-label">Notas adicionales</p>
              <p class="detail-value" *ngIf="detail.notasReserva">Reserva: {{ detail.notasReserva }}</p>
              <p class="detail-value" *ngIf="detail.notasMesa">Mesa: {{ detail.notasMesa }}</p>
              <p class="detail-value" *ngIf="detail.notasComandas">Comandas: {{ detail.notasComandas }}</p>
            </div>

            <section class="detail-block">
              <h4>Comanda / Orden</h4>
              <div class="detail-empty" *ngIf="!detail.itemsComanda.length">Sin comanda registrada.</div>
              <ul class="detail-list" *ngIf="detail.itemsComanda.length">
                <li *ngFor="let item of detail.itemsComanda">
                  <div>
                    <p class="detail-item">{{ item.nombreProducto }}</p>
                    <p class="detail-sub" *ngIf="item.descripcion">{{ item.descripcion }}</p>
                  </div>
                  <span class="detail-qty">x{{ item.cantidad }}</span>
                </li>
              </ul>
            </section>

            <button class="btn-primary" type="button" *ngIf="detail.puedeGenerarCuenta" (click)="goToGenerarCuenta(detail)">
              Generar cuenta
            </button>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      .mapa-shell {
        gap: 1rem;
      }

      .mapa-panel {
        padding: 1rem;
      }

      .mapa-state {
        display: grid;
        place-items: center;
        min-height: 160px;
        color: var(--muted);
        text-align: center;
      }

      .mapa-state.error {
        color: var(--danger);
      }

      .tabs-row {
        display: flex;
        flex-wrap: wrap;
        gap: 0.45rem;
        margin-bottom: 0.85rem;
      }

      .tab-btn {
        border: 1px solid rgba(10, 10, 10, 0.1);
        background: #fff;
        border-radius: 999px;
        padding: 0.35rem 0.7rem;
        cursor: pointer;
        font-size: 0.78rem;
      }

      .tab-btn.tab-active {
        background: #8b5e3c;
        color: #fff;
        border-color: #8b5e3c;
      }

      .zone-section {
        border: 1px solid rgba(10, 10, 10, 0.1);
        border-radius: 14px;
        margin-bottom: 0.7rem;
        padding: 0.7rem;
      }

      .zone-header {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        margin-bottom: 0.65rem;
      }

      .zone-header h3,
      .zone-header p {
        margin: 0;
      }

      .zone-header p {
        color: var(--muted);
        font-size: 0.8rem;
      }

      .mesa-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
        gap: 0.7rem;
      }

      .mesa-card {
        border: 1px solid rgba(10, 10, 10, 0.12);
        border-radius: 12px;
        padding: 0.7rem;
        display: grid;
        gap: 0.55rem;
      }

      .state-attended {
        border-color: rgba(41, 98, 150, 0.45);
        background: rgba(41, 98, 150, 0.06);
      }

      .state-prep {
        border-color: rgba(46, 125, 50, 0.45);
        background: rgba(46, 125, 50, 0.07);
      }

      .state-wait {
        border-color: rgba(107, 114, 128, 0.55);
        background: rgba(107, 114, 128, 0.09);
      }

      .mesa-top {
        display: flex;
        justify-content: space-between;
        gap: 0.5rem;
      }

      .mesa-id {
        margin: 0;
        font-weight: 700;
      }

      .mesa-meta {
        margin: 0.14rem 0 0;
        color: var(--muted);
        font-size: 0.77rem;
      }

      .mesa-status {
        border-radius: 999px;
        padding: 0.15rem 0.5rem;
        font-size: 0.72rem;
        font-weight: 700;
        text-transform: uppercase;
      }

      .state-attended .mesa-status {
        color: #1d4ed8;
        background: rgba(37, 99, 235, 0.15);
      }

      .state-prep .mesa-status {
        color: #15803d;
        background: rgba(34, 197, 94, 0.15);
      }

      .state-wait .mesa-status {
        color: #4b5563;
        background: rgba(107, 114, 128, 0.15);
      }

      .mesa-tags {
        display: flex;
        align-items: center;
        gap: 0.35rem;
        flex-wrap: wrap;
      }

      .mesa-draft {
        font-size: 0.95rem;
      }

      .notif-icon {
        border: 1px solid rgba(10, 10, 10, 0.18);
        border-radius: 999px;
        width: 28px;
        height: 28px;
        display: grid;
        place-items: center;
        padding: 0;
        cursor: pointer;
        background: #fff;
      }

      .notif-icon:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      .notif-attention {
        color: #be123c;
        border-color: rgba(190, 18, 60, 0.35);
        background: rgba(244, 63, 94, 0.14);
      }

      .notif-platos {
        color: #b45309;
        border-color: rgba(180, 83, 9, 0.35);
        background: rgba(245, 158, 11, 0.16);
      }

      .notif-bebidas {
        color: #166534;
        border-color: rgba(22, 101, 52, 0.35);
        background: rgba(34, 197, 94, 0.15);
      }

      .notif-cambio {
        color: #1d4ed8;
        border-color: rgba(29, 78, 216, 0.35);
        background: rgba(59, 130, 246, 0.16);
      }

      .mesa-actions {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .btn-link {
        border: none;
        background: none;
        color: #8b5e3c;
        font-weight: 600;
        padding: 0;
        cursor: pointer;
      }

      .btn-primary {
        border: none;
        background: #8b5e3c;
        color: #fff;
        border-radius: 999px;
        padding: 0.35rem 0.8rem;
        font-size: 0.76rem;
        cursor: pointer;
      }

      .btn-primary:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      .btn-outline {
        border: 1px solid rgba(10, 10, 10, 0.2);
        background: #fff;
        border-radius: 999px;
        padding: 0.35rem 0.8rem;
      }

      .mapa-message {
        margin-bottom: 0.7rem;
        padding: 0.5rem 0.7rem;
        border-radius: 10px;
        font-size: 0.8rem;
        font-weight: 600;
      }

      .mapa-message.success {
        color: #166534;
        background: rgba(34, 197, 94, 0.12);
      }

      .mapa-message.error {
        color: #be123c;
        background: rgba(244, 63, 94, 0.13);
      }

      .mapa-empty {
        color: var(--muted);
        display: grid;
        place-items: center;
        min-height: 90px;
      }

      .mapa-empty p {
        margin: 0;
      }

      .modal-backdrop {
        position: fixed;
        inset: 0;
        background: rgba(10, 10, 10, 0.45);
        display: grid;
        place-items: center;
        padding: 1rem;
        z-index: 20;
      }

      .modal-card {
        width: min(640px, 100%);
        max-height: calc(100vh - 2rem);
        overflow: auto;
        background: #fff;
        border-radius: 16px;
        padding: 1rem;
      }

      .modal-head {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.8rem;
      }

      .modal-eyebrow {
        margin: 0;
        font-size: 0.72rem;
        color: var(--muted);
        text-transform: uppercase;
      }

      .modal-head h3 {
        margin: 0.1rem 0 0;
      }

      .modal-body {
        display: grid;
        gap: 0.85rem;
      }

      .detail-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
        gap: 0.65rem;
      }

      .detail-label {
        margin: 0;
        font-size: 0.72rem;
        color: var(--muted);
        text-transform: uppercase;
      }

      .detail-value {
        margin: 0.2rem 0 0;
        font-size: 0.84rem;
        font-weight: 600;
      }

      .detail-notes {
        background: rgba(10, 10, 10, 0.04);
        border-radius: 10px;
        padding: 0.65rem;
        display: grid;
        gap: 0.35rem;
      }

      .detail-block h4 {
        margin: 0 0 0.45rem;
        font-size: 0.88rem;
      }

      .detail-list {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        gap: 0.45rem;
      }

      .detail-list li {
        border: 1px solid rgba(10, 10, 10, 0.08);
        border-radius: 10px;
        padding: 0.45rem 0.55rem;
        display: flex;
        justify-content: space-between;
        gap: 0.6rem;
      }

      .detail-item {
        margin: 0;
        font-weight: 600;
      }

      .detail-sub {
        margin: 0.1rem 0 0;
        color: var(--muted);
        font-size: 0.74rem;
      }

      .detail-qty {
        font-weight: 700;
      }

      .detail-empty {
        color: var(--muted);
        font-size: 0.84rem;
      }
    `,
  ]
})
export class MapaMesasCajeroPageComponent implements OnInit, OnDestroy {
  private readonly mesaService = inject(MesaMapService);
  private readonly webSocket = inject(WebSocketService);
  private readonly notificacionService = inject(MesaNotificacionService);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly mapa = signal<{ zonas: MesaZonaMapa[]; totalMesas: number }>({ zonas: [], totalMesas: 0 });
  readonly selectedZonaId = signal<string>('ALL');
  readonly selectedMesa = signal<MesaDetalle | null>(null);
  readonly actionMessage = signal('');
  readonly actionTone = signal<'success' | 'error' | ''>('');
  readonly pendingNotifications = signal<Set<string>>(new Set());

  ngOnInit(): void {
    this.loadMapa();

    this.webSocket
      .subscribe('/topic/mesas')
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadMapa(false));

    timer(45000, 45000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadMapa(false));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadMapa(showSpinner = true): void {
    if (showSpinner) {
      this.loading.set(true);
    }
    this.errorMessage.set(null);

    this.mesaService
      .getMapa()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.mapa.set(data);
          const selected = this.selectedZonaId();
          if (selected !== 'ALL' && !data.zonas.some((zona) => zona.id === selected)) {
            this.selectedZonaId.set('ALL');
          }
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('No pudimos cargar el mapa de mesas.');
          this.loading.set(false);
        },
      });
  }

  selectZona(zonaId: string): void {
    this.selectedZonaId.set(zonaId);
  }

  visibleZonas(): MesaZonaMapa[] {
    const selected = this.selectedZonaId();
    const zonas = this.mapa().zonas;
    if (selected === 'ALL') {
      return zonas;
    }
    return zonas.filter((zona) => zona.id === selected);
  }

  openDetalle(mesa: MesaMapaItem): void {
    this.selectedMesa.set({
      mesaId: mesa.id,
      identificador: mesa.identificador,
      itemsComanda: [],
    });

    this.mesaService
      .getDetalle(mesa.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (detail) => this.selectedMesa.set(detail),
        error: () => {
          this.selectedMesa.set({
            mesaId: mesa.id,
            identificador: mesa.identificador,
            itemsComanda: [],
          });
        },
      });
  }

  closeDetalle(): void {
    this.selectedMesa.set(null);
  }

  goToGenerarCuenta(detail: MesaDetalle): void {
    this.router.navigate(['/app/cajero/pago-cierre'], {
      queryParams: { mesaId: detail.mesaId, visitaId: detail.visitaId },
    });
  }

  statusLabel(status: string): string {
    const normalized = status?.toUpperCase() ?? '';
    switch (normalized) {
      case 'ATENDIDA':
        return 'Atendida';
      case 'EN_PREPARACION':
        return 'En preparación';
      case 'ESPERA':
        return 'Espera';
      case 'CERRADA':
        return 'Cerrada';
      default:
        return normalized ? normalized.toLowerCase().replace('_', ' ') : 'Sin estado';
    }
  }

  statusClass(status: string): string {
    const normalized = status?.toUpperCase() ?? '';
    switch (normalized) {
      case 'ATENDIDA':
        return 'state-attended';
      case 'EN_PREPARACION':
        return 'state-prep';
      case 'ESPERA':
        return 'state-wait';
      default:
        return '';
    }
  }

  notificationShort(tipo: string): string {
    switch (tipo?.toUpperCase()) {
      case 'ATENCION':
      case 'AT':
        return '🛎️';
      case 'PLATOS_LISTOS':
      case 'PL':
        return '🍽️';
      case 'BEBIDAS_LISTAS':
      case 'BE':
        return '🥤';
      case 'CAMBIO':
      case 'CA':
        return '🔄';
      default:
        return '🔔';
    }
  }

  notificationClass(tipo: string): string {
    switch (tipo?.toUpperCase()) {
      case 'ATENCION':
      case 'AT':
        return 'notif-attention';
      case 'PLATOS_LISTOS':
      case 'PL':
        return 'notif-platos';
      case 'BEBIDAS_LISTAS':
      case 'BE':
        return 'notif-bebidas';
      case 'CAMBIO':
      case 'CA':
        return 'notif-cambio';
      default:
        return '';
    }
  }

  notificationTitle(tipo: string): string {
    switch (tipo?.toUpperCase()) {
      case 'ATENCION':
      case 'AT':
        return 'Atención';
      case 'PLATOS_LISTOS':
      case 'PL':
        return 'Platos listos';
      case 'BEBIDAS_LISTAS':
      case 'BE':
        return 'Bebidas listas';
      case 'CAMBIO':
      case 'CA':
        return 'Solicitud de cambio';
      default:
        return tipo ?? '';
    }
  }

  isNotificationPending(notificacionId: string): boolean {
    return this.pendingNotifications().has(notificacionId);
  }

  handleNotification(mesa: MesaMapaItem, notificacion: MesaNotificacionActiva): void {
    const action = this.resolveActionType(notificacion.tipo);
    if (!action || this.isNotificationPending(notificacion.id)) {
      return;
    }

    this.setPending(notificacion.id, true);
    this.actionMessage.set('');
    this.actionTone.set('');

    switch (action) {
      case 'ATENCION':
        this.notificacionService.atenderAsistencia(notificacion.id).subscribe({
          next: () => this.handleActionSuccess(mesa.id, notificacion.id, 'Atención registrada'),
          error: (err) => this.handleActionError(notificacion.id, err),
        });
        break;
      case 'PLATOS_LISTOS':
        this.notificacionService.servirPlatos(notificacion.id).subscribe({
          next: () => this.handleActionSuccess(mesa.id, notificacion.id, 'Platos servidos'),
          error: (err) => this.handleActionError(notificacion.id, err),
        });
        break;
      case 'BEBIDAS_LISTAS':
        this.notificacionService.servirBebidas(notificacion.id).subscribe({
          next: () => this.handleActionSuccess(mesa.id, notificacion.id, 'Bebidas servidas'),
          error: (err) => this.handleActionError(notificacion.id, err),
        });
        break;
      case 'CAMBIO':
        this.notificacionService.atenderCambio(notificacion.id).subscribe({
          next: () => this.handleActionSuccess(mesa.id, notificacion.id, 'Comanda lista para modificar'),
          error: (err) => this.handleActionError(notificacion.id, err),
        });
        break;
    }
  }

  formatDateTime(value?: string): string {
    if (!value) {
      return 'Sin hora';
    }

    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return value;
    }

    return parsed.toLocaleString('es-CO', {
      dateStyle: 'short',
      timeStyle: 'short',
    });
  }

  private resolveActionType(tipo: string): 'ATENCION' | 'PLATOS_LISTOS' | 'BEBIDAS_LISTAS' | 'CAMBIO' | null {
    switch (tipo?.toUpperCase()) {
      case 'ATENCION':
      case 'AT':
        return 'ATENCION';
      case 'PLATOS_LISTOS':
      case 'PL':
        return 'PLATOS_LISTOS';
      case 'BEBIDAS_LISTAS':
      case 'BE':
        return 'BEBIDAS_LISTAS';
      case 'CAMBIO':
      case 'CA':
        return 'CAMBIO';
      default:
        return null;
    }
  }

  private handleActionSuccess(mesaId: string, notificacionId: string, message: string): void {
    this.setPending(notificacionId, false);
    this.removeNotification(mesaId, notificacionId);
    this.actionMessage.set(message);
    this.actionTone.set('success');
    this.loadMapa(false);
  }

  private handleActionError(notificacionId: string, err: unknown): void {
    this.setPending(notificacionId, false);
    this.actionMessage.set(this.resolveErrorMessage(err));
    this.actionTone.set('error');
  }

  private removeNotification(mesaId: string, notificacionId: string): void {
    const current = this.mapa();
    const zonas = current.zonas.map((zona) => ({
      ...zona,
      mesas: zona.mesas.map((mesa) =>
        mesa.id === mesaId
          ? {
              ...mesa,
              notificaciones: mesa.notificaciones.filter((item) => item.id !== notificacionId),
            }
          : mesa
      ),
    }));

    this.mapa.set({ ...current, zonas });
  }

  private setPending(notificacionId: string, pending: boolean): void {
    const next = new Set(this.pendingNotifications());
    if (pending) {
      next.add(notificacionId);
    } else {
      next.delete(notificacionId);
    }
    this.pendingNotifications.set(next);
  }

  private resolveErrorMessage(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      if (typeof err.error?.message === 'string' && err.error.message.trim().length > 0) {
        return err.error.message;
      }
      if (typeof err.error === 'string' && err.error.trim().length > 0) {
        return err.error;
      }
    }
    return 'No fue posible completar la acción.';
  }
}
