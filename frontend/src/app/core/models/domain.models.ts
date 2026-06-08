export type Role = 'ADMIN' | 'CLIENTE' | 'MESERO' | 'PRODUCCION' | 'CAJERO';

export interface User {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
  role: Role;
  status: 'ACTIVE' | 'INACTIVE';
  avatarUrl?: string;
  password?: string;
  createdAt: string;
}

export interface Cliente {
  id: string;
  userId: string;
  loyaltyTier: 'NEW' | 'FREQUENT' | 'VIP';
  notes?: string;
}

export interface Empleado {
  id: string;
  userId: string;
  employeeCode: string;
  role: Exclude<Role, 'CLIENTE'>;
  shift: 'MORNING' | 'AFTERNOON' | 'NIGHT';
  status: 'ACTIVE' | 'INACTIVE';
}

export interface Reserva {
  id: string;
  clienteId: string;
  guestName: string;
  phone?: string;
  guests: number;
  date: string;
  time: string;
  status: 'PENDING' | 'CONFIRMED' | 'ARRIVED' | 'CANCELLED' | 'COMPLETED';
  type?: 'BASIC' | 'SPECIAL';
  tableCode?: string;
  decorationId?: string;
  decorationName?: string;
  zoneId?: string;
  zoneName?: string;
  preorderItems?: ReservaPreorderItem[];
  notes?: string;
}

export interface ReservaPreorderItem {
  productId: string;
  productName: string;
  quantity: number;
  description?: string;
  isSpecialMenu?: boolean;
  modificationOptionIds?: string[];
}

export interface Mesa {
  id: string;
  code: string;
  seats: number;
  zone: string;
  status: 'AVAILABLE' | 'RESERVED' | 'OCCUPIED' | 'CLEANING';
}

export interface ComandaItem {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  notes?: string;
}

export interface Comanda {
  id: string;
  reservaId?: string;
  mesaCode: string;
  waiterId: string;
  status: 'PENDING' | 'IN_PREPARATION' | 'READY' | 'DELIVERED' | 'CLOSED';
  items: ComandaItem[];
  createdAt: string;
}

export interface Producto {
  id: string;
  name: string;
  category: string;
  type: 'DIRECT_SALE' | 'PREPARATION';
  price: number;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface Preparacion {
  id: string;
  name: string;
  estimatedMinutes: number;
  status: 'ACTIVE' | 'INACTIVE';
  ingredients: string[];
}

export interface Insumo {
  id: string;
  name: string;
  unit: string;
  currentStock: number;
  minStock: number;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface Pago {
  id: string;
  saleId: string;
  method: 'CASH' | 'CARD' | 'TRANSFER';
  amount: number;
  paidAt: string;
}

export interface Venta {
  id: string;
  clienteId?: string;
  comandaId: string;
  total: number;
  paid: boolean;
  createdAt: string;
  payments: Pago[];
}

export interface Notificacion {
  id: string;
  type: 'ASSISTANCE' | 'KITCHEN' | 'PAYMENT' | 'SYSTEM';
  title: string;
  description: string;
  createdAt: string;
  read: boolean;
}

export interface Decoracion {
  id: string;
  title: string;
  imageUrl: string;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface DashboardMetric {
  id: string;
  label: string;
  value: number;
  trend?: number;
  tone?: 'neutral' | 'success' | 'danger';
}

export interface AdminDashboardVentaResumen {
  totalVentas: number;
  reservasConcretadas: number;
}

export interface AdminDashboardMetodoPago {
  metodo: 'CASH' | 'CARD' | 'TRANSFER';
  total: number;
}

export interface AdminDashboardZonaVenta {
  zona: string;
  total: number;
}

export interface AdminDashboardTopPlato {
  nombre: string;
  cantidad: number;
  total: number;
}

export interface AdminDashboardMenuResumen {
  menuEspecial: number;
  carta: number;
}

export interface AdminDashboardMeseroRendimiento {
  mesero: string;
  mesasAtendidas: number;
  totalFacturado: number;
  promedioPorMesa: number;
  mesasActivas: number;
}

export interface AdminDashboardPedidoProduccion {
  id: string;
  cliente: string;
  mesa: string;
  minutosTranscurridos: number;
  items: string[];
}

export interface AdminDashboardPedidosProduccion {
  totalActivos: number;
  promedioMinutos: number;
  pedidos: AdminDashboardPedidoProduccion[];
}

export interface AdminDashboardPedidoListo {
  id: string;
  cliente: string;
  mesa: string;
  items: string[];
}

export interface AdminDashboardPersonalItem {
  nombre: string;
  rol: string;
  mesasActivas?: number;
}

export interface AdminDashboardPersonalGrupo {
  rol: string;
  total: number;
  personal: AdminDashboardPersonalItem[];
}

export interface AdminDashboardPersonalResumen {
  resumen: string;
  grupos: AdminDashboardPersonalGrupo[];
}

export interface AdminDashboardOcupacion {
  ocupadas: number;
  reservasPendientes: number;
}

export interface AdminDashboardData {
  fecha: string;
  ventasDelDia: AdminDashboardVentaResumen;
  ventasPorMetodo: AdminDashboardMetodoPago[];
  ventasPorZona: AdminDashboardZonaVenta[];
  topPlatos: AdminDashboardTopPlato[];
  menuEspecialVsCarta: AdminDashboardMenuResumen;
  variacionVsAyer: number;
  rendimientoMeseros: AdminDashboardMeseroRendimiento[];
  pedidosProduccion: AdminDashboardPedidosProduccion;
  pedidosListos: AdminDashboardPedidoListo[];
  personalTurno: AdminDashboardPersonalResumen;
  ocupacion: AdminDashboardOcupacion;
}

export interface SearchFilter {
  key: string;
  label: string;
  type: 'text' | 'select' | 'date';
  options?: Array<{ label: string; value: string }>;
}

export interface PagedResult<T> {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
}