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
    imagenUrl?: string;
  }>;
  zonas: Array<{
    zonaId: number;
    nombre?: string;
    zonaNombre?: string;
    capacidad?: number;
    imagenUrl?: string;
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
  zonaId?: number;
  decoracionId?: number;
  zonaNombre?: string;
  decoracionNombre?: string;
  notas?: string;
  clienteId?: number;
  clienteNombre?: string;
  clienteTelefono?: string;
  preOrdenItems?: BackendPreOrdenItem[];
  preOrdenTotal?: number;
  abonos?: BackendAbonoItem[];
  totalAbonado?: number;
}

export interface BackendReservaConsultaItem {
  reservaId: number;
  clienteNombre?: string;
  zonaId?: number;
  zonaNombre?: string;
  decoracionNombre?: string;
  horaLlegada?: string; // HH:mm
  numeroPersonas?: number;
  clienteTelefono?: string;
  estado?: string;
}

export interface BackendListadoReservasResponse {
  reservas: BackendReservaConsultaItem[];
  resumenZonas: Array<{
    zonaId?: number;
    zonaNombre: string;
    cantidadReservas: number;
  }>;
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

export interface BackendModificarReservaResponse {
  reservaId: number;
  estado: string;
  tipo?: string;
  fechaHoraLlegada: string;
  numeroPersonas: number;
  zonaNombre?: string;
  decoracionNombre?: string;
  notas?: string;
  requiereWhatsApp: boolean;
  mensajeWhatsApp?: string;
}

export interface BackendCancelarReservaResponse {
  reservaId: number;
  estado: string;
  tipo?: string;
  fechaHoraLlegada: string;
  numeroPersonas: number;
  requiereWhatsApp: boolean;
  mensajeWhatsApp?: string;
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

export interface BackendItemVisita {
  comandaItemId: number;
  nombreProducto: string;
  cantidad: number;
  estadoItem: string;
  precioUnitario: number;
  subtotal: number;
}

export interface BackendEstadoVisita {
  visitaId: number;
  mesaIdentificador?: string;
  visitaCerrada: boolean;
  items: BackendItemVisita[];
  total: number;
  asistenciaSolicitada: boolean;
  notificacionAsistenciaId?: number;
}

export interface BackendNotificacionAsistencia {
  notificacionId: number;
}

export interface BackendAtenderCambioResponse {
  comandaId: number;
}

export interface BackendNotificacionActiva {
  notificacionId: number;
  tipo: string;
  fechaHora: string;
}

export interface BackendMesaMapa {
  mesaId: number;
  visitaId: number;
  identificador: string;
  numeroPersonas: number;
  estado: string;
  nombreMesero?: string;
  esMesaPropia?: boolean;
  tieneBorrador?: boolean;
  notificacionesActivas?: BackendNotificacionActiva[];
}

export interface BackendZonaMesas {
  zonaId: number;
  zonaNombre: string;
  cantidadMesasActivas: number;
  mesas: BackendMesaMapa[];
}

export interface BackendMapaMesasResponse {
  zonas: BackendZonaMesas[];
}

export interface BackendItemComandaEnProduccion {
  nombreProducto: string;
  descripcion?: string;
  categoriaProducto?: string;
  cantidad: number;
  estadoComanda?: string;
}

export interface BackendMesaDetalleResponse {
  mesaId: number;
  visitaId?: number;
  identificador: string;
  nombreCliente?: string;
  horaLlegada?: string;
  numeroPersonas?: number;
  estado?: string;
  notasReserva?: string;
  notasMesa?: string;
  notasComandas?: string;
  itemsComanda?: BackendItemComandaEnProduccion[];
}

export interface BackendMesaItemsProduccionResponse {
  identificadorMesa: string;
  itemsEnProduccion: BackendItemComandaEnProduccion[];
}

export interface BackendZonaDisponibleMesaResponse {
  zonaId: number;
  zonaNombre: string;
  capacidadTotal: number;
  personasOcupadas: number;
  disponibilidad: number;
}

export interface BackendAsignarMesaRequest {
  mesaIdentificador: string;
  zonaId: number;
  numeroPersonas: number;
  reservaId?: number;
  mesaNotas?: string;
}

export interface BackendMesaAsignadaResponse {
  visitaId: number;
  mesaIdentificador: string;
  zonaId: number;
  zonaNombre: string;
  numeroPersonas: number;
  estadoMesa: string;
  emailMesero: string;
  reservaId?: number;
}

export interface BackendVentaDetalleCliente {
  nombre: string;
  telefono?: string | null;
}

export interface BackendVentaDetalleMesa {
  identificador: string;
  zona?: string | null;
}

export interface BackendVentaDetalleItem {
  nombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  especificaciones?: string | null;
}

export interface BackendVentaDetalleMenuEspecial {
  nombreMenu: string;
  valorPorPersona: number;
  numeroPersonas: number;
  totalCalculado: number;
}

export interface BackendVentaDetalleServicioAdicional {
  nombre: string;
  costo: number;
}

export interface BackendVentaDetalleResponse {
  ventaId: number;
  fechaHora: string;
  cliente?: BackendVentaDetalleCliente | null;
  mesa?: BackendVentaDetalleMesa | null;
  meseroNombre?: string | null;
  items: BackendVentaDetalleItem[];
  menuEspecial?: BackendVentaDetalleMenuEspecial | null;
  serviciosAdicionales: BackendVentaDetalleServicioAdicional[];
  notaReserva?: string | null;
  subtotal: number;
  total: number;
  metodoPago?: string | null;
  estadoReserva?: string | null;
  alertaReservaCancelada?: string | null;
}

export interface BackendClienteBusquedaResponse {
  clienteId: number;
  nombre: string;
  email: string;
  telefono: string;
}

export interface BackendClienteResumenResponse {
  clienteId: number;
  nombre: string;
  email: string;
  telefono: string;
  fechaNacimiento?: string | null;
  clienteDesde?: string | null;
}

export interface BackendClienteVentasResumenResponse {
  totalVisitas: number;
  totalGastado: number;
  promedioPorVisita: number;
  ultimaVisita?: string | null;
  clienteDesde?: string | null;
}

export interface BackendClienteVentaDetalle {
  visitaId: number;
  subtotal: number;
  descuento: number;
  total: number;
  metodo?: string | null;
  fechaHora: string;
  createdAt?: string | null;
  cajeroNombre?: string | null;
}

export interface BackendClienteVentasResponse {
  cliente: BackendClienteResumenResponse;
  resumen: BackendClienteVentasResumenResponse;
  ventas: BackendClienteVentaDetalle[];
  mensajeCumpleanos?: string | null;
  mensajeInactivo?: string | null;
  mostrarRecordatorio: boolean;
}

export interface BackendVentaAgrupadaAnioResponse {
  anio: number;
  total: number;
  cantidad: number;
}

export interface BackendVentaAgrupadaMesResponse {
  anio: number;
  mes: number;
  total: number;
  cantidad: number;
}
