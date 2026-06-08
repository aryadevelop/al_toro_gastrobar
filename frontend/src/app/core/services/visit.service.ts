import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable, of } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { ApiEnvelope, BackendVisitaResumen } from '../models/api.models';

export interface VisitHistoryEntry {
  visitId: string;
  reservationId?: string;
  dateTime: Date;
  guests: number;
  statusLabel: string;
  total: number;
  hasDetail: boolean;
}

@Injectable({ providedIn: 'root' })
export class VisitService {
  private readonly http = inject(HttpClient);

  getHistory(emailCliente?: string): Observable<VisitHistoryEntry[]> {
    if (!emailCliente) {
      return of([]);
    }

    const params = new HttpParams().set('emailCliente', emailCliente);
    return this.http
      .get<ApiEnvelope<BackendVisitaResumen[]>>(API_PATHS.visitas.historial, { params })
      .pipe(
        map((response) =>
          response.data
            .map((item) => ({
              visitId: String(item.visitaId),
              reservationId: item.reservaId ? String(item.reservaId) : undefined,
              dateTime: new Date(item.fechaHoraLlegada),
              guests: item.numeroPersonas,
              statusLabel: this.toVisitStatusLabel(item.estadoVisita),
              total: item.montoTotal ?? 0,
              hasDetail: Boolean(item.reservaId),
            }))
            .sort((a, b) => b.dateTime.getTime() - a.dateTime.getTime())
        )
      );
  }

  private toVisitStatusLabel(rawStatus: string): string {
    const normalized = rawStatus.toUpperCase();

    if (normalized === 'CONFIRMADA' || normalized === 'CONFIRMED') {
      return 'Confirmada';
    }

    if (normalized === 'PENDIENTE' || normalized === 'PENDING') {
      return 'Pendiente';
    }

    if (normalized === 'COMPLETADA' || normalized === 'COMPLETED' || normalized === 'FINALIZADA') {
      return 'Completada';
    }

    if (normalized === 'CANCELADA' || normalized === 'CANCELLED' || normalized === 'DEVUELTA') {
      return 'Cancelada';
    }

    if (normalized === 'ARRIVED' || normalized === 'ASISTIO' || normalized === 'ASISTIÓ') {
      return 'Asistió';
    }

    return rawStatus;
  }
}
