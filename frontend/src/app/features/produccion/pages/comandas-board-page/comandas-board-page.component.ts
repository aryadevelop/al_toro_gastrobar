import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription, forkJoin } from 'rxjs';
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
  listosAgrupadosData: any[] = [];

  /** Mensaje de retroalimentación temporal (toast inline) */
  toastMessage = '';
  toastTone: 'success' | 'error' = 'success';
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  // ── Modal de detalle ──
  detalleVisible = false;
  detalleCargando = false;
  detalleData: BackendComandaProduccionDetalle | null = null;

  // ── Impresión ──
  printData: BackendComandaProduccionDetalle | null = null;
  isReimpresion = false;
  printIsAdicion = false;

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
        this.actualizarListosAgrupados();
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
        const sub = this.wsService.subscribe<any>(topic).subscribe(() => {
          // Recarga silenciosa para mantener los datos actualizados
          this.produccionService.obtenerTablero().subscribe((data) => {
            this.tablero = data;
            this.actualizarListosAgrupados();
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

  private actualizarListosAgrupados(): void {
    if (!this.tablero?.listos) {
      this.listosAgrupadosData = [];
      return;
    }
    
    const map = new Map<string, any>();
    
    for (const comanda of this.tablero.listos) {
      const key = `${comanda.mesaIdentificador}-${comanda.estacion}`;
      if (!map.has(key)) {
        map.set(key, {
          ...comanda,
          isGroup: true,
          groupId: key,
          comandasIds: [comanda.comandaId],
          totalItems: comanda.totalItems
        });
      } else {
        const group = map.get(key);
        group.comandasIds.push(comanda.comandaId);
        group.totalItems += comanda.totalItems;
        
        // Conservar la fecha de creación de la comanda original y la fecha "listo" más reciente
        if (new Date(comanda.createdAt) < new Date(group.createdAt)) {
          group.createdAt = comanda.createdAt;
        }
        if (new Date(comanda.fechaHoraListo) > new Date(group.fechaHoraListo)) {
          group.fechaHoraListo = comanda.fechaHoraListo;
        }
      }
    }
    
    this.listosAgrupadosData = Array.from(map.values());
  }

  verDetalle(comanda: any): void {
    this.detalleVisible = true;
    this.detalleCargando = true;
    this.detalleData = null;

    if (comanda.isGroup && comanda.comandasIds.length > 1) {
      const requests = comanda.comandasIds.map((id: number) => this.produccionService.obtenerDetalle(id));
      forkJoin(requests).subscribe({
        next: (responses: any[]) => {
          const merged: any = { ...responses[0] };
          merged.comandaId = comanda.comandasIds.join(', ');
          merged.platos = [];
          merged.bebidas = [];
          merged.otros = [];
          merged.notas = [];
          
          responses.forEach(res => {
            if (res.platos) merged.platos.push(...res.platos);
            if (res.bebidas) merged.bebidas.push(...res.bebidas);
            if (res.otros) merged.otros.push(...res.otros);
            if (res.notas) merged.notas.push(res.notas);
          });
          
          merged.notas = merged.notas.filter((n: string) => !!n).join(' | ');
          this.detalleData = merged;
          this.detalleCargando = false;
        },
        error: () => {
          this.detalleCargando = false;
        }
      });
    } else {
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

  imprimirComanda(comanda: any, isReimpresion = false): void {
    this.isReimpresion = isReimpresion;
    this.printIsAdicion = false;

    // Verificar si es adición: hay alguna comanda de la misma mesa y estación con fecha anterior
    if (this.tablero) {
      const allComandas = [
        ...this.tablero.pendientes,
        ...this.tablero.enPreparacion,
        ...this.tablero.listos
      ];
      const isOlderExisting = allComandas.some(c => 
        c.mesaIdentificador === comanda.mesaIdentificador &&
        c.estacion === comanda.estacion &&
        c.comandaId !== comanda.comandaId &&
        new Date(c.createdAt) < new Date(comanda.createdAt)
      );
      if (isOlderExisting) {
        this.printIsAdicion = true;
      }
    }

    const doPrint = () => {
      document.body.classList.add('is-printing-ticket');
      setTimeout(() => {
        window.print();
        document.body.classList.remove('is-printing-ticket');
        this.printData = null;
      }, 200);
    };

    // Si ya es un detalle cargado (desde el modal)
    if (comanda.platos || comanda.bebidas) {
      this.printData = comanda;
      doPrint();
      return;
    }

    // Si es desde la tarjeta, obtenemos el detalle para imprimir
    if (this.actionLoading[comanda.comandaId]) return;
    this.actionLoading[comanda.comandaId] = true;
    
    this.produccionService.obtenerDetalle(comanda.comandaId).subscribe({
      next: (data) => {
        this.actionLoading[comanda.comandaId] = false;
        this.printData = data;
        doPrint();
      },
      error: () => {
        this.actionLoading[comanda.comandaId] = false;
        this.mostrarToast('Error al obtener datos para impresión', 'error');
      }
    });
  }

  notificarCambio(comanda: BackendComandaProduccionResumen): void {
    if (this.actionLoading[comanda.comandaId]) return;
    this.actionLoading[comanda.comandaId] = true;
    this.produccionService.notificarCambio(comanda.comandaId).subscribe({
      next: () => {
        this.actionLoading[comanda.comandaId] = false;
        this.mostrarToast(`Notificación de cambio enviada para la comanda de la mesa "${comanda.mesaIdentificador}"`, 'success');
      },
      error: () => {
        this.actionLoading[comanda.comandaId] = false;
        this.mostrarToast('Error al enviar la notificación de cambio.', 'error');
      },
    });
  }

  private mostrarToast(mensaje: string, tono: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage = mensaje;
    this.toastTone = tono;
    this.toastTimer = setTimeout(() => {
      this.toastMessage = '';
    }, 4000);
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

  trackByGrupo(_: number, group: any): string {
    return group.groupId;
  }
}
