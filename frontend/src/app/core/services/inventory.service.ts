import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { ApiEnvelope, BackendAjusteInventarioRequest, BackendInventarioItemBusqueda } from '../models/api.models';
import { MOCK_DECORACIONES, MOCK_INSUMOS, MOCK_PREPARACIONES, MOCK_PRODUCTOS } from '../mocks/restaurant.mock';
import { Decoracion, Insumo, Preparacion, Producto } from '../models/domain.models';
import { MockApiService } from './mock-api.service';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  constructor(
    private readonly mockApiService: MockApiService,
    private readonly http: HttpClient
  ) {}

  listPreparaciones(): Observable<Preparacion[]> {
    return this.mockApiService.respond([...MOCK_PREPARACIONES]);
  }

  listProductos(): Observable<Producto[]> {
    return this.mockApiService.respond([...MOCK_PRODUCTOS]);
  }

  listInsumos(): Observable<Insumo[]> {
    return this.mockApiService.respond([...MOCK_INSUMOS]);
  }

  listDecoraciones(): Observable<Decoracion[]> {
    return this.mockApiService.respond([...MOCK_DECORACIONES]);
  }

  buscarItemsInventario(query: string): Observable<ApiEnvelope<BackendInventarioItemBusqueda[]>> {
    const params = new HttpParams().set('q', query);
    return this.http.get<ApiEnvelope<BackendInventarioItemBusqueda[]>>(API_PATHS.inventario.buscar, { params });
  }

  registrarAjuste(request: BackendAjusteInventarioRequest): Observable<ApiEnvelope<void>> {
    return this.http.post<ApiEnvelope<void>>(API_PATHS.inventario.ajuste, request);
  }
}