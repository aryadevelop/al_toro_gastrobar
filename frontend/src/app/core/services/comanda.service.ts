import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MOCK_COMANDAS } from '../mocks/restaurant.mock';
import { Comanda } from '../models/domain.models';
import { MockApiService } from './mock-api.service';

@Injectable({ providedIn: 'root' })
export class ComandaService {
  constructor(private readonly mockApiService: MockApiService) {}

  list(): Observable<Comanda[]> {
    return this.mockApiService.respond([...MOCK_COMANDAS]);
  }

  updateStatus(id: string, status: Comanda['status']): Observable<Comanda | null> {
    const index = MOCK_COMANDAS.findIndex((item) => item.id === id);
    if (index < 0) {
      return this.mockApiService.respond(null);
    }

    MOCK_COMANDAS[index] = {
      ...MOCK_COMANDAS[index],
      status
    };
    return this.mockApiService.respond(MOCK_COMANDAS[index], 350);
  }
}