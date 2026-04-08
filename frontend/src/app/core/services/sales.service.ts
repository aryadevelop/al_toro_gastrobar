import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MOCK_VENTAS } from '../mocks/restaurant.mock';
import { Venta } from '../models/domain.models';
import { MockApiService } from './mock-api.service';

@Injectable({ providedIn: 'root' })
export class SalesService {
  constructor(private readonly mockApiService: MockApiService) {}

  list(): Observable<Venta[]> {
    return this.mockApiService.respond([...MOCK_VENTAS]);
  }

  closeSale(payload: Omit<Venta, 'id' | 'createdAt'>): Observable<Venta> {
    const sale: Venta = {
      ...payload,
      id: `v-${Date.now()}`,
      createdAt: new Date().toISOString()
    };
    MOCK_VENTAS.unshift(sale);
    return this.mockApiService.respond(sale, 500);
  }
}