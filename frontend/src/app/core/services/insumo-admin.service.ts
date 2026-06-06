import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { ApiEnvelope, BackendValidacionDescontinuarInsumo, BackendCambiarEstadoRequest } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class InsumoAdminService {
  constructor(private readonly http: HttpClient) {}

  listarInsumos(): Observable<ApiEnvelope<any[]>> {
    return this.http.get<ApiEnvelope<any[]>>(`${API_PATHS.adminInsumos.cambiarEstado('').replace('/estado', '')}`);
  }

  validarCambioEstado(id: number | string): Observable<ApiEnvelope<BackendValidacionDescontinuarInsumo>> {
    return this.http.get<ApiEnvelope<BackendValidacionDescontinuarInsumo>>(API_PATHS.adminInsumos.validarEstado(id));
  }

  cambiarEstado(id: number | string, request: BackendCambiarEstadoRequest): Observable<ApiEnvelope<void>> {
    return this.http.post<ApiEnvelope<void>>(API_PATHS.adminInsumos.cambiarEstado(id), request);
  }
}
