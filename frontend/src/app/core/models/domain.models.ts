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
  guests: number;
  date: string;
  time: string;
  status: 'PENDING' | 'CONFIRMED' | 'ARRIVED' | 'CANCELLED' | 'COMPLETED';
  tableCode?: string;
  notes?: string;
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