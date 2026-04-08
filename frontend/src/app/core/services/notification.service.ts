import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MOCK_NOTIFICACIONES } from '../mocks/restaurant.mock';
import { Notificacion } from '../models/domain.models';
import { MockApiService } from './mock-api.service';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(private readonly mockApiService: MockApiService) {}

  list(): Observable<Notificacion[]> {
    return this.mockApiService.respond([...MOCK_NOTIFICACIONES]);
  }

  markAsRead(id: string): Observable<Notificacion | null> {
    const target = MOCK_NOTIFICACIONES.find((item) => item.id === id);
    if (!target) {
      return this.mockApiService.respond(null);
    }

    target.read = true;
    return this.mockApiService.respond(target);
  }
}