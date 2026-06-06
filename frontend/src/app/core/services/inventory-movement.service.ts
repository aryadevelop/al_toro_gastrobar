import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { ApiEnvelope, BackendInventarioMovimientoRequest, BackendInventarioItemBusqueda } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class InventoryMovementService {
  constructor(private readonly http: HttpClient) {}

  registrarMovimiento(request: BackendInventarioMovimientoRequest): Observable<ApiEnvelope<void>> {
    return this.http.post<ApiEnvelope<void>>(API_PATHS.inventarioMovimientos, request);
  }

  buscarItems(query: string): Observable<ApiEnvelope<BackendInventarioItemBusqueda[]>> {
    return this.http.get<ApiEnvelope<BackendInventarioItemBusqueda[]>>(API_PATHS.inventarioBuscar(query));
  }
}
