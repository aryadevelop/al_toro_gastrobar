import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import {
  ApiEnvelope,
  BackendClienteBusquedaResponse,
  BackendClienteListadoResponse,
  BackendClienteResumenResponse,
  BackendClienteVentasResponse,
  BackendClienteVentasResumenResponse,
  BackendClienteVentaDetalle,
  BackendVentaAgrupadaAnioResponse,
  BackendVentaAgrupadaMesResponse,
} from '../models/api.models';

export type ClienteSearchMode = 'nombre' | 'correo' | 'telefono';

export interface ClienteBusqueda {
  clienteId: string;
  nombre: string;
  email: string;
  telefono: string;
}

export interface ClienteResumen {
  clienteId: string;
  nombre: string;
  email: string;
  telefono: string;
  fechaNacimiento?: Date | null;
  clienteDesde?: Date | null;
}

export interface ClienteVentasResumen {
  totalVisitas: number;
  totalGastado: number;
  promedioPorVisita: number;
  ultimaVisita?: Date | null;
  clienteDesde?: Date | null;
}

export interface VentaDetalle {
  visitaId: string;
  subtotal: number;
  descuento: number;
  total: number;
  metodo?: string | null;
  fechaHora: Date;
  createdAt?: Date | null;
  cajeroNombre?: string | null;
}

export interface ClienteVentasHistory {
  cliente: ClienteResumen;
  resumen: ClienteVentasResumen;
  ventas: VentaDetalle[];
  mensajeCumpleanos?: string | null;
  mensajeInactivo?: string | null;
  mostrarRecordatorio: boolean;
}

export interface VentaAgrupadaAnio {
  anio: number;
  total: number;
  cantidad: number;
}

export interface VentaAgrupadaMes {
  anio: number;
  mes: number;
  total: number;
  cantidad: number;
}

export interface ClienteSearchResult {
  results: ClienteBusqueda[];
  message?: string;
}

export interface ClienteHistorialResult {
  data: ClienteVentasHistory;
  message?: string;
}

export type EstadoClienteFiltro = 'ACTIVO' | 'INACTIVO';

export interface ClienteListadoFiltros {
  minVisitas?: number | null;
  maxVisitas?: number | null;
  desdeRegistro?: string | null;
  hastaRegistro?: string | null;
  estado?: EstadoClienteFiltro | '';
  nombre?: string;
  correo?: string;
  cumpleanosHoy?: boolean;
  reservasUltimosMeses?: number | null;
}

export interface ClienteListado {
  clienteId: string;
  nombre: string;
  correoElectronico: string;
  telefono: string;
  totalVisitas: number;
  totalGastado: number;
  puntosAcumulados: number;
  estado: string;
  clienteFrecuente: boolean;
}

export interface ClienteListadoResult {
  results: ClienteListado[];
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class ClienteVentasAdminService {
  private readonly http = inject(HttpClient);

  listarClientes(filtros: ClienteListadoFiltros = {}): Observable<ClienteListadoResult> {
    let params = new HttpParams();

    if (typeof filtros.minVisitas === 'number' && filtros.minVisitas >= 0) {
      params = params.set('minVisitas', String(filtros.minVisitas));
    }

    if (typeof filtros.maxVisitas === 'number' && filtros.maxVisitas >= 0) {
      params = params.set('maxVisitas', String(filtros.maxVisitas));
    }

    if (filtros.desdeRegistro) {
      params = params.set('desdeRegistro', filtros.desdeRegistro);
    }

    if (filtros.hastaRegistro) {
      params = params.set('hastaRegistro', filtros.hastaRegistro);
    }

    if (filtros.estado) {
      params = params.set('estado', filtros.estado);
    }

    if (filtros.nombre && filtros.nombre.trim().length > 0) {
      params = params.set('nombre', filtros.nombre.trim());
    }

    if (filtros.correo && filtros.correo.trim().length > 0) {
      params = params.set('correo', filtros.correo.trim());
    }

    if (filtros.cumpleanosHoy) {
      params = params.set('cumpleanosHoy', 'true');
    }

    if (typeof filtros.reservasUltimosMeses === 'number' && filtros.reservasUltimosMeses > 0) {
      params = params.set('reservasUltimosMeses', String(filtros.reservasUltimosMeses));
    }

    return this.http
      .get<ApiEnvelope<BackendClienteListadoResponse[]>>(API_PATHS.clientesAdmin.listar, { params })
      .pipe(
        map((response) => ({
          results: (response.data ?? []).map((item) => this.toClienteListado(item)),
          message: response.message,
        }))
      );
  }

  buscarClientes(mode: ClienteSearchMode, value: string): Observable<ClienteSearchResult> {
    const endpoint = this.resolveSearchEndpoint(mode);
    const params = new HttpParams().set('valor', value.trim());

    return this.http
      .get<ApiEnvelope<BackendClienteBusquedaResponse[]>>(endpoint, { params })
      .pipe(
        map((response) => ({
          results: (response.data ?? []).map((item) => this.toClienteBusqueda(item)),
          message: response.message,
        }))
      );
  }

  private toClienteListado(item: BackendClienteListadoResponse): ClienteListado {
    return {
      clienteId: String(item.clienteId),
      nombre: item.nombre,
      correoElectronico: item.correoElectronico,
      telefono: item.telefono,
      totalVisitas: item.totalVisitas ?? 0,
      totalGastado: item.totalGastado ?? 0,
      puntosAcumulados: item.puntosAcumulados ?? 0,
      estado: item.estado,
      clienteFrecuente: Boolean(item.clienteFrecuente),
    };
  }

  obtenerHistorial(clienteId: string): Observable<ClienteHistorialResult> {
    return this.http
      .get<ApiEnvelope<BackendClienteVentasResponse>>(API_PATHS.clientesAdmin.ventas(clienteId))
      .pipe(
        map((response) => ({
          data: this.toHistorial(response.data),
          message: response.message,
        }))
      );
  }

  obtenerAgrupadoPorAnio(clienteId: string): Observable<VentaAgrupadaAnio[]> {
    return this.http
      .get<ApiEnvelope<BackendVentaAgrupadaAnioResponse[]>>(API_PATHS.clientesAdmin.ventasAgrupadasAnio(clienteId))
      .pipe(map((response) => (response.data ?? []).map((item) => this.toAgrupadoAnio(item))));
  }

  obtenerAgrupadoPorMes(clienteId: string): Observable<VentaAgrupadaMes[]> {
    return this.http
      .get<ApiEnvelope<BackendVentaAgrupadaMesResponse[]>>(API_PATHS.clientesAdmin.ventasAgrupadasMes(clienteId))
      .pipe(map((response) => (response.data ?? []).map((item) => this.toAgrupadoMes(item))));
  }

  enviarRecordatorio(clienteId: string): Observable<string> {
    return this.http
      .post<ApiEnvelope<void>>(API_PATHS.clientesAdmin.enviarRecordatorio(clienteId), {})
      .pipe(map((response) => response.message ?? 'Recordatorio enviado.'));
  }

  private resolveSearchEndpoint(mode: ClienteSearchMode): string {
    switch (mode) {
      case 'nombre':
        return API_PATHS.clientesAdmin.buscarNombre;
      case 'correo':
        return API_PATHS.clientesAdmin.buscarCorreo;
      case 'telefono':
        return API_PATHS.clientesAdmin.buscarTelefono;
      default:
        return API_PATHS.clientesAdmin.buscarCorreo;
    }
  }

  private toClienteBusqueda(item: BackendClienteBusquedaResponse): ClienteBusqueda {
    return {
      clienteId: String(item.clienteId),
      nombre: item.nombre,
      email: item.email,
      telefono: item.telefono,
    };
  }

  private toClienteResumen(item: BackendClienteResumenResponse): ClienteResumen {
    return {
      clienteId: String(item.clienteId),
      nombre: item.nombre,
      email: item.email,
      telefono: item.telefono,
      fechaNacimiento: this.toDate(item.fechaNacimiento),
      clienteDesde: this.toDate(item.clienteDesde),
    };
  }

  private toResumen(item: BackendClienteVentasResumenResponse): ClienteVentasResumen {
    return {
      totalVisitas: item.totalVisitas ?? 0,
      totalGastado: item.totalGastado ?? 0,
      promedioPorVisita: item.promedioPorVisita ?? 0,
      ultimaVisita: this.toDate(item.ultimaVisita),
      clienteDesde: this.toDate(item.clienteDesde),
    };
  }

  private toVentaDetalle(item: BackendClienteVentaDetalle): VentaDetalle {
    return {
      visitaId: String(item.visitaId),
      subtotal: item.subtotal ?? 0,
      descuento: item.descuento ?? 0,
      total: item.total ?? 0,
      metodo: item.metodo ?? null,
      fechaHora: this.toDate(item.fechaHora) ?? new Date(),
      createdAt: this.toDate(item.createdAt),
      cajeroNombre: item.cajeroNombre ?? null,
    };
  }

  private toHistorial(response: BackendClienteVentasResponse): ClienteVentasHistory {
    return {
      cliente: this.toClienteResumen(response.cliente),
      resumen: this.toResumen(response.resumen),
      ventas: (response.ventas ?? []).map((item) => this.toVentaDetalle(item)),
      mensajeCumpleanos: response.mensajeCumpleanos ?? null,
      mensajeInactivo: response.mensajeInactivo ?? null,
      mostrarRecordatorio: Boolean(response.mostrarRecordatorio),
    };
  }

  private toAgrupadoAnio(item: BackendVentaAgrupadaAnioResponse): VentaAgrupadaAnio {
    return {
      anio: item.anio,
      total: item.total ?? 0,
      cantidad: item.cantidad ?? 0,
    };
  }

  private toAgrupadoMes(item: BackendVentaAgrupadaMesResponse): VentaAgrupadaMes {
    return {
      anio: item.anio,
      mes: item.mes,
      total: item.total ?? 0,
      cantidad: item.cantidad ?? 0,
    };
  }

  private toDate(value?: string | null): Date | null {
    if (!value) {
      return null;
    }
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }
}
