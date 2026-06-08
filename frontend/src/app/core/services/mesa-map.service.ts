import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import {
  ApiEnvelope,
  BackendAsignarMesaRequest,
  BackendMesaAsignadaResponse,
  BackendMapaMesasResponse,
  BackendMesaDetalleResponse,
  BackendMesaItemsProduccionResponse,
  BackendZonaDisponibleMesaResponse,
} from '../models/api.models';

export interface MesaNotificacionActiva {
  id: string;
  tipo: string;
  fechaHora: string;
}

export interface MesaMapaItem {
  id: string;
  visitaId?: string;
  identificador: string;
  numeroPersonas: number;
  estado: string;
  nombreMesero?: string;
  esMesaPropia: boolean;
  tieneBorrador: boolean;
  notificaciones: MesaNotificacionActiva[];
}

export interface MesaZonaMapa {
  id: string;
  name: string;
  count: number;
  mesas: MesaMapaItem[];
}

export interface MesaMapaData {
  zonas: MesaZonaMapa[];
  totalMesas: number;
}

export interface MesaItemComanda {
  nombreProducto: string;
  descripcion?: string;
  categoriaProducto?: string;
  cantidad: number;
  estadoComanda?: string;
}

export interface MesaDetalle {
  mesaId: string;
  visitaId?: string;
  identificador: string;
  nombreCliente?: string;
  clienteId?: string;
  puntosFidelizacion?: number;
  esCumpleanos?: boolean;
  puedeGenerarCuenta?: boolean;
  horaLlegada?: string;
  numeroPersonas?: number;
  estado?: string;
  notasReserva?: string;
  notasMesa?: string;
  notasComandas?: string;
  itemsComanda: MesaItemComanda[];
}

export interface MesaItemsProduccion {
  identificadorMesa: string;
  items: MesaItemComanda[];
}

export interface MesaZonaDisponible {
  id: string;
  name: string;
  capacidadTotal: number;
  personasOcupadas: number;
  disponibilidad: number;
}

export interface MesaAsignacionPayload {
  mesaIdentificador: string;
  zonaId: string;
  numeroPersonas: number;
  reservaId?: string;
  mesaNotas?: string;
}

export interface MesaAsignacionResult {
  visitaId: string;
  mesaIdentificador: string;
  zonaId: string;
  zonaNombre: string;
  numeroPersonas: number;
  estadoMesa: string;
  emailMesero: string;
  reservaId?: string;
}

@Injectable({ providedIn: 'root' })
export class MesaMapService {
  private readonly http = inject(HttpClient);

  getMapa(): Observable<MesaMapaData> {
    return this.http.get<ApiEnvelope<BackendMapaMesasResponse>>(API_PATHS.mesas.mapa).pipe(
      map((response) => {
        const zonas = (response.data.zonas ?? []).map((zona) => ({
          id: String(zona.zonaId),
          name: zona.zonaNombre,
          count: zona.cantidadMesasActivas ?? zona.mesas?.length ?? 0,
          mesas: (zona.mesas ?? []).map((mesa) => ({
            id: String(mesa.mesaId),
            visitaId: mesa.visitaId ? String(mesa.visitaId) : undefined,
            identificador: mesa.identificador,
            numeroPersonas: mesa.numeroPersonas,
            estado: mesa.estado,
            nombreMesero: mesa.nombreMesero,
            esMesaPropia: mesa.esMesaPropia ?? false,
            tieneBorrador: mesa.tieneBorrador ?? false,
            notificaciones: (mesa.notificacionesActivas ?? []).map((item) => ({
              id: String(item.notificacionId),
              tipo: item.tipo,
              fechaHora: item.fechaHora,
            })),
          })),
        }));

        const totalMesas = zonas.reduce((acc, zona) => acc + zona.mesas.length, 0);

        return { zonas, totalMesas };
      })
    );
  }

  getDetalle(mesaId: string): Observable<MesaDetalle> {
    return this.http
      .get<ApiEnvelope<BackendMesaDetalleResponse>>(API_PATHS.mesas.detalle(mesaId))
      .pipe(
        map((response) => {
          const data = response.data;
          return {
            mesaId: String(data.mesaId),
            visitaId: data.visitaId ? String(data.visitaId) : undefined,
            identificador: data.identificador,
            nombreCliente: data.nombreCliente,
            clienteId: typeof data.clienteId === 'number' ? String(data.clienteId) : undefined,
            puntosFidelizacion:
              typeof data.puntosFidelizacion === 'number' ? data.puntosFidelizacion : undefined,
            esCumpleanos: typeof data.esCumpleanos === 'boolean' ? data.esCumpleanos : undefined,
            puedeGenerarCuenta:
              typeof data.puedeGenerarCuenta === 'boolean' ? data.puedeGenerarCuenta : undefined,
            horaLlegada: data.horaLlegada,
            numeroPersonas: data.numeroPersonas,
            estado: data.estado,
            notasReserva: data.notasReserva,
            notasMesa: data.notasMesa,
            notasComandas: data.notasComandas,
            itemsComanda: (data.itemsComanda ?? []).map((item) => ({
              nombreProducto: item.nombreProducto,
              descripcion: item.descripcion,
              categoriaProducto: item.categoriaProducto,
              cantidad: item.cantidad,
              estadoComanda: item.estadoComanda,
            })),
          } satisfies MesaDetalle;
        })
      );
  }

  getItemsProduccion(mesaId: string): Observable<MesaItemsProduccion> {
    return this.http
      .get<ApiEnvelope<BackendMesaItemsProduccionResponse>>(API_PATHS.mesas.itemsProduccion(mesaId))
      .pipe(
        map((response) => ({
          identificadorMesa: response.data.identificadorMesa,
          items: (response.data.itemsEnProduccion ?? []).map((item) => ({
            nombreProducto: item.nombreProducto,
            descripcion: item.descripcion,
            categoriaProducto: item.categoriaProducto,
            cantidad: item.cantidad,
            estadoComanda: item.estadoComanda,
          })),
        }))
      );
  }

  getZonasDisponibles(): Observable<MesaZonaDisponible[]> {
    return this.http
      .get<ApiEnvelope<BackendZonaDisponibleMesaResponse[]>>(API_PATHS.mesas.zonasDisponibles)
      .pipe(
        map((response) =>
          (response.data ?? []).map((zona) => ({
            id: String(zona.zonaId),
            name: zona.zonaNombre,
            capacidadTotal: zona.capacidadTotal,
            personasOcupadas: zona.personasOcupadas,
            disponibilidad: zona.disponibilidad,
          }))
        )
      );
  }

  asignarMesa(payload: MesaAsignacionPayload): Observable<MesaAsignacionResult> {
    const request: BackendAsignarMesaRequest = {
      mesaIdentificador: payload.mesaIdentificador,
      zonaId: Number(payload.zonaId),
      numeroPersonas: payload.numeroPersonas,
      reservaId: payload.reservaId ? Number(payload.reservaId) : undefined,
      mesaNotas: payload.mesaNotas || undefined,
    };

    return this.http
      .post<ApiEnvelope<BackendMesaAsignadaResponse>>(API_PATHS.mesas.asignar, request)
      .pipe(
        map((response) => ({
          visitaId: String(response.data.visitaId),
          mesaIdentificador: response.data.mesaIdentificador,
          zonaId: String(response.data.zonaId),
          zonaNombre: response.data.zonaNombre,
          numeroPersonas: response.data.numeroPersonas,
          estadoMesa: response.data.estadoMesa,
          emailMesero: response.data.emailMesero,
          reservaId: response.data.reservaId ? String(response.data.reservaId) : undefined,
        }))
      );
  }
}
