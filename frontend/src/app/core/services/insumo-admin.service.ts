import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import {
  ApiEnvelope,
  BackendInsumoDetalleResponse,
  BackendMovimientoHistorialItem,
  BackendCambiarEstadoRequest,
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class InsumoAdminService {
  constructor(private readonly http: HttpClient) {}

  listarInsumos(): Observable<ApiEnvelope<any[]>> {
    return this.http.get<ApiEnvelope<any[]>>(API_PATHS.adminInsumos.listar);
  }

  obtenerDetalle(id: number | string): Observable<ApiEnvelope<BackendInsumoDetalleResponse>> {
    return this.http.get<ApiEnvelope<BackendInsumoDetalleResponse>>(API_PATHS.adminInsumos.detalle(id));
  }

  validarCambioEstado(id: number | string): Observable<ApiEnvelope<any>> {
    return this.http.get<ApiEnvelope<any>>(API_PATHS.adminInsumos.validarEstado(id));
  }

  cambiarEstado(id: number | string, request: BackendCambiarEstadoRequest): Observable<ApiEnvelope<void>> {
    return this.http.put<ApiEnvelope<void>>(API_PATHS.adminInsumos.cambiarEstado(id), request);
  }

  listarMovimientos(insumoId: number | string): Observable<ApiEnvelope<BackendMovimientoHistorialItem[]>> {
    return this.http.get<ApiEnvelope<BackendMovimientoHistorialItem[]>>(
      API_PATHS.inventarioMovimientosPorInsumo(insumoId)
    );
  }
}
