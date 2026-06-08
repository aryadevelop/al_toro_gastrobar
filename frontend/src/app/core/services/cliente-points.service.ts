import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable, of } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { ApiEnvelope, BackendClientePuntos } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ClientePointsService {
  private readonly http = inject(HttpClient);

  getMyPoints(emailCliente?: string): Observable<number> {
    if (!emailCliente) {
      return of(0);
    }

    const params = new HttpParams().set('emailCliente', emailCliente);
    return this.http
      .get<ApiEnvelope<BackendClientePuntos>>(API_PATHS.clientes.misPuntos, { params })
      .pipe(map((response) => response.data.puntosActuales ?? 0));
  }
}
