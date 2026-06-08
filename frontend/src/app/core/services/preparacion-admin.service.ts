import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { ApiEnvelope, BackendValidacionCambioEstado, BackendCambiarEstadoRequest, BackendEstadoHistorial } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class PreparacionAdminService {
  constructor(private readonly http: HttpClient) {}

  listarPreparaciones(): Observable<ApiEnvelope<any[]>> {
    return this.http.get<ApiEnvelope<any[]>>(`${API_PATHS.adminPreparaciones.cambiarEstado('').replace('/estado', '')}`);
  }

  validarCambioEstado(id: number | string): Observable<ApiEnvelope<BackendValidacionCambioEstado>> {
    return this.http.get<ApiEnvelope<BackendValidacionCambioEstado>>(API_PATHS.adminPreparaciones.validarEstado(id));
  }

  cambiarEstado(id: number | string, request: BackendCambiarEstadoRequest): Observable<ApiEnvelope<void>> {
    return this.http.post<ApiEnvelope<void>>(API_PATHS.adminPreparaciones.cambiarEstado(id), request);
  }

  obtenerHistorialEstados(id: number | string): Observable<ApiEnvelope<BackendEstadoHistorial[]>> {
    return this.http.get<ApiEnvelope<BackendEstadoHistorial[]>>(API_PATHS.adminPreparaciones.historialEstados(id));
  }
}
