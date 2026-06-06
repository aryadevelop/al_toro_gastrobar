import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { 
  ApiEnvelope, 
  BackendDecoracionAdminResponse, 
  BackendCrearDecoracionRequest, 
  BackendActualizarDecoracionRequest 
} from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class DecoracionAdminService {
  constructor(private readonly http: HttpClient) {}

  listarDecoraciones(): Observable<ApiEnvelope<BackendDecoracionAdminResponse[]>> {
    return this.http.get<ApiEnvelope<BackendDecoracionAdminResponse[]>>(API_PATHS.adminDecoraciones.listar);
  }

  crearDecoracion(request: BackendCrearDecoracionRequest): Observable<ApiEnvelope<BackendDecoracionAdminResponse>> {
    return this.http.post<ApiEnvelope<BackendDecoracionAdminResponse>>(API_PATHS.adminDecoraciones.crear, request);
  }

  actualizarDecoracion(id: number, request: BackendActualizarDecoracionRequest): Observable<ApiEnvelope<BackendDecoracionAdminResponse>> {
    return this.http.put<ApiEnvelope<BackendDecoracionAdminResponse>>(API_PATHS.adminDecoraciones.actualizar(id), request);
  }

  eliminarDecoracion(id: number): Observable<ApiEnvelope<void>> {
    return this.http.delete<ApiEnvelope<void>>(API_PATHS.adminDecoraciones.eliminar(id));
  }

  subirImagenDecoracion(id: number, file: File): Observable<ApiEnvelope<string>> {
    const formData = new FormData();
    formData.append('imagen', file);
    return this.http.post<ApiEnvelope<string>>(API_PATHS.adminDecoraciones.subirImagen(id), formData);
  }

  eliminarImagenDecoracion(id: number): Observable<ApiEnvelope<void>> {
    return this.http.delete<ApiEnvelope<void>>(API_PATHS.adminDecoraciones.eliminarImagen(id));
  }
}
