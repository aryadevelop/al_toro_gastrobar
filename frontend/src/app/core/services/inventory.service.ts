import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MOCK_DECORACIONES, MOCK_INSUMOS, MOCK_PREPARACIONES, MOCK_PRODUCTOS } from '../mocks/restaurant.mock';
import { Decoracion, Insumo, Preparacion, Producto } from '../models/domain.models';
import { MockApiService } from './mock-api.service';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  constructor(private readonly mockApiService: MockApiService) {}

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
}