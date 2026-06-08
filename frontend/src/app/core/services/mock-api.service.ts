import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class MockApiService {
  respond<T>(payload: T, latency = 350): Observable<T> {
    return of(payload).pipe(delay(latency));
  }
}