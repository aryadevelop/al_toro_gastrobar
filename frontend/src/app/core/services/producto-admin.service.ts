import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { ApiEnvelope, BackendProductoAdminItem } from '../models/api.models';

@Injectable({
  providedIn: 'root'
})
export class ProductoAdminService {
  constructor(private readonly http: HttpClient) {}

  listarProductosVentaDirecta(): Observable<ApiEnvelope<BackendProductoAdminItem[]>> {
    return this.http.get<ApiEnvelope<BackendProductoAdminItem[]>>(API_PATHS.adminProductos.listar);
  }
}
