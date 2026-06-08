import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PATHS } from '../config/api-paths';
import { ApiEnvelope, BackendCategoriaCarta, BackendMenuEspecial, BackendProductoBusqueda } from '../models/api.models';

export interface CartaCatalogItem {
  productId: string;
  productName: string;
  category: 'Platos' | 'Bebidas';
  description: string;
  unitPrice: number;
}

export interface SpecialMenuCatalogOption {
  optionId: string;
  optionName: string;
}

export interface SpecialMenuCatalogItem {
  id: string;
  name: string;
  description: string;
  pricePerPerson: number;
  customizationOptions: SpecialMenuCatalogOption[];
}

@Injectable({ providedIn: 'root' })
export class ProductCatalogService {
  private readonly http = inject(HttpClient);

  listCartaItems(): Observable<CartaCatalogItem[]> {
    return this.http
      .get<ApiEnvelope<BackendCategoriaCarta[]>>(API_PATHS.productos.carta)
      .pipe(
        map((response) =>
          response.data
            .flatMap((category) =>
              category.productos.map((product) => ({
                productId: String(product.productoId),
                productName: product.productoNombre,
                category: this.mapCategory(category.categoriaNombre, product.productoCategoria),
                description: product.productoDescripcion ?? 'Preparación disponible en la carta del día.',
                unitPrice: Number(product.productoPrecio),
              }))
            )
            .sort((a, b) => a.productName.localeCompare(b.productName, 'es'))
        )
      );
  }

  /**
   * Busca productos por nombre parcial usando el endpoint /api/productos/buscar.
   * Accesible para cualquier usuario autenticado (incluye MESERO).
   */
  buscarProductos(query: string): Observable<CartaCatalogItem[]> {
    const params = { q: query };
    return this.http
      .get<ApiEnvelope<BackendProductoBusqueda[]>>(API_PATHS.productos.buscar, { params })
      .pipe(
        map((response) =>
          response.data.map((product) => ({
            productId: String(product.productoId),
            productName: product.productoNombre,
            category: product.productoCategoria === 'BEBIDA' ? 'Bebidas' : 'Platos' as 'Platos' | 'Bebidas',
            description: '',
            unitPrice: Number(product.productoPrecio),
          }))
        )
      );
  }

  listSpecialMenus(): Observable<SpecialMenuCatalogItem[]> {
    return this.http
      .get<ApiEnvelope<BackendMenuEspecial[]>>(API_PATHS.productos.menuEspecial)
      .pipe(
        map((response) =>
          response.data.map((menu) => ({
            id: String(menu.productoId),
            name: menu.productoNombre,
            description: menu.productoDescripcion ?? 'Menú especial disponible para grupos.',
            pricePerPerson: Number(menu.productoPrecio),
            customizationOptions: menu.modificacionesPorComponente
              .flatMap((group) => group.opciones)
              .map((option) => ({
                optionId: String(option.opcionId),
                optionName: option.opcionNombre,
              })),
          }))
        )
      );
  }

  private mapCategory(categoriaNombre: string, productoCategoria: string): 'Platos' | 'Bebidas' {
    const normalized = `${categoriaNombre} ${productoCategoria}`.toLowerCase();
    return normalized.includes('bebida') ? 'Bebidas' : 'Platos';
  }
}
