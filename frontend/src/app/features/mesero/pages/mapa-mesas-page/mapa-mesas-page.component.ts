import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, timer } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MesaMapService, MesaDetalle, MesaMapaItem, MesaZonaMapa } from '../../../../core/services/mesa-map.service';
import { WebSocketService } from '../../../../core/services/websocket.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-mapa-mesas-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent],
  template: `
    <section class="page-grid mapa-mesas-shell">
      <app-page-header title="Mapa de mesas" subtitle="Control rapido por zonas y estado"></app-page-header>

      <article class="card mapa-panel">
        <div class="mapa-state" *ngIf="loading()">Cargando mesas...</div>
        <div class="mapa-state error" *ngIf="!loading() && errorMessage()">
          <p>{{ errorMessage() }}</p>
          <button class="btn-outline" type="button" (click)="loadMapa()">Reintentar</button>
        </div>

        <ng-container *ngIf="!loading() && !errorMessage()">
          <div class="mapa-empty" *ngIf="!mapa().zonas.length">
            <p>No hay mesas registradas.</p>
          </div>

          <section class="zone-section" *ngFor="let zona of mapa().zonas">
            <div class="zone-head">
              <button class="zone-toggle" type="button" (click)="toggleZone(zona.id)">
                <div>
                  <h3 class="zone-title">{{ zona.name }}</h3>
                  <p class="zone-sub">{{ zona.count }} mesas</p>
                </div>
                <span class="zone-caret">{{ isZoneExpanded(zona.id) ? '-' : '+' }}</span>
              </button>
              <button class="btn-outline btn-compact" type="button" (click)="goToAgregarMesa()">
                Agregar mesa
              </button>
            </div>

            <div class="zone-body" *ngIf="isZoneExpanded(zona.id)">
              <div *ngIf="zona.mesas.length; else emptyZone">
                <div class="mesa-grid">
                  <article
                    class="mesa-card"
                    *ngFor="let mesa of zona.mesas"
                    [ngClass]="[statusClass(mesa.estado), mesa.esMesaPropia ? 'is-own' : '']"
                  >
                    <div class="mesa-top">
                      <div>
                        <p class="mesa-id">{{ mesa.identificador }}</p>
                        <p class="mesa-meta">{{ mesa.numeroPersonas }} personas</p>
                      </div>
                      <span class="mesa-status">{{ statusLabel(mesa.estado) }}</span>
                    </div>

                    <p class="mesa-mesero" *ngIf="!mesa.esMesaPropia && mesa.nombreMesero">
                      Mesero: {{ mesa.nombreMesero }}
                    </p>

                    <div class="mesa-tags" *ngIf="mesa.tieneBorrador || mesa.notificaciones.length">
                      <span class="tag tag-draft" *ngIf="mesa.tieneBorrador">Borrador</span>
                      <span
                        class="tag"
                        *ngFor="let notificacion of mesa.notificaciones"
                        [attr.title]="notificationTitle(notificacion.tipo)"
                      >
                        {{ notificationShort(notificacion.tipo) }}
                      </span>
                    </div>

                    <div class="mesa-actions">
                      <button class="btn-link" type="button" (click)="openDetalle(mesa)">Ver</button>
                      <button class="btn-primary" type="button" (click)="goToModificarComanda(mesa)">
                        Modificar
                      </button>
                    </div>
                  </article>
                </div>
              </div>

              <ng-template #emptyZone>
                <div class="empty-zone">
                  <p>No hay mesas registradas en esta zona.</p>
                </div>
              </ng-template>
            </div>
          </section>
        </ng-container>
      </article>

      <div class="modal-backdrop" *ngIf="selectedMesa() as detail">
        <div class="modal-card">
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
                <p class="detail-label">Estado</p>
                <p class="detail-value">{{ statusLabel(detail.estado ?? '') }}</p>
              </div>
              <div>
                <p class="detail-label">Personas</p>
                <p class="detail-value">{{ detail.numeroPersonas ?? 0 }}</p>
              </div>
              <div>
                <p class="detail-label">Cliente</p>
                <p class="detail-value">{{ detail.nombreCliente ?? 'Sin asignar' }}</p>
              </div>
              <div>
                <p class="detail-label">Hora llegada</p>
                <p class="detail-value">{{ formatDateTime(detail.horaLlegada) }}</p>
              </div>
            </div>

            <div class="detail-notes" *ngIf="detail.notasReserva || detail.notasMesa || detail.notasComandas">
              <p class="detail-label">Notas</p>
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
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      :host {
        --mesa-primary: #2c1810;
        --mesa-secondary: #c41e3a;
        --mesa-accent: #e8d5b7;
        --mesa-muted: #a0a0a0;
        --mesa-text: #333333;
        display: block;
        color: var(--mesa-text);
        font-family: 'Manrope', 'Montserrat', 'Segoe UI', sans-serif;
      }

      .mapa-mesas-shell {
        gap: 1rem;
      }

      .mapa-panel {
        padding: 1rem;
        background: radial-gradient(circle at top right, rgba(232, 213, 183, 0.4), #ffffff 55%);
        border: 1px solid rgba(44, 24, 16, 0.1);
      }

      .mapa-state {
        display: grid;
        place-items: center;
        min-height: 160px;
        color: var(--mesa-muted);
        text-align: center;
      }

      .mapa-state.error {
        color: var(--mesa-secondary);
      }

      .mapa-empty {
        text-align: center;
        color: var(--mesa-muted);
        display: grid;
        gap: 0.75rem;
        justify-items: center;
      }

      .zone-section {
        border: 1px solid rgba(44, 24, 16, 0.12);
        border-radius: 16px;
        background: #ffffff;
        overflow: hidden;
        margin-bottom: 0.75rem;
        animation: fadeUp 0.35s ease;
      }

      .zone-head {
        display: flex;
        align-items: center;
        gap: 0.6rem;
        padding: 0.45rem;
        background: #fffaf3;
      }

      .zone-toggle {
        flex: 1;
        border: none;
        background: transparent;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        padding: 0.3rem 0.45rem;
        text-align: left;
        cursor: pointer;
      }

      .zone-caret {
        font-size: 1.1rem;
        font-weight: 600;
        color: var(--mesa-secondary);
      }

      .zone-body {
        padding: 0.75rem 0.9rem 1rem;
        border-top: 1px dashed rgba(44, 24, 16, 0.12);
      }

      .zone-title {
        margin: 0;
        font-size: 0.98rem;
        color: var(--mesa-primary);
      }

      .zone-sub {
        margin: 0.1rem 0 0;
        color: var(--mesa-muted);
        font-size: 0.78rem;
      }

      .mesa-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
        gap: 0.75rem;
      }

      .mesa-card {
        border: 1px solid rgba(44, 24, 16, 0.12);
        border-radius: 16px;
        padding: 0.85rem;
        background: #ffffff;
        display: grid;
        gap: 0.5rem;
        box-shadow: 0 8px 18px rgba(44, 24, 16, 0.08);
        transition: transform 0.2s ease, box-shadow 0.2s ease;
      }

      .mesa-card.is-own {
        border-color: rgba(196, 30, 58, 0.5);
        box-shadow: 0 10px 22px rgba(196, 30, 58, 0.2);
      }

      .mesa-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 12px 24px rgba(44, 24, 16, 0.15);
      }

      .mesa-top {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 0.5rem;
      }

      .mesa-id {
        margin: 0;
        font-size: 1.1rem;
        font-weight: 700;
        color: var(--mesa-primary);
      }

      .mesa-meta {
        margin: 0.2rem 0 0;
        color: var(--mesa-muted);
        font-size: 0.78rem;
      }

      .mesa-status {
        font-size: 0.72rem;
        padding: 0.2rem 0.55rem;
        border-radius: 999px;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.04em;
      }

      .mesa-mesero {
        margin: 0;
        font-size: 0.78rem;
        color: var(--mesa-text);
      }

      .mesa-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 0.4rem;
      }

      .tag {
        border-radius: 999px;
        padding: 0.15rem 0.5rem;
        font-size: 0.7rem;
        background: rgba(44, 24, 16, 0.08);
        color: var(--mesa-primary);
        font-weight: 600;
      }

      .tag-draft {
        background: rgba(196, 30, 58, 0.12);
        color: var(--mesa-secondary);
      }

      .mesa-actions {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 0.5rem;
      }

      .btn-link {
        border: none;
        background: none;
        color: var(--mesa-secondary);
        font-weight: 600;
        cursor: pointer;
        padding: 0;
      }

      .btn-primary {
        border: none;
        background: var(--mesa-secondary);
        color: #ffffff;
        border-radius: 999px;
        padding: 0.35rem 0.9rem;
        font-size: 0.8rem;
        cursor: pointer;
      }

      .btn-outline {
        border: 1px solid rgba(44, 24, 16, 0.25);
        background: #ffffff;
        color: var(--mesa-primary);
        border-radius: 999px;
        padding: 0.35rem 0.9rem;
        font-size: 0.78rem;
        cursor: pointer;
      }

      .btn-compact {
        padding: 0.28rem 0.7rem;
        font-size: 0.72rem;
        white-space: nowrap;
      }

      .state-attended .mesa-status {
        background: rgba(41, 98, 150, 0.2);
        color: #296296;
      }

      .state-prep .mesa-status {
        background: rgba(46, 125, 50, 0.2);
        color: #2e7d32;
      }

      .state-wait .mesa-status {
        background: rgba(107, 114, 128, 0.2);
        color: #6b7280;
      }

      .empty-zone {
        display: grid;
        justify-items: center;
        gap: 0.5rem;
        padding: 1rem 0;
        color: var(--mesa-muted);
      }

      .modal-backdrop {
        position: fixed;
        inset: 0;
        background: rgba(44, 24, 16, 0.45);
        display: grid;
        place-items: center;
        padding: 1rem;
        z-index: 20;
      }

      .modal-card {
        background: #ffffff;
        border-radius: 18px;
        max-width: 560px;
        width: 100%;
        padding: 1.2rem;
        box-shadow: 0 18px 40px rgba(44, 24, 16, 0.2);
        animation: fadeUp 0.3s ease;
      }

      .modal-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        margin-bottom: 1rem;
      }

      .modal-eyebrow {
        margin: 0;
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        color: var(--mesa-muted);
      }

      .modal-body {
        display: grid;
        gap: 1rem;
      }

      .detail-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
        gap: 0.75rem;
      }

      .detail-label {
        margin: 0;
        font-size: 0.72rem;
        text-transform: uppercase;
        letter-spacing: 0.06em;
        color: var(--mesa-muted);
      }

      .detail-value {
        margin: 0.2rem 0 0;
        font-size: 0.85rem;
        color: var(--mesa-primary);
        font-weight: 600;
      }

      .detail-notes {
        background: rgba(232, 213, 183, 0.35);
        border-radius: 12px;
        padding: 0.75rem;
        display: grid;
        gap: 0.4rem;
      }

      .detail-block h4 {
        margin: 0 0 0.5rem;
        font-size: 0.9rem;
        color: var(--mesa-primary);
      }

      .detail-list {
        list-style: none;
        padding: 0;
        margin: 0;
        display: grid;
        gap: 0.5rem;
      }

      .detail-list li {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 0.75rem;
        padding: 0.5rem 0.6rem;
        border-radius: 10px;
        background: rgba(44, 24, 16, 0.05);
      }

      .detail-item {
        margin: 0;
        font-size: 0.85rem;
        font-weight: 600;
      }

      .detail-sub {
        margin: 0.1rem 0 0;
        font-size: 0.75rem;
        color: var(--mesa-muted);
      }

      .detail-qty {
        font-weight: 700;
        color: var(--mesa-secondary);
      }

      .detail-empty {
        color: var(--mesa-muted);
        font-size: 0.85rem;
      }

      @keyframes fadeUp {
        from {
          opacity: 0;
          transform: translateY(8px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      @media (max-width: 720px) {
        .mesa-actions {
          flex-direction: column;
          align-items: flex-start;
        }
      }
    `,
  ],
})
export class MapaMesasPageComponent implements OnInit, OnDestroy {
  private readonly mesaService = inject(MesaMapService);
  private readonly webSocket = inject(WebSocketService);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly mapa = signal<{ zonas: MesaZonaMapa[]; totalMesas: number }>({ zonas: [], totalMesas: 0 });
  readonly expandedZoneId = signal<string | null>(null);
  readonly selectedMesa = signal<MesaDetalle | null>(null);

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

    this.mesaService.getMapa().pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.mapa.set(data);
        const expanded = this.expandedZoneId();
        if (expanded && !data.zonas.some((zona) => zona.id === expanded)) {
          this.expandedZoneId.set(null);
        }
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('No pudimos cargar el mapa de mesas.');
        this.loading.set(false);
      },
    });
  }

  toggleZone(zoneId: string): void {
    this.expandedZoneId.set(this.expandedZoneId() === zoneId ? null : zoneId);
  }

  isZoneExpanded(zoneId: string): boolean {
    return this.expandedZoneId() === zoneId;
  }

  openDetalle(mesa: MesaMapaItem): void {
    this.selectedMesa.set({
      mesaId: mesa.id,
      identificador: mesa.identificador,
      itemsComanda: [],
    });

    this.mesaService.getDetalle(mesa.id).pipe(takeUntil(this.destroy$)).subscribe({
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

  goToAgregarMesa(): void {
    this.router.navigate(['/app/mesero/llegada-reserva'], {
      state: { openAsignacion: true, origen: 'mapa' },
    });
  }

  goToModificarComanda(mesa: MesaMapaItem): void {
    this.router.navigate(['/app/mesero/comanda-editor'], { queryParams: { mesaId: mesa.id } });
  }

  statusLabel(status: string): string {
    const normalized = status?.toUpperCase() ?? '';
    switch (normalized) {
      case 'ATENDIDA':
        return 'Atendida';
      case 'EN_PREPARACION':
        return 'En preparacion';
      case 'ESPERA':
        return 'En espera';
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
        return 'state-default';
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

  notificationTitle(tipo: string): string {
    switch (tipo?.toUpperCase()) {
      case 'ATENCION':
      case 'AT':
        return 'Atencion solicitada';
      case 'PLATOS_LISTOS':
      case 'PL':
        return 'Platos listos';
      case 'BEBIDAS_LISTAS':
      case 'BE':
        return 'Bebidas listas';
      case 'CAMBIO':
      case 'CA':
        return 'Solicitar cambio';
      default:
        return tipo ?? '';
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
}
