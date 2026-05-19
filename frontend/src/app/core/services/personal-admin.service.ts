import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import {
  ApiEnvelope,
  BackendCrearEmpleadoRequest,
  BackendEmpleadoListadoResponse,
  BackendEmpleadoResponse,
} from '../models/api.models';

export type RolEmpleadoAlta = 'ADMIN' | 'MESERO' | 'COCINERO' | 'CAJERO';
export type RolEmpleadoFiltro = 'MESERO' | 'CAJERO' | 'BARTENDER';
export type EstadoEmpleadoFiltro = 'ACTIVO' | 'INACTIVO';

export interface CrearEmpleadoPayload {
  nombre: string;
  correoElectronico: string;
  telefono: string;
  direccion?: string;
  roles: RolEmpleadoAlta[];
  fechaIngreso: string;
  password: string;
  passwordConfirmacion: string;
}

export interface EmpleadoCreado {
  empleadoId: string;
  nombre: string;
  correoElectronico: string;
  warning?: string;
}

export interface EmpleadoListado {
  empleadoId: string;
  nombre: string;
  correoElectronico: string;
  telefono: string;
  fechaIngreso: Date | null;
  roles: string[];
  estado: string;
}

export interface EmpleadoListadoFiltros {
  rol?: RolEmpleadoFiltro | '';
  estado?: EstadoEmpleadoFiltro | '';
  nombre?: string;
}

@Injectable({ providedIn: 'root' })
export class PersonalAdminService {
  private readonly http = inject(HttpClient);

  listarEmpleados(filtros: EmpleadoListadoFiltros = {}): Observable<EmpleadoListado[]> {
    let params = new HttpParams();

    if (filtros.rol) {
      params = params.set('rol', filtros.rol);
    }

    if (filtros.estado) {
      params = params.set('estado', filtros.estado);
    }

    const nombre = filtros.nombre?.trim();
    if (nombre) {
      params = params.set('nombre', nombre);
    }

    return this.http
      .get<ApiEnvelope<BackendEmpleadoListadoResponse[]>>(API_PATHS.empleados.listar, { params })
      .pipe(map((response) => (response.data ?? []).map((item) => this.toEmpleadoListado(item))));
  }

  crearEmpleado(payload: CrearEmpleadoPayload): Observable<EmpleadoCreado> {
    const request: BackendCrearEmpleadoRequest = {
      ...payload,
      direccion: payload.direccion?.trim() || undefined,
    };

    return this.http
      .post<ApiEnvelope<BackendEmpleadoResponse>>(API_PATHS.empleados.crear, request)
      .pipe(
        map((response) => ({
          empleadoId: String(response.data.empleadoId),
          nombre: response.data.nombre,
          correoElectronico: response.data.correoElectronico,
          warning: response.data.warning || undefined,
        }))
      );
  }

  private toEmpleadoListado(item: BackendEmpleadoListadoResponse): EmpleadoListado {
    return {
      empleadoId: String(item.empleadoId),
      nombre: item.nombre,
      correoElectronico: item.correoElectronico,
      telefono: item.telefono,
      fechaIngreso: this.toDate(item.fechaIngreso),
      roles: item.roles ?? [],
      estado: item.estado,
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
