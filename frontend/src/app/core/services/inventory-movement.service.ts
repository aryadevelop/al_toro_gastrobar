import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import {
  ApiEnvelope,
  BackendInventarioMovimientoRequest,
  BackendInventarioItemBusqueda,
  BackendMovimientoHistorialItem,
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class InventoryMovementService {
  constructor(private readonly http: HttpClient) {}

  /** Registra un ingreso o egreso usando el endpoint que acepta fecha opcional. */
  registrarMovimiento(request: BackendInventarioMovimientoRequest): Observable<ApiEnvelope<void>> {
    return this.http.post<ApiEnvelope<void>>(API_PATHS.inventarioMovimientosRegistro, request);
  }

  buscarItems(query: string): Observable<ApiEnvelope<BackendInventarioItemBusqueda[]>> {
    return this.http.get<ApiEnvelope<BackendInventarioItemBusqueda[]>>(API_PATHS.inventarioBuscar(query));
  }

  /** Obtiene el historial completo de movimientos de inventario. */
  listarHistorial(): Observable<ApiEnvelope<BackendMovimientoHistorialItem[]>> {
    return this.http.get<ApiEnvelope<BackendMovimientoHistorialItem[]>>(API_PATHS.inventarioMovimientos);
  }

  /** Filtra el historial por insumo. */
  listarHistorialPorInsumo(insumoId: number): Observable<ApiEnvelope<BackendMovimientoHistorialItem[]>> {
    return this.http.get<ApiEnvelope<BackendMovimientoHistorialItem[]>>(
      API_PATHS.inventarioMovimientosPorInsumo(insumoId)
    );
  }

  /** Filtra el historial por producto. */
  listarHistorialPorProducto(productoId: number): Observable<ApiEnvelope<BackendMovimientoHistorialItem[]>> {
    return this.http.get<ApiEnvelope<BackendMovimientoHistorialItem[]>>(
      API_PATHS.inventarioMovimientosPorProducto(productoId)
    );
  }
}
