export interface ApiEnvelope<T> {
  success: boolean;
  code?: string;
  message?: string;
  data: T;
}

export interface BackendDisponibilidadResponse {
  disponible: boolean;
  decoraciones: Array<{
    decoracionId: number;
    nombre?: string;
    decoracionNombre?: string;
    puedeSeleccionarZona?: boolean;
    zonaIdsCompatibles?: number[];
  }>;
  zonas: Array<{
    zonaId: number;
    nombre?: string;
    zonaNombre?: string;
    capacidad?: number;
  }>;
}

export interface BackendPreOrdenItem {
  comandaItemId?: number;
  productoId: number;
  productoNombre: string;
  cantidad: number;
  precioUnitario?: number;
  descripcion?: string;
  modificaciones?: Array<{
    opcionId: number;
    opcionNombre: string;
    tipoComponente: string;
  }>;
}

export interface BackendAbonoItem {
  abonoId: number;
  monto: number;
  fechaHora: string;
  metodo: string;
  tipo: string;
}

export interface BackendReservaDetalle {
  reservaId: number;
  fechaHoraLlegada: string;
  numeroPersonas: number;
  estado: string;
  tipo?: string;
  zonaNombre?: string;
  decoracionNombre?: string;
  notas?: string;
  clienteId?: number;
  clienteNombre?: string;
  preOrdenItems?: BackendPreOrdenItem[];
  preOrdenTotal?: number;
  abonos?: BackendAbonoItem[];
  totalAbonado?: number;
}

export interface BackendCrearReservaRequest {
  fechaHoraLlegada: string;
  numeroPersonas: number;
  decoracionId?: number;
  zonaId?: number;
  notas?: string;
  preOrden?: Array<{
    productoId: number;
    cantidad: number;
    descripcion?: string;
    esMenuEspecial?: boolean;
    opcionesModificacion?: number[];
  }>;
}

export interface BackendCategoriaCarta {
  categoriaId: number;
  categoriaNombre: string;
  orden: number;
  productos: Array<{
    productoId: number;
    productoNombre: string;
    productoDescripcion?: string;
    productoPrecio: number;
    productoCategoria: string;
  }>;
}

export interface BackendMenuEspecial {
  productoId: number;
  productoNombre: string;
  productoDescripcion?: string;
  productoPrecio: number;
  modificacionesPorComponente: Array<{
    tipoComponente: string;
    tipoComponenteDescripcion: string;
    opciones: Array<{
      opcionId: number;
      opcionNombre: string;
    }>;
  }>;
}

export interface BackendVisitaResumen {
  visitaId: number;
  reservaId?: number;
  fechaHoraLlegada: string;
  numeroPersonas: number;
  mesaIdentificador?: string;
  zonaNombre?: string;
  estadoVisita: string;
  montoTotal?: number;
}

export interface BackendClientePuntos {
  puntosActuales: number;
  puntosAcumulados: number;
}
