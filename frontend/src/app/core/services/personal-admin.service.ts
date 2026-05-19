import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import {
  ApiEnvelope,
  BackendCrearEmpleadoRequest,
  BackendEmpleadoResponse,
} from '../models/api.models';

export type RolEmpleadoAlta = 'ADMIN' | 'MESERO' | 'COCINERO' | 'CAJERO';

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

@Injectable({ providedIn: 'root' })
export class PersonalAdminService {
  private readonly http = inject(HttpClient);

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
}
