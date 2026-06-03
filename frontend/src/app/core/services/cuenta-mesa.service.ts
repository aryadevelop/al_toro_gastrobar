import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import {
  ApiEnvelope,
  BackendAjustarItemsRequest,
  BackendCerrarCuentaRequest,
  BackendClienteBuscarResponse,
  BackendClientePuntosResponse,
  BackendCuentaPreliminarResponse,
} from '../models/api.models';

/* ── Modelos de dominio del cajero para la cuenta de mesa ── */

export interface CuentaItem {
  comandaItemId: number;
  nombreProducto: string;
  categoriaProducto: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  descripcion?: string;
  esModificado: boolean;
  menuGrupo?: string;
  esMenuEspecial: boolean;
}

export interface AbonoItem {
  abonoId: number;
  monto: number;
  fechaHora: string;
  metodo: string;
  tipo: string;
}

export interface CuentaPreliminar {
  visitaId: number;
  clienteId?: number;
  clienteNombre?: string;
  clienteEmail?: string;
  puntosCanjeables?: number;
  puntosAcumulados?: number;
  fechaHoraLlegada?: string;
  meseroNombre?: string;
  mesaIdentificador?: string;
  items: CuentaItem[];
  decoracionNombre?: string;
  valorDecoracion?: number;
  totalPreorden: number;
  totalAPagar: number;
  anticipos?: AbonoItem[];
  montoAbonado?: number;
  saldoPendiente?: number;
}

export interface ClienteBusqueda {
  clienteId: number;
  nombre: string;
  email: string;
  telefono: string;
}

export type MetodoPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA';

export interface AjusteItem {
  comandaItemId: number;
  cantidad: number;
  precio?: number;
}

@Injectable({ providedIn: 'root' })
export class CuentaMesaService {
  private readonly http = inject(HttpClient);

  /**
   * Obtiene la cuenta preliminar de una visita.
   * Endpoint: GET /api/ventas/{visitaId}/cuenta
   *
   * 🔌 Cambiar si el backend modifica la ruta: actualizar API_PATHS.ventas.cuenta
   */
  getCuenta(visitaId: number): Observable<CuentaPreliminar> {
    return this.http
      .get<ApiEnvelope<BackendCuentaPreliminarResponse>>(API_PATHS.ventas.cuenta(visitaId))
      .pipe(map((r) => this.mapCuenta(r.data)));
  }

  /**
   * Busca clientes por coincidencia parcial de correo.
   * Endpoint: GET /api/clientes/buscar?correo={q}
   *
   * 🔌 Cambiar si el backend modifica el parámetro: actualizar el HttpParams abajo
   */
  buscarClientes(correo: string): Observable<ClienteBusqueda[]> {
    return this.http
      .get<ApiEnvelope<BackendClienteBuscarResponse[]>>(API_PATHS.clientesPuntos.buscar, {
        params: { correo },
      })
      .pipe(
        map((r) =>
          (r.data ?? []).map((c) => ({
            clienteId: c.clienteId,
            nombre: c.nombre,
            email: c.email,
            telefono: c.telefono,
          }))
        )
      );
  }

  /**
   * Asigna un cliente a la visita, o la marca como invitado (clienteId = undefined).
   * Endpoint: PATCH /api/visitas/{visitaId}/cliente?clienteId={id}
   *
   * 🔌 Cambiar si el backend modifica la ruta: actualizar API_PATHS.visitasAcciones.asignarCliente
   */
  asignarCliente(visitaId: number, clienteId?: number): Observable<void> {
    const params: Record<string, string> = {};
    if (clienteId !== undefined) {
      params['clienteId'] = String(clienteId);
    }
    return this.http
      .patch<ApiEnvelope<null>>(API_PATHS.visitasAcciones.asignarCliente(visitaId), null, { params })
      .pipe(map(() => undefined));
  }

  /**
   * Canjea los puntos del cliente (los resetea a 0).
   * Endpoint: POST /api/clientes/{clienteId}/canje-puntos?emailEmpleado={email}
   *
   * 🔌 Cambiar si el backend modifica la ruta o el parámetro
   */
  canjearPuntos(clienteId: number, emailEmpleado: string): Observable<BackendClientePuntosResponse> {
    return this.http
      .post<ApiEnvelope<BackendClientePuntosResponse>>(
        API_PATHS.clientesPuntos.canjearPuntos(clienteId),
        null,
        { params: { emailEmpleado } }
      )
      .pipe(map((r) => r.data));
  }

  /**
   * Ajusta en bloque cantidades, precios y eliminaciones de ítems.
   * Devuelve la cuenta recalculada.
   * Endpoint: PATCH /api/visitas/{visitaId}/items
   *
   * 🔌 Cambiar si el backend modifica la ruta: actualizar API_PATHS.visitasAcciones.ajustarItems
   */
  ajustarItems(
    visitaId: number,
    items: AjusteItem[],
    eliminados: number[]
  ): Observable<CuentaPreliminar> {
    const body: BackendAjustarItemsRequest = {
      items: items.map((i) => ({
        comandaItemId: i.comandaItemId,
        cantidad: i.cantidad,
        precio: i.precio ?? null,
      })),
      eliminados: eliminados.length > 0 ? eliminados : null,
    };
    return this.http
      .patch<ApiEnvelope<BackendCuentaPreliminarResponse>>(
        API_PATHS.visitasAcciones.ajustarItems(visitaId),
        body
      )
      .pipe(map((r) => this.mapCuenta(r.data)));
  }

  /**
   * Cierra la cuenta de la visita y registra la venta.
   * Endpoint: POST /api/ventas
   *
   * 🔌 Cambiar si el backend modifica la ruta: actualizar API_PATHS.ventas.cerrar
   */
  cerrarCuenta(
    visitaId: number,
    emailCajero: string,
    metodo: MetodoPago,
    descuento?: number
  ): Observable<void> {
    const body: BackendCerrarCuentaRequest = {
      emailCajero,
      visitaId,
      metodo,
      descuento: descuento ?? 0,
    };
    return this.http
      .post<ApiEnvelope<null>>(API_PATHS.ventas.cerrar, body)
      .pipe(map(() => undefined));
  }

  /* ── Mapper privado ── */
  private mapCuenta(data: BackendCuentaPreliminarResponse): CuentaPreliminar {
    return {
      visitaId: data.visitaId,
      clienteId: data.clienteId ?? undefined,
      clienteNombre: data.clienteNombre ?? undefined,
      clienteEmail: data.clienteEmail ?? undefined,
      puntosCanjeables: data.puntosCanjeables ?? undefined,
      puntosAcumulados: data.puntosAcumulados ?? undefined,
      fechaHoraLlegada: data.fechaHoraLlegada ?? undefined,
      meseroNombre: data.meseroNombre ?? undefined,
      mesaIdentificador: data.mesaIdentificador ?? undefined,
      items: (data.items ?? []).map((item) => ({
        comandaItemId: item.comandaItemId,
        nombreProducto: item.nombreProducto,
        categoriaProducto: item.categoriaProducto,
        cantidad: item.cantidad,
        precioUnitario: item.precioUnitario,
        subtotal: item.subtotal,
        descripcion: item.descripcion ?? undefined,
        esModificado: item.esModificado,
        menuGrupo: item.menuGrupo ?? undefined,
        esMenuEspecial: item.esMenuEspecial,
      })),
      decoracionNombre: data.decoracionNombre ?? undefined,
      valorDecoracion: data.valorDecoracion ?? undefined,
      totalPreorden: data.totalPreorden ?? 0,
      totalAPagar: data.totalAPagar ?? 0,
      anticipos: (data.anticipos ?? []).map((a) => ({
        abonoId: a.abonoId,
        monto: a.monto,
        fechaHora: a.fechaHora,
        metodo: a.metodo,
        tipo: a.tipo,
      })),
      montoAbonado: data.montoAbonado ?? undefined,
      saldoPendiente: data.saldoPendiente ?? undefined,
    };
  }
}
