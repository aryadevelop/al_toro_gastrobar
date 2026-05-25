import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { combineLatest, map, Observable, of } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import {
  ApiEnvelope,
  BackendAbonoItem,
  BackendCancelarReservaResponse,
  BackendConfirmarReservaResponse,
  BackendCrearReservaRequest,
  BackendDisponibilidadResponse,
  BackendPreOrdenItem,
  BackendRegistrarAbonoRequest,
  BackendRegistrarAbonoResponse,
  BackendResumenPagoResponse,
  BackendMarcarInasistenciaResponse,
  BackendModificarReservaResponse,
  BackendListadoReservasResponse,
  BackendReservaDetalle,
} from '../models/api.models';
import { Pago, Reserva } from '../models/domain.models';
import { AuthService } from './auth.service';

export interface ReservationAvailabilityOption {
  id: string;
  name: string;
  imageUrl?: string;
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
  clienteId?: string;
  rawEstado: string;
  rawTipo?: string;
  preOrderTotal: number;
  totalReserva: number;
  valorDecoracion: number;
  totalPaid: number;
  payments: ReservationDetailPayment[];
  preOrderItemsDetail: ReservationDetailPreOrderItem[];
  canConfirm: boolean;
  canAddAnticipo: boolean;
  canAddDevolucion: boolean;
  canCancel: boolean;
}

export interface ReservationDetailPreOrderItem {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  description?: string;
  isSpecialMenu?: boolean;
  modificationLabels: string[];
}

export interface ReservationDetailPayment extends Pago {
  tipo?: string;
  rawMetodo: string;
}

export interface ReservationUpdateResult {
  reservation: Reserva;
  requiresWhatsApp: boolean;
  whatsappMessage?: string;
}

export interface ReservationListCajeroItem extends Reserva {
  rawEstado: string;
  rawTipo?: string;
  mostrarConfirmar: boolean;
  mostrarAgregarAnticipo: boolean;
  mostrarAgregarDevolucion: boolean;
  mostrarCancelar: boolean;
}

export interface ReservationPaymentSummary {
  reservaId: string;
  clienteNombre: string;
  fechaHoraLlegada: string;
  numeroPersonas: number;
  estado: string;
  tipo: string;
  totalReserva: number;
  totalAnticipado: number;
  totalDevuelto: number;
  netoAbonado: number;
  pendientePorAbonar?: number | null;
  pendientePorDevolver?: number | null;
}

export interface RegisterAbonoPayload {
  tipo: 'ANTICIPO' | 'DEVOLUCION';
  monto: number;
  metodo: 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA' | 'OTRO';
  fechaHora: string;
}

export interface RegisterAbonoResult {
  abonoId: string;
  tipo: string;
  estado: string;
  resumen?: ReservationPaymentSummary;
  message?: string;
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

  /**
   * Lista reservas para el rol Mesero usando el endpoint de consulta.
   * Devuelve tanto las reservas (mapeadas a `Reserva`) como el resumen por zona.
   */
  listForMesero(fecha?: string, identificador?: string): Observable<{
    reservas: Reserva[];
    resumenZonas: Array<{ zonaId?: string; zonaNombre: string; cantidadReservas: number }>;
  }> {
    let params = new HttpParams();
    if (fecha) {
      params = params.set('fecha', fecha);
    }
    if (identificador) {
      params = params.set('identificador', identificador);
    }

    return this.http
      .get<ApiEnvelope<BackendListadoReservasResponse>>(API_PATHS.reservas.meseroConsulta, { params })
      .pipe(
        map((response) => {
          const data = response.data;

          const reservas = (data.reservas ?? []).map((item) => ({
            id: String(item.reservaId),
            clienteId: '',
            guestName: item.clienteNombre ?? 'Cliente',
            phone: item.clienteTelefono,
            guests: item.numeroPersonas ?? 0,
            date: fecha ?? '',
            time: (item.horaLlegada ?? '00:00').slice(0, 5),
            status: this.toReservationStatus(item.estado ?? ''),
            type: undefined,
            decorationId: undefined,
            decorationName: item.decoracionNombre,
            zoneId: item.zonaId ? String(item.zonaId) : undefined,
            zoneName: item.zonaNombre,
            notes: undefined,
            preorderItems: [],
            mostrarBotonInasistencia: item.mostrarBotonInasistencia ?? false,
          } as Reserva & { mostrarBotonInasistencia: boolean }));

          const resumenZonas = (data.resumenZonas ?? []).map((z) => ({
            zonaId: z.zonaId ? String(z.zonaId) : undefined,
            zonaNombre: z.zonaNombre,
            cantidadReservas: z.cantidadReservas,
          }));

          return { reservas, resumenZonas };
        })
      );
  }

  confirmar(reservaId: string): Observable<ReservationUpdateResult> {
    return this.http
      .patch<ApiEnvelope<BackendConfirmarReservaResponse>>(API_PATHS.reservas.confirmar(reservaId), {})
      .pipe(
        map((response) => {
          const data = response.data;
          const reservation = this.toReserva({
            reservaId: data.reservaId,
            fechaHoraLlegada: data.fechaHoraLlegada,
            numeroPersonas: data.numeroPersonas,
            estado: data.estado,
            tipo: data.tipo,
            clienteNombre: data.clienteNombre,
            zonaNombre: data.zonaNombre,
          });

          return {
            reservation,
            requiresWhatsApp: false,
          } satisfies ReservationUpdateResult;
        })
      );
  }

  listForCajero(fecha?: string, identificador?: string): Observable<{
    reservas: ReservationListCajeroItem[];
    resumenZonas: Array<{ zonaId?: string; zonaNombre: string; cantidadReservas: number }>;
  }> {
    let params = new HttpParams();
    if (fecha) {
      params = params.set('fecha', fecha);
    }
    if (identificador) {
      params = params.set('identificador', identificador);
    }

    return this.http
      .get<ApiEnvelope<BackendListadoReservasResponse>>(API_PATHS.reservas.meseroConsulta, { params })
      .pipe(
        map((response) => {
          const data = response.data;

          const reservas = (data.reservas ?? []).map((item) => {
            const estado = item.estado ?? '';
            const tipo = item.tipo ?? undefined;

            return {
              id: String(item.reservaId),
              clienteId: '',
              guestName: item.clienteNombre ?? 'Cliente',
              phone: item.clienteTelefono,
              guests: item.numeroPersonas ?? 0,
              date: fecha ?? '',
              time: (item.horaLlegada ?? '00:00').slice(0, 5),
              status: this.toReservationStatus(estado),
              type: this.toReservationType(tipo),
              decorationId: undefined,
              decorationName: item.decoracionNombre,
              zoneId: item.zonaId ? String(item.zonaId) : undefined,
              zoneName: item.zonaNombre,
              notes: undefined,
              preorderItems: [],
              rawEstado: estado,
              rawTipo: tipo,
              mostrarConfirmar: Boolean(item.mostrarConfirmar),
              mostrarAgregarAnticipo: Boolean(item.mostrarAgregarAnticipo),
              mostrarAgregarDevolucion: Boolean(item.mostrarAgregarDevolucion),
              mostrarCancelar: Boolean(item.mostrarCancelar),
            } satisfies ReservationListCajeroItem;
          });

          const resumenZonas = (data.resumenZonas ?? []).map((z) => ({
            zonaId: z.zonaId ? String(z.zonaId) : undefined,
            zonaNombre: z.zonaNombre,
            cantidadReservas: z.cantidadReservas,
          }));

          return { reservas, resumenZonas };
        })
      );
  }

  getResumenPago(reservaId: string): Observable<ReservationPaymentSummary> {
    return this.http
      .get<ApiEnvelope<BackendResumenPagoResponse>>(API_PATHS.reservas.resumenPago(reservaId))
      .pipe(map((response) => this.toResumenPago(response.data)));
  }

  registrarAbono(reservaId: string, payload: RegisterAbonoPayload): Observable<RegisterAbonoResult> {
    const request: BackendRegistrarAbonoRequest = {
      tipo: payload.tipo,
      monto: payload.monto,
      metodo: payload.metodo,
      fechaHora: payload.fechaHora,
    };

    return this.http
      .post<ApiEnvelope<BackendRegistrarAbonoResponse>>(API_PATHS.reservas.abonos(reservaId), request)
      .pipe(
        map((response) => ({
          abonoId: String(response.data.abonoId),
          tipo: response.data.tipo,
          estado: response.data.estado,
          resumen: response.data.resumen ? this.toResumenPago(response.data.resumen) : undefined,
          message: response.message,
        }))
      );
  }

  marcarInasistencia(reservaId: string): Observable<{ reservaId: string; estado: string; zonaLiberada?: string; decoracionLiberada?: string }> {
    return this.http
      .patch<ApiEnvelope<BackendMarcarInasistenciaResponse>>(API_PATHS.reservas.marcarInasistencia(reservaId), {})
      .pipe(
        map((response) => ({
          reservaId: String(response.data.reservaId),
          estado: response.data.estado,
          zonaLiberada: response.data.zonaLiberada,
          decoracionLiberada: response.data.decoracionLiberada,
        }))
      );
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
            imageUrl: item.imagenUrl,
            compatibleZoneIds: (item.zonaIdsCompatibles ?? []).map((zoneId) => String(zoneId)),
            allowZoneSelection: item.puedeSeleccionarZona ?? true,
          })),
          zones: (response.data.zonas ?? []).map((item) => ({
            id: String(item.zonaId),
            name: item.nombre ?? item.zonaNombre ?? 'Zona',
            imageUrl: item.imagenUrl,
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
          const rawEstado = detail.estado ?? '';
          const rawTipo = detail.tipo;
          const abonos = detail.abonos ?? [];

          return {
            reservation: this.toReserva(detail),
            clienteId: detail.clienteId ? String(detail.clienteId) : undefined,
            rawEstado,
            rawTipo,
            preOrderTotal: detail.preOrdenTotal ?? 0,
            totalReserva: detail.total ?? 0,
            valorDecoracion: detail.valorDecoracion ?? 0,
            totalPaid: detail.totalAbonado ?? 0,
            payments: abonos.map((item) => this.toDetailPayment(item, detail.reservaId)),
            preOrderItemsDetail: (detail.preOrdenItems ?? []).map((item) => this.toDetailPreOrderItem(item)),
            canConfirm: this.canConfirmFromState(rawEstado, rawTipo),
            canAddAnticipo: this.canAddAnticipoFromState(rawEstado),
            canAddDevolucion: this.canAddDevolucionFromState(rawEstado, abonos.length),
            canCancel: this.canCancelFromState(rawEstado),
          } satisfies ReservationDetailData;
        })
      );
  }

  update(id: string, payload: Partial<Reserva>): Observable<ReservationUpdateResult> {
    const request = this.toCreateRequest(payload as Omit<Reserva, 'id' | 'status'> & { status?: Reserva['status'] });

    return this.http
      .put<ApiEnvelope<BackendModificarReservaResponse>>(API_PATHS.reservas.modificar(id), request)
      .pipe(
        map((response) => {
          const data = response.data;
          const reservation = this.toReserva({
            reservaId: data.reservaId,
            fechaHoraLlegada: data.fechaHoraLlegada,
            numeroPersonas: data.numeroPersonas,
            estado: data.estado,
            tipo: data.tipo,
            zonaNombre: data.zonaNombre,
            decoracionNombre: data.decoracionNombre,
            notas: data.notas,
          });

          return {
            reservation,
            requiresWhatsApp: Boolean(data.requiereWhatsApp),
            whatsappMessage: data.mensajeWhatsApp,
          } satisfies ReservationUpdateResult;
        })
      );
  }

  cancel(id: string): Observable<ReservationUpdateResult> {
    return this.http
      .patch<ApiEnvelope<BackendCancelarReservaResponse>>(API_PATHS.reservas.cancelar(id), {})
      .pipe(
        map((response) => {
          const data = response.data;
          const reservation = this.toReserva({
            reservaId: data.reservaId,
            fechaHoraLlegada: data.fechaHoraLlegada,
            numeroPersonas: data.numeroPersonas,
            estado: data.estado,
            tipo: data.tipo,
          });

          return {
            reservation,
            requiresWhatsApp: Boolean(data.requiereWhatsApp),
            whatsappMessage: data.mensajeWhatsApp,
          } satisfies ReservationUpdateResult;
        })
      );
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
      phone: input.clienteTelefono,
      guests: input.numeroPersonas,
      date: datePart,
      time: timePart,
      status: this.toReservationStatus(input.estado),
      type: this.toReservationType(input.tipo),
      decorationId: input.decoracionId ? String(input.decoracionId) : undefined,
      decorationName: input.decoracionNombre,
      zoneId: input.zonaId ? String(input.zonaId) : undefined,
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

  private toReservationType(rawType?: string): Reserva['type'] {
    const normalized = (rawType ?? '').toUpperCase();
    if (normalized === 'ESPECIAL' || normalized === 'SPECIAL') {
      return 'SPECIAL';
    }

    if (normalized === 'BASICA' || normalized === 'BASIC') {
      return 'BASIC';
    }

    return undefined;
  }

  private toDetailPayment(item: BackendAbonoItem, reservaId: number): ReservationDetailPayment {
    return {
      id: String(item.abonoId),
      saleId: String(reservaId),
      method: this.toPaymentMethod(item.metodo),
      amount: item.monto,
      paidAt: item.fechaHora,
      tipo: item.tipo,
      rawMetodo: item.metodo,
    };
  }

  private toDetailPreOrderItem(item: BackendPreOrdenItem): ReservationDetailPreOrderItem {
    const unitPrice = item.precioUnitario ?? 0;
    return {
      productId: String(item.productoId),
      productName: item.productoNombre,
      quantity: item.cantidad,
      unitPrice,
      subtotal: unitPrice * item.cantidad,
      description: item.descripcion,
      isSpecialMenu: Boolean(item.modificaciones && item.modificaciones.length > 0),
      modificationLabels: (item.modificaciones ?? []).map((mod) => mod.opcionNombre),
    };
  }

  private canConfirmFromState(rawEstado: string, rawTipo?: string): boolean {
    return (rawEstado ?? '').toUpperCase() === 'PENDIENTE' && (rawTipo ?? '').toUpperCase() === 'ESPECIAL';
  }

  private canAddAnticipoFromState(rawEstado: string): boolean {
    return (rawEstado ?? '').toUpperCase() === 'CONFIRMADA';
  }

  private canAddDevolucionFromState(rawEstado: string, abonosCount: number): boolean {
    return (rawEstado ?? '').toUpperCase() === 'CANCELADA' && abonosCount > 0;
  }

  private canCancelFromState(rawEstado: string): boolean {
    const normalized = (rawEstado ?? '').toUpperCase();
    return normalized === 'PENDIENTE' || normalized === 'CONFIRMADA';
  }

  private toResumenPago(input: BackendResumenPagoResponse): ReservationPaymentSummary {
    return {
      reservaId: String(input.reservaId),
      clienteNombre: input.clienteNombre,
      fechaHoraLlegada: input.fechaHoraLlegada,
      numeroPersonas: input.numeroPersonas,
      estado: input.estado,
      tipo: input.tipo,
      totalReserva: input.totalReserva ?? 0,
      totalAnticipado: input.totalAnticipado ?? 0,
      totalDevuelto: input.totalDevuelto ?? 0,
      netoAbonado: input.netoAbonado ?? 0,
      pendientePorAbonar: input.pendientePorAbonar ?? null,
      pendientePorDevolver: input.pendientePorDevolver ?? null,
    };
  }

  private toNumberOrUndefined(value?: string): number | undefined {
    if (!value) {
      return undefined;
    }

    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }
}