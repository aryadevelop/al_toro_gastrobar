import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import {
  ApiEnvelope,
  BackendProductoAdminItem,
  BackendProductoInventarioResponse,
  BackendCambiarEstadoRequest,
  BackendValidacionCambioEstado,
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class ProductoAdminService {
  constructor(private readonly http: HttpClient) {}

  listarProductosVentaDirecta(): Observable<ApiEnvelope<BackendProductoAdminItem[]>> {
    return this.http.get<ApiEnvelope<BackendProductoAdminItem[]>>(API_PATHS.adminProductos.listar);
  }

  /** Lista todos los productos de inventario con su stock actual. */
  listarInventario(): Observable<ApiEnvelope<BackendProductoInventarioResponse[]>> {
    return this.http.get<ApiEnvelope<BackendProductoInventarioResponse[]>>(API_PATHS.adminProductos.listar);
  }

  validarCambioEstado(productoId: number | string): Observable<ApiEnvelope<BackendValidacionCambioEstado>> {
    return this.http.get<ApiEnvelope<BackendValidacionCambioEstado>>(API_PATHS.adminProductos.validarEstado(productoId));
  }

  cambiarEstado(productoId: number | string, request: BackendCambiarEstadoRequest): Observable<ApiEnvelope<void>> {
    return this.http.put<ApiEnvelope<void>>(API_PATHS.adminProductos.cambiarEstado(productoId), request);
  }
}
