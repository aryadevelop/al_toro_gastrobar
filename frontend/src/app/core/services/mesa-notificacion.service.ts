import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { ApiEnvelope, BackendAtenderCambioResponse } from '../models/api.models';

export interface AtenderCambioResult {
  comandaId: string;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class MesaNotificacionService {
  private readonly http = inject(HttpClient);

  atenderAsistencia(notificacionId: string): Observable<string> {
    return this.http
      .patch<ApiEnvelope<void>>(API_PATHS.notificaciones.atender(notificacionId), {})
      .pipe(map((response) => response.message ?? 'Atencion registrada'));
  }

  servirPlatos(notificacionId: string): Observable<string> {
    return this.http
      .patch<ApiEnvelope<void>>(API_PATHS.notificaciones.servirPlatos(notificacionId), {})
      .pipe(map((response) => response.message ?? 'Platos servidos'));
  }

  servirBebidas(notificacionId: string): Observable<string> {
    return this.http
      .patch<ApiEnvelope<void>>(API_PATHS.notificaciones.servirBebidas(notificacionId), {})
      .pipe(map((response) => response.message ?? 'Bebidas servidas'));
  }

  atenderCambio(notificacionId: string): Observable<AtenderCambioResult> {
    return this.http
      .patch<ApiEnvelope<BackendAtenderCambioResponse>>(API_PATHS.notificaciones.atenderCambio(notificacionId), {})
      .pipe(
        map((response) => ({
          comandaId: response.data?.comandaId ? String(response.data.comandaId) : '',
          message: response.message ?? 'Comanda lista para modificar',
        }))
      );
  }
}
