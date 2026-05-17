import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { ComandaProduccionService } from '../../../../core/services/comanda-produccion.service';
import { WebSocketService } from '../../../../core/services/websocket.service';
import {
  BackendComandaProduccionDetalle,
  BackendComandaProduccionResumen,
  BackendTableroProduccion,
} from '../../../../core/models/api.models';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-comandas-board-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent, DatePipe],
  templateUrl: './comandas-board-page.component.html',
  styleUrls: ['./comandas-board-page.component.scss'],
})
export class ComandasBoardPageComponent implements OnInit, OnDestroy {
  tablero: BackendTableroProduccion | null = null;
  loading = true;
  actionLoading: Record<number, boolean> = {};
  error = '';

  // ── Modal de detalle ──
  detalleVisible = false;
  detalleCargando = false;
  detalleData: BackendComandaProduccionDetalle | null = null;

  private wsSubscriptions: Subscription[] = [];
  private estacionesSuscritas = new Set<string>();

  constructor(
    private readonly produccionService: ComandaProduccionService,
    private readonly wsService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.cargarTablero();
  }

  ngOnDestroy(): void {
    this.wsSubscriptions.forEach((sub) => sub.unsubscribe());
  }

  cargarTablero(): void {
    this.loading = true;
    this.error = '';
    this.produccionService.obtenerTablero().subscribe({
      next: (data) => {
        this.tablero = data;
        this.loading = false;
        this.suscribirAEstaciones(data.estaciones);
      },
      error: () => {
        this.error = 'No se pudo cargar el tablero de producción.';
        this.loading = false;
      },
    });
  }

  /**
   * Suscribe a los tópicos WS de las estaciones del usuario para actualizar
   * el tablero automáticamente ante nuevas comandas o cambios de estado.
   */
  private suscribirAEstaciones(estaciones: string[]): void {
    estaciones.forEach((estacion) => {
      if (!this.estacionesSuscritas.has(estacion)) {
        this.estacionesSuscritas.add(estacion);
        const topic = `/topic/produccion/${estacion}`;
        const sub = this.wsService.subscribe<any>(topic).subscribe((msg) => {
          console.log(`[WS Producción] Evento en ${estacion}:`, msg);
          // Recarga silenciosa para mantener los datos actualizados
          this.produccionService.obtenerTablero().subscribe((data) => {
            this.tablero = data;
          });
        });
        this.wsSubscriptions.push(sub);
      }
    });
  }

  iniciarPreparacion(comanda: BackendComandaProduccionResumen): void {
    if (this.actionLoading[comanda.comandaId]) return;
    this.actionLoading[comanda.comandaId] = true;
    this.produccionService.iniciarPreparacion(comanda.comandaId).subscribe({
      next: () => {
        this.actionLoading[comanda.comandaId] = false;
        this.cargarTablero();
      },
      error: () => {
        this.actionLoading[comanda.comandaId] = false;
      },
    });
  }

  marcarListo(comanda: BackendComandaProduccionResumen): void {
    if (this.actionLoading[comanda.comandaId]) return;
    this.actionLoading[comanda.comandaId] = true;
    this.produccionService.marcarListo(comanda.comandaId).subscribe({
      next: () => {
        this.actionLoading[comanda.comandaId] = false;
        this.cargarTablero();
      },
      error: () => {
        this.actionLoading[comanda.comandaId] = false;
      },
    });
  }

  verDetalle(comanda: BackendComandaProduccionResumen): void {
    this.detalleVisible = true;
    this.detalleCargando = true;
    this.detalleData = null;
    this.produccionService.obtenerDetalle(comanda.comandaId).subscribe({
      next: (data) => {
        this.detalleData = data;
        this.detalleCargando = false;
      },
      error: () => {
        this.detalleCargando = false;
      },
    });
  }

  cerrarDetalle(): void {
    this.detalleVisible = false;
    this.detalleData = null;
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('detalle-overlay')) {
      this.cerrarDetalle();
    }
  }

  imprimirComanda(comanda: BackendComandaProduccionResumen): void {
    window.print();
  }

  notificarCambio(comanda: BackendComandaProduccionResumen): void {
    if (this.actionLoading[comanda.comandaId]) return;
    this.actionLoading[comanda.comandaId] = true;
    this.produccionService.notificarCambio(comanda.comandaId).subscribe({
      next: () => {
        this.actionLoading[comanda.comandaId] = false;
        alert(`Notificación de cambio enviada para la comanda de la mesa ${comanda.mesaIdentificador}`);
      },
      error: () => {
        this.actionLoading[comanda.comandaId] = false;
        alert('Error al enviar la notificación de cambio.');
      },
    });
  }

  tiempoTranscurrido(fecha: string | undefined): string {
    if (!fecha) return '';
    const diff = Date.now() - new Date(fecha).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'Hace un momento';
    if (mins < 60) return `Hace ${mins} min`;
    const hrs = Math.floor(mins / 60);
    return `Hace ${hrs}h ${mins % 60}m`;
  }

  estadoLegible(estado: string): string {
    switch (estado) {
      case 'PENDIENTE': return 'Pendiente';
      case 'EN_PREPARACION': return 'En preparación';
      case 'LISTO': return 'Lista para servir';
      default: return estado;
    }
  }

  trackByComandaId(_: number, comanda: BackendComandaProduccionResumen): number {
    return comanda.comandaId;
  }
}
