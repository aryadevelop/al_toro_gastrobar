import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { ApiEnvelope, BackendProductoAdminItem } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class ProductoAdminService {
  constructor(private readonly http: HttpClient) {}

  listarProductosVentaDirecta(): Observable<ApiEnvelope<BackendProductoAdminItem[]>> {
    return this.http.get<ApiEnvelope<BackendProductoAdminItem[]>>(API_PATHS.adminProductos.listar);
  }

  validarCambioEstado(productoId: number | string): Observable<ApiEnvelope<import('../models/api.models').BackendValidacionCambioEstado>> {
    return this.http.get<ApiEnvelope<import('../models/api.models').BackendValidacionCambioEstado>>(API_PATHS.adminProductos.validarEstado(productoId));
  }

  cambiarEstado(productoId: number | string, request: import('../models/api.models').BackendCambiarEstadoRequest): Observable<ApiEnvelope<void>> {
    return this.http.put<ApiEnvelope<void>>(API_PATHS.adminProductos.cambiarEstado(productoId), request);
  }
}
