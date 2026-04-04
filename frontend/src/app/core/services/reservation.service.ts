import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { MOCK_RESERVAS } from '../mocks/restaurant.mock';
import { Reserva } from '../models/domain.models';
import { MockApiService } from './mock-api.service';

@Injectable({ providedIn: 'root' })
export class ReservationService {
  constructor(private readonly mockApiService: MockApiService) {}

  list(): Observable<Reserva[]> {
    return this.mockApiService.respond([...MOCK_RESERVAS]);
  }

  listByCliente(clienteId: string): Observable<Reserva[]> {
    return this.list().pipe(map((items) => items.filter((item) => item.clienteId === clienteId)));
  }

  create(payload: Omit<Reserva, 'id' | 'status'>): Observable<Reserva> {
    const created: Reserva = {
      ...payload,
      id: `r-${Date.now()}`,
      status: 'PENDING'
    };
    MOCK_RESERVAS.unshift(created);
    return this.mockApiService.respond(created, 500);
  }

  update(id: string, payload: Partial<Reserva>): Observable<Reserva | null> {
    const index = MOCK_RESERVAS.findIndex((item) => item.id === id);
    if (index < 0) {
      return this.mockApiService.respond(null);
    }

    MOCK_RESERVAS[index] = {
      ...MOCK_RESERVAS[index],
      ...payload
    };
    return this.mockApiService.respond(MOCK_RESERVAS[index], 450);
  }
}