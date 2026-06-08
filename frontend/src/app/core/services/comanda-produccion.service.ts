import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import {
  ApiEnvelope,
  BackendComandaProduccionDetalle,
  BackendComandaProduccionResumen,
  BackendTableroProduccion,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ComandaProduccionService {
  private readonly http = inject(HttpClient);

  /** GET /api/comandas/produccion – tablero con las 3 columnas */
  obtenerTablero(): Observable<BackendTableroProduccion> {
    return this.http
      .get<ApiEnvelope<BackendTableroProduccion>>(API_PATHS.comandasProduccion.tablero)
      .pipe(map((r) => r.data));
  }

  /** GET /api/comandas/produccion/:id – detalle de una comanda */
  obtenerDetalle(comandaId: number): Observable<BackendComandaProduccionDetalle> {
    return this.http
      .get<ApiEnvelope<BackendComandaProduccionDetalle>>(API_PATHS.comandasProduccion.detalle(comandaId))
      .pipe(map((r) => r.data));
  }

  /** POST /api/comandas/produccion/:id/iniciar – PENDIENTE → EN_PREPARACION */
  iniciarPreparacion(comandaId: number): Observable<BackendComandaProduccionResumen> {
    return this.http
      .post<ApiEnvelope<BackendComandaProduccionResumen>>(API_PATHS.comandasProduccion.iniciar(comandaId), {})
      .pipe(map((r) => r.data));
  }

  /** POST /api/comandas/produccion/:id/listo – EN_PREPARACION → LISTO */
  marcarListo(comandaId: number): Observable<BackendComandaProduccionResumen> {
    return this.http
      .post<ApiEnvelope<BackendComandaProduccionResumen>>(API_PATHS.comandasProduccion.listo(comandaId), {})
      .pipe(map((r) => r.data));
  }

  /** POST /api/notificaciones/cambio – notifica cambio de comanda */
  notificarCambio(comandaId: number): Observable<any> {
    return this.http
      .post<ApiEnvelope<any>>(API_PATHS.notificaciones.cambio, { comandaId })
      .pipe(map((r) => r.data));
  }
}
