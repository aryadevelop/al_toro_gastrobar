import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { MOCK_COMANDAS } from '../mocks/restaurant.mock';
import { ApiEnvelope, BackendBorradorComandaResponse } from '../models/api.models';
import { Comanda } from '../models/domain.models';
import { MockApiService } from './mock-api.service';

export interface ComandaDraftItem {
  comandaItemId: string;
  productoId: string;
  productoNombre: string;
  categoriaProducto: string;
  precioUnitario: number;
  cantidad: number;
  subtotal: number;
  descripcion?: string;
  menuGrupo?: string;
}

export interface ComandaDraftData {
  visitaId: string;
  mesaIdentificador: string;
  comandaCocinaId?: string;
  comandaBarraId?: string;
  platos: ComandaDraftItem[];
  bebidas: ComandaDraftItem[];
  subTotal: number;
  total: number;
  notasCocina?: string;
  notasBarra?: string;
  puedeEnviarCocina: boolean;
  puedeEnviarBarra: boolean;
}

@Injectable({ providedIn: 'root' })
export class ComandaService {
  private readonly http = inject(HttpClient);

  constructor(private readonly mockApiService: MockApiService) {}

  list(): Observable<Comanda[]> {
    return this.mockApiService.respond([...MOCK_COMANDAS]);
  }

  updateStatus(id: string, status: Comanda['status']): Observable<Comanda | null> {
    const index = MOCK_COMANDAS.findIndex((item) => item.id === id);
    if (index < 0) {
      return this.mockApiService.respond(null);
    }

    MOCK_COMANDAS[index] = {
      ...MOCK_COMANDAS[index],
      status
    };
    return this.mockApiService.respond(MOCK_COMANDAS[index], 350);
  }

  getBorrador(visitaId: string): Observable<ComandaDraftData> {
    const params = new HttpParams().set('visitaId', visitaId);
    return this.http
      .get<ApiEnvelope<BackendBorradorComandaResponse>>(API_PATHS.comandas.borrador, { params })
      .pipe(map((response) => this.mapDraft(response.data)));
  }

  addItem(payload: { visitaId: string; productoId: string; cantidad: number; descripcion?: string }): Observable<ComandaDraftData> {
    return this.http
      .post<ApiEnvelope<BackendBorradorComandaResponse>>(API_PATHS.comandas.borradorItems, {
        visitaId: Number(payload.visitaId),
        productoId: Number(payload.productoId),
        cantidad: payload.cantidad,
        descripcion: payload.descripcion,
      })
      .pipe(map((response) => this.mapDraft(response.data)));
  }

  updateItem(itemId: string, payload: { cantidad?: number; descripcion?: string }): Observable<ComandaDraftData> {
    return this.http
      .patch<ApiEnvelope<BackendBorradorComandaResponse>>(API_PATHS.comandas.borradorItem(itemId), payload)
      .pipe(map((response) => this.mapDraft(response.data)));
  }

  deleteItem(itemId: string): Observable<ComandaDraftData> {
    return this.http
      .delete<ApiEnvelope<BackendBorradorComandaResponse>>(API_PATHS.comandas.borradorItem(itemId))
      .pipe(map((response) => this.mapDraft(response.data)));
  }

  updateNotas(comandaId: string, notas: string): Observable<ComandaDraftData> {
    return this.http
      .patch<ApiEnvelope<BackendBorradorComandaResponse>>(API_PATHS.comandas.notas(comandaId), { notas })
      .pipe(map((response) => this.mapDraft(response.data)));
  }

  enviarAProduccion(comandaId: string): Observable<ComandaDraftData> {
    return this.http
      .post<ApiEnvelope<BackendBorradorComandaResponse>>(API_PATHS.comandas.enviar(comandaId), {})
      .pipe(map((response) => this.mapDraft(response.data)));
  }

  cancelarFormulario(visitaId: string): Observable<void> {
    const params = new HttpParams().set('visitaId', visitaId);
    return this.http.delete<ApiEnvelope<void>>(API_PATHS.comandas.borrador, { params }).pipe(map(() => undefined));
  }

  private mapDraft(source: BackendBorradorComandaResponse): ComandaDraftData {
    const mapItems = (items: BackendBorradorComandaResponse['platos']): ComandaDraftItem[] =>
      (items ?? []).map((item) => ({
        comandaItemId: String(item.comandaItemId),
        productoId: String(item.productoId),
        productoNombre: item.productoNombre,
        categoriaProducto: item.categoriaProducto,
        precioUnitario: Number(item.precioUnitario) || 0,
        cantidad: Number(item.cantidad) || 0,
        subtotal: Number(item.subtotal) || 0,
        descripcion: item.descripcion,
        menuGrupo: item.menuGrupo,
      }));

    return {
      visitaId: String(source.visitaId),
      mesaIdentificador: source.mesaIdentificador,
      comandaCocinaId: source.comandaCocinaId ? String(source.comandaCocinaId) : undefined,
      comandaBarraId: source.comandaBarraId ? String(source.comandaBarraId) : undefined,
      platos: mapItems(source.platos),
      bebidas: mapItems(source.bebidas),
      subTotal: Number(source.subTotal) || 0,
      total: Number(source.total) || 0,
      notasCocina: source.notasCocina,
      notasBarra: source.notasBarra,
      puedeEnviarCocina: Boolean(source.puedeEnviarCocina),
      puedeEnviarBarra: Boolean(source.puedeEnviarBarra),
    };
  }
}