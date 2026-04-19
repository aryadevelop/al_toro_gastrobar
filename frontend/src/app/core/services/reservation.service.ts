import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { combineLatest, map, Observable, of, throwError } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import {
  ApiEnvelope,
  BackendCrearReservaRequest,
  BackendDisponibilidadResponse,
  BackendReservaDetalle,
} from '../models/api.models';
import { Pago, Reserva } from '../models/domain.models';
import { AuthService } from './auth.service';

export interface ReservationAvailabilityOption {
  id: string;
  name: string;
  compatibleZoneIds?: string[];
  allowZoneSelection?: boolean;
}

export interface ReservationAvailability {
  available: boolean;
  decorations: ReservationAvailabilityOption[];
  zones: ReservationAvailabilityOption[];
}

export interface ReservationDetailData {
  reservation: Reserva;
  preOrderTotal: number;
  totalPaid: number;
  payments: Pago[];
}

@Injectable({ providedIn: 'root' })
export class ReservationService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  list(): Observable<Reserva[]> {
    return combineLatest([this.listFuture(), this.listCancelledOrReturned()]).pipe(
      map(([future, cancelled]) => {
        const seen = new Set<string>();
        return [...future, ...cancelled].filter((item) => {
          if (seen.has(item.id)) {
            return false;
          }
          seen.add(item.id);
          return true;
        });
      })
    );
  }

  listByCliente(_: string): Observable<Reserva[]> {
    return this.list();
  }

  listFuture(): Observable<Reserva[]> {
    return this.listByEndpoint(API_PATHS.reservas.futuras);
  }

  listCancelledOrReturned(): Observable<Reserva[]> {
    return this.listByEndpoint(API_PATHS.reservas.canceladasDevueltas);
  }

  getAvailability(date: string, time: string): Observable<ReservationAvailability> {
    const fechaHora = `${date}T${time}:00`;
    const params = new HttpParams().set('fechaHora', fechaHora);

    return this.http
      .get<ApiEnvelope<BackendDisponibilidadResponse>>(API_PATHS.reservas.disponibilidad, { params })
      .pipe(
        map((response) => ({
          available: response.data.disponible,
          decorations: (response.data.decoraciones ?? []).map((item) => ({
            id: String(item.decoracionId),
            name: item.nombre ?? item.decoracionNombre ?? 'Decoración',
            compatibleZoneIds: (item.zonaIdsCompatibles ?? []).map((zoneId) => String(zoneId)),
            allowZoneSelection: item.puedeSeleccionarZona ?? true,
          })),
          zones: (response.data.zonas ?? []).map((item) => ({
            id: String(item.zonaId),
            name: item.nombre ?? item.zonaNombre ?? 'Zona',
          })),
        }))
      );
  }

  create(payload: Omit<Reserva, 'id' | 'status'> & { status?: Reserva['status'] }): Observable<Reserva> {
    const request = this.toCreateRequest(payload);

    return this.http
      .post<ApiEnvelope<BackendReservaDetalle>>(API_PATHS.reservas.crear, request)
      .pipe(map((response) => this.toReserva(response.data)));
  }

  getDetail(id: string): Observable<ReservationDetailData> {
    return this.http
      .get<ApiEnvelope<BackendReservaDetalle>>(API_PATHS.reservas.detalle(id))
      .pipe(
        map((response) => {
          const detail = response.data;
          return {
            reservation: this.toReserva(detail),
            preOrderTotal: detail.preOrdenTotal ?? 0,
            totalPaid: detail.totalAbonado ?? 0,
            payments: (detail.abonos ?? []).map((item) => ({
              id: String(item.abonoId),
              saleId: String(detail.reservaId),
              method: this.toPaymentMethod(item.metodo),
              amount: item.monto,
              paidAt: item.fechaHora,
            })),
          } satisfies ReservationDetailData;
        })
      );
  }

  update(_id: string, _payload: Partial<Reserva>): Observable<Reserva | null> {
    return throwError(() => new Error('La API actual no tiene endpoint para actualizar reservas.'));
  }

  private listByEndpoint(endpoint: string): Observable<Reserva[]> {
    const email = this.authService.currentUser()?.email;
    if (!email) {
      return of([]);
    }

    const params = new HttpParams().set('emailCliente', email);
    return this.http
      .get<ApiEnvelope<BackendReservaDetalle[]>>(endpoint, { params })
      .pipe(map((response) => response.data.map((item) => this.toReserva(item))));
  }

  private toCreateRequest(payload: Omit<Reserva, 'id' | 'status'> & { status?: Reserva['status'] }): BackendCrearReservaRequest {
    const preOrden = (payload.preorderItems ?? [])
      .map((item) => {
        const productId = Number(item.productId);
        if (!Number.isFinite(productId)) {
          return null;
        }

        const rawOptions = item.modificationOptionIds ?? [];
        const options = rawOptions
          .map((optionId) => Number(optionId))
          .filter((optionId) => Number.isFinite(optionId));

        return {
          productoId: productId,
          cantidad: item.quantity,
          descripcion: item.description,
          esMenuEspecial: item.isSpecialMenu,
          opcionesModificacion: options.length > 0 ? options : undefined,
        };
      })
      .filter((item): item is NonNullable<typeof item> => item !== null);

    return {
      fechaHoraLlegada: `${payload.date}T${payload.time}:00`,
      numeroPersonas: payload.guests,
      decoracionId: this.toNumberOrUndefined(payload.decorationId),
      zonaId: this.toNumberOrUndefined(payload.zoneId),
      notas: payload.notes,
      preOrden,
    };
  }

  private toReserva(input: BackendReservaDetalle): Reserva {
    const [datePart, timePartRaw] = input.fechaHoraLlegada.split('T');
    const timePart = (timePartRaw ?? '00:00:00').slice(0, 5);

    return {
      id: String(input.reservaId),
      clienteId: String(input.clienteId ?? ''),
      guestName: input.clienteNombre ?? 'Cliente',
      guests: input.numeroPersonas,
      date: datePart,
      time: timePart,
      status: this.toReservationStatus(input.estado),
      decorationName: input.decoracionNombre,
      zoneName: input.zonaNombre,
      notes: input.notas,
      preorderItems: (input.preOrdenItems ?? []).map((item) => ({
        productId: String(item.productoId),
        productName: item.productoNombre,
        quantity: item.cantidad,
        description: item.descripcion,
        isSpecialMenu: Boolean(item.modificaciones && item.modificaciones.length > 0),
        modificationOptionIds: (item.modificaciones ?? []).map((option) => String(option.opcionId)),
      })),
    };
  }

  private toReservationStatus(rawStatus: string): Reserva['status'] {
    const normalized = rawStatus.toUpperCase();

    if (normalized === 'CONFIRMADA' || normalized === 'CONFIRMED') {
      return 'CONFIRMED';
    }

    if (normalized === 'PENDIENTE' || normalized === 'PENDING') {
      return 'PENDING';
    }

    if (
      normalized === 'ARRIVED' ||
      normalized === 'ASISTIO' ||
      normalized === 'ASISTIÓ' ||
      normalized === 'EN_CURSO'
    ) {
      return 'ARRIVED';
    }

    if (normalized === 'COMPLETADA' || normalized === 'COMPLETED' || normalized === 'FINALIZADA') {
      return 'COMPLETED';
    }

    if (normalized === 'CANCELADA' || normalized === 'CANCELLED' || normalized === 'DEVUELTA') {
      return 'CANCELLED';
    }

    return 'PENDING';
  }

  private toPaymentMethod(method: string): Pago['method'] {
    const normalized = method.toUpperCase();

    if (normalized.includes('EFECTIVO') || normalized === 'CASH') {
      return 'CASH';
    }

    if (normalized.includes('TARJETA') || normalized === 'CARD') {
      return 'CARD';
    }

    return 'TRANSFER';
  }

  private toNumberOrUndefined(value?: string): number | undefined {
    if (!value) {
      return undefined;
    }

    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }
}