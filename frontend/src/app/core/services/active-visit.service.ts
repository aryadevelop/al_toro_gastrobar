import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { ApiEnvelope, BackendEstadoVisita, BackendNotificacionAsistencia } from '../models/api.models';

export interface OrderItem {
  comandaItemId: string;
  productName: string;
  quantity: number;
  status: string;
  unitPrice: number;
  subtotal: number;
}

export interface ActiveVisitState {
  visitaId: string;
  tableCode?: string;
  closed: boolean;
  items: OrderItem[];
  total: number;
  assistanceRequested: boolean;
  assistanceNotificationId?: string;
}

@Injectable({ providedIn: 'root' })
export class ActiveVisitService {
  private readonly http = inject(HttpClient);

  getActiveVisit(): Observable<ActiveVisitState | null> {
    return this.http
      .get<ApiEnvelope<BackendEstadoVisita | null>>(API_PATHS.visitas.activa)
      .pipe(
        map((response) => (response.data ? this.toActiveVisitState(response.data) : null)),
        catchError((err: any) => {
          // Treat 404 (no active visit) as no visit; return null silently.
          if (err?.status === 404) {
            return of(null);
          }
          throw err;
        })
      );
  }

  requestAssistance(visitaId: string): Observable<string> {
    return this.http
      .post<ApiEnvelope<BackendNotificacionAsistencia>>(API_PATHS.visitas.asistencia(visitaId), {})
      .pipe(map((response) => String(response.data.notificacionId)));
  }

  private toActiveVisitState(data: BackendEstadoVisita): ActiveVisitState {
    return {
      visitaId: String(data.visitaId),
      tableCode: data.mesaIdentificador,
      closed: data.visitaCerrada,
      items: (data.items ?? []).map((item) => ({
        comandaItemId: String(item.comandaItemId),
        productName: item.nombreProducto,
        quantity: item.cantidad,
        status: item.estadoItem,
        unitPrice: item.precioUnitario,
        subtotal: item.subtotal,
      })),
      total: data.total ?? 0,
      assistanceRequested: data.asistenciaSolicitada,
      assistanceNotificationId: data.notificacionAsistenciaId
        ? String(data.notificacionAsistenciaId)
        : undefined,
    };
  }
}
