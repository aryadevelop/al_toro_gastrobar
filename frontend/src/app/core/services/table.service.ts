import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MOCK_MESAS } from '../mocks/restaurant.mock';
import { Mesa } from '../models/domain.models';
import { MockApiService } from './mock-api.service';

@Injectable({ providedIn: 'root' })
export class TableService {
  constructor(private readonly mockApiService: MockApiService) {}

  list(): Observable<Mesa[]> {
    return this.mockApiService.respond([...MOCK_MESAS]);
  }
}