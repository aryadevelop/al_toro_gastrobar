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
  valorDecoracion?: number;
  total?: number;
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
  mostrarBotonInasistencia?: boolean;
  tipo?: string;
  mostrarConfirmar?: boolean;
  mostrarAgregarAnticipo?: boolean;
  mostrarAgregarDevolucion?: boolean;
  mostrarCancelar?: boolean;
}

export interface BackendResumenPagoResponse {
  reservaId: number;
  clienteNombre: string;
  fechaHoraLlegada: string;
  numeroPersonas: number;
  estado: string;
  tipo: string;
  totalAPagar: number;      // valor total de la reserva (pre-orden + decoración)
  totalAnticipado: number;
  totalDevuelto: number;
  montoAbonado: number;     // neto = totalAnticipado - totalDevuelto
  saldoPendiente?: number | null;    // solo presente cuando estado=CONFIRMADA
  pendientePorDevolver?: number | null;
}

export interface BackendRegistrarAbonoRequest {
  tipo: 'ANTICIPO' | 'DEVOLUCION';
  monto: number;
  metodo: 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA' | 'OTRO';
  fechaHora: string;
}

export interface BackendRegistrarAbonoResponse {
  abonoId: number;
  tipo: string;
  estado: string;
  resumen?: BackendResumenPagoResponse | null;
}

export interface BackendMarcarInasistenciaResponse {
  reservaId: number;
  estado: string;
  zonaLiberada?: string;
  decoracionLiberada?: string;
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

export interface BackendItemBorradorResponse {
  comandaItemId: number;
  productoId: number;
  productoNombre: string;
  categoriaProducto: string;
  precioUnitario: number;
  cantidad: number;
  subtotal: number;
  descripcion?: string;
  menuGrupo?: string;
}

export interface BackendBorradorComandaResponse {
  visitaId: number;
  mesaIdentificador: string;
  comandaCocinaId?: number;
  comandaBarraId?: number;
  platos: BackendItemBorradorResponse[];
  bebidas: BackendItemBorradorResponse[];
  subTotal: number;
  total: number;
  notasCocina?: string;
  notasBarra?: string;
  puedeEnviarCocina: boolean;
  puedeEnviarBarra: boolean;
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
  clienteId?: number | null;
  puntosFidelizacion?: number | null;
  esCumpleanos?: boolean | null;
  puedeGenerarCuenta?: boolean | null;
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

export interface BackendClienteListadoResponse {
  clienteId: number;
  nombre: string;
  correoElectronico: string;
  telefono: string;
  totalVisitas: number;
  totalGastado: number;
  puntosAcumulados: number;
  estado: string;
  clienteFrecuente: boolean;
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

export interface BackendCrearEmpleadoRequest {
  nombre: string;
  correoElectronico: string;
  telefono: string;
  direccion?: string;
  roles: string[];
  fechaIngreso: string;
  password: string;
  passwordConfirmacion: string;
}

export interface BackendEmpleadoResponse {
  empleadoId: number;
  nombre: string;
  correoElectronico: string;
  telefono: string;
  direccion?: string | null;
  fechaIngreso: string;
  roles: string[];
  warning?: string | null;
}

export interface BackendEmpleadoListadoResponse {
  empleadoId: number;
  nombre: string;
  correoElectronico: string;
  telefono: string;
  direccion?: string | null;
  fechaIngreso: string;
  roles: string[];
  estado: string;
}

/* ── Producción (cocina / barra) ── */

export interface BackendComandaProduccionResumen {
  comandaId: number;
  estacion: string;
  comandaEstado: string;
  mesaIdentificador: string;
  meseroNombre: string;
  totalItems: number;
  createdAt: string;
  fechaHoraInicio?: string;
  fechaHoraListo?: string;
  notas?: string;
}

export interface BackendTableroProduccion {
  estaciones: string[];
  pendientes: BackendComandaProduccionResumen[];
  enPreparacion: BackendComandaProduccionResumen[];
  listos: BackendComandaProduccionResumen[];
}

export interface BackendItemDetalle {
  comandaItemId: number;
  productoId: number;
  productoNombre: string;
  categoriaProducto: string;
  cantidad: number;
  descripcion?: string;
  modificacionesMenu?: string[];
}

export interface BackendComandaProduccionDetalle {
  comandaId: number;
  estacion: string;
  comandaEstado: string;
  mesaIdentificador: string;
  meseroNombre: string;
  createdAt: string;
  fechaHoraInicio?: string;
  fechaHoraListo?: string;
  notas?: string;
  platos: BackendItemDetalle[];
  bebidas: BackendItemDetalle[];
  otros: BackendItemDetalle[];
}

export interface BackendProductoBusqueda {
  productoId: number;
  productoNombre: string;
  productoPrecio: number;
  productoCategoria: string;
}

/* ── Cuenta de mesa / Pago y cierre (pagos_caja) ── */

export interface BackendCuentaItemResponse {
  comandaItemId: number;
  nombreProducto: string;
  categoriaProducto: string; // "PLATO" | "BEBIDA" | "OTRO"
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  descripcion?: string | null;
  esModificado: boolean;
  menuGrupo?: string | null;
  esMenuEspecial: boolean;
}

export interface BackendAbonoItemCuenta {
  abonoId: number;
  monto: number;
  fechaHora: string;
  metodo: string;
  tipo: string;
}

export interface BackendCuentaPreliminarResponse {
  visitaId: number;
  clienteId?: number | null;
  clienteNombre?: string | null;
  clienteEmail?: string | null;
  puntosCanjeables?: number | null;
  puntosAcumulados?: number | null;
  fechaHoraLlegada?: string | null;
  meseroNombre?: string | null;
  mesaIdentificador?: string | null;
  items: BackendCuentaItemResponse[];
  decoracionNombre?: string | null;
  valorDecoracion?: number | null;
  totalPreorden: number;
  totalAPagar: number;
  anticipos?: BackendAbonoItemCuenta[] | null;
  montoAbonado?: number | null;
  saldoPendiente?: number | null;
}

export interface BackendClienteBuscarResponse {
  clienteId: number;
  nombre: string;
  email: string;
  telefono: string;
}

export interface BackendClientePuntosResponse {
  puntosActuales: number;
  puntosAcumulados: number;
}

export interface BackendItemAjuste {
  comandaItemId: number;
  cantidad: number;
  precio?: number | null;
}

export interface BackendAjustarItemsRequest {
  items?: BackendItemAjuste[] | null;
  eliminados?: number[] | null;
}

export interface BackendCerrarCuentaRequest {
  emailCajero: string;
  visitaId: number;
  descuento?: number | null;
  metodo: 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA';
}

/* ── Inventario (Admin) ── */

export interface BackendProductoAdminItem {
  productoId: number;
  nombre: string;
  categoria: string;
  precioVenta: number;
  stockActual: number;
  estado: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'DISCONTINUED';
}

/* ── Gestión de Estados (Admin) ── */

export interface BackendValidacionCambioEstado {
  tieneReservasFuturas: boolean;
  cantidadReservas: number;
}

export interface BackendPreparacionAfectada {
  id: number;
  nombre: string;
  categoria: string;
  estado: string;
  pedidosPendientes: number;
  otrosInsumosDescontinuados: boolean;
}

export interface BackendValidacionDescontinuarInsumo {
  preparacionesAfectadas: BackendPreparacionAfectada[];
}

export interface BackendEstadoHistorial {
  fechaHora: string;
  usuario: string;
  estadoAnterior: string;
  estadoNuevo: string;
  motivo?: string;
}

export interface BackendCambiarEstadoRequest {
  nuevoEstado: string;
  motivo?: string;
  notificarClientes?: boolean;
  accionPreparacionesAfectadas?: 'DESACTIVAR' | 'MANTENER' | 'REACTIVAR' | 'MANTENER_INACTIVAS';
}

export interface BackendInventarioItemBusqueda {
  id: number;
  nombre: string;
  tipo: 'PRODUCTO' | 'INSUMO';
  stockActual: number;
  unidad: string;
}

export interface BackendInventarioMovimientoRequest {
  tipoElemento: 'PRODUCTO' | 'INSUMO';
  elementoId: number;
  tipoMovimiento: 'INGRESO' | 'EGRESO';
  cantidad: number;
  fecha?: string;
  observaciones?: string;
}

export interface BackendDecoracionAdminResponse {
  decoracionId: number;
  decoracionNombre: string;
  decoracionEstado: string;
  decoracionCostoAdicional?: number | null;
  decoracionImagenUrl?: string | null;
  zonaIds: number[];
}

export interface BackendCrearDecoracionRequest {
  decoracionNombre: string;
  decoracionCostoAdicional?: number | null;
  zonaIds?: number[];
}

export interface BackendActualizarDecoracionRequest {
  decoracionNombre: string;
  decoracionCostoAdicional?: number | null;
  zonaIds?: number[];
}

