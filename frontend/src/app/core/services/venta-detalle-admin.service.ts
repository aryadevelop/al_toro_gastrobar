import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import {
  ApiEnvelope,
  BackendVentaDetalleResponse,
} from '../models/api.models';

export interface VentaDetalleCliente {
  nombre: string;
  telefono?: string;
}

export interface VentaDetalleMesa {
  identificador: string;
  zona?: string;
}

export interface VentaDetalleItem {
  nombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  especificaciones?: string;
}

export interface VentaDetalleMenuEspecial {
  nombreMenu: string;
  valorPorPersona: number;
  numeroPersonas: number;
  totalCalculado: number;
}

export interface VentaDetalleServicioAdicional {
  nombre: string;
  costo: number;
}

export interface VentaDetalleAdmin {
  ventaId: string;
  fechaHora: string;
  cliente: VentaDetalleCliente;
  mesa?: VentaDetalleMesa;
  meseroNombre?: string;
  items: VentaDetalleItem[];
  menuEspecial?: VentaDetalleMenuEspecial;
  serviciosAdicionales: VentaDetalleServicioAdicional[];
  notaReserva?: string;
  subtotal: number;
  total: number;
  metodoPago?: string;
  estadoReserva?: string;
  alertaReservaCancelada?: string;
}

@Injectable({ providedIn: 'root' })
export class VentaDetalleAdminService {
  private readonly http = inject(HttpClient);

  getDetalle(visitaId: string): Observable<VentaDetalleAdmin> {
    return this.http
      .get<ApiEnvelope<BackendVentaDetalleResponse>>(API_PATHS.ventas.detalle(visitaId))
      .pipe(map((response) => this.toVentaDetalle(response.data)));
  }

  private toVentaDetalle(data: BackendVentaDetalleResponse): VentaDetalleAdmin {
    return {
      ventaId: String(data.ventaId),
      fechaHora: data.fechaHora,
      cliente: {
        nombre: data.cliente?.nombre || 'Cliente ocasional',
        telefono: data.cliente?.telefono || undefined,
      },
      mesa: data.mesa
        ? {
            identificador: data.mesa.identificador,
            zona: data.mesa.zona || undefined,
          }
        : undefined,
      meseroNombre: data.meseroNombre || undefined,
      items: (data.items ?? []).map((item) => ({
        nombre: item.nombre,
        cantidad: item.cantidad,
        precioUnitario: item.precioUnitario,
        subtotal: item.subtotal,
        especificaciones: item.especificaciones || undefined,
      })),
      menuEspecial: data.menuEspecial
        ? {
            nombreMenu: data.menuEspecial.nombreMenu,
            valorPorPersona: data.menuEspecial.valorPorPersona,
            numeroPersonas: data.menuEspecial.numeroPersonas,
            totalCalculado: data.menuEspecial.totalCalculado,
          }
        : undefined,
      serviciosAdicionales: (data.serviciosAdicionales ?? []).map((servicio) => ({
        nombre: servicio.nombre,
        costo: servicio.costo,
      })),
      notaReserva: data.notaReserva || undefined,
      subtotal: data.subtotal ?? 0,
      total: data.total ?? 0,
      metodoPago: data.metodoPago || undefined,
      estadoReserva: data.estadoReserva || undefined,
      alertaReservaCancelada: data.alertaReservaCancelada || undefined,
    };
  }
}
