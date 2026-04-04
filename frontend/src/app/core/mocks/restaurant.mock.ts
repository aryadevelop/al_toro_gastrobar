import {
  Comanda,
  DashboardMetric,
  Decoracion,
  Insumo,
  Mesa,
  Notificacion,
  Preparacion,
  Producto,
  Reserva,
  Venta
} from '../models/domain.models';

export const MOCK_RESERVAS: Reserva[] = [
  {
    id: 'r-100',
    clienteId: 'u-2',
    guestName: 'Carlos Cliente',
    guests: 4,
    date: '2026-04-05',
    time: '20:00',
    status: 'CONFIRMED',
    tableCode: 'M12'
  },
  {
    id: 'r-101',
    clienteId: 'u-2',
    guestName: 'Carlos Cliente',
    guests: 2,
    date: '2026-04-15',
    time: '19:30',
    status: 'PENDING'
  }
];

export const MOCK_MESAS: Mesa[] = [
  { id: 'm-1', code: 'M01', seats: 2, zone: 'Terraza', status: 'AVAILABLE' },
  { id: 'm-2', code: 'M12', seats: 4, zone: 'Saln principal', status: 'RESERVED' },
  { id: 'm-3', code: 'M07', seats: 6, zone: 'Saln principal', status: 'OCCUPIED' }
];

export const MOCK_COMANDAS: Comanda[] = [
  {
    id: 'c-500',
    reservaId: 'r-100',
    mesaCode: 'M12',
    waiterId: 'u-3',
    status: 'IN_PREPARATION',
    createdAt: '2026-04-04T18:12:00Z',
    items: [
      {
        id: 'ci-1',
        productId: 'p-1',
        productName: 'Bife Ancho',
        quantity: 2,
        unitPrice: 18.5
      }
    ]
  }
];

export const MOCK_PRODUCTOS: Producto[] = [
  { id: 'p-1', name: 'Bife Ancho', category: 'Carnes', type: 'PREPARATION', price: 18.5, status: 'ACTIVE' },
  { id: 'p-2', name: 'Limonada', category: 'Bebidas', type: 'DIRECT_SALE', price: 4.5, status: 'ACTIVE' }
];

export const MOCK_PREPARACIONES: Preparacion[] = [
  {
    id: 'pr-1',
    name: 'Bife Ancho',
    estimatedMinutes: 18,
    status: 'ACTIVE',
    ingredients: ['Carne', 'Sal de mar', 'Mantequilla']
  }
];

export const MOCK_INSUMOS: Insumo[] = [
  { id: 'i-1', name: 'Carne Premium', unit: 'kg', currentStock: 35, minStock: 10, status: 'ACTIVE' },
  { id: 'i-2', name: 'Limon', unit: 'kg', currentStock: 9, minStock: 12, status: 'ACTIVE' }
];

export const MOCK_VENTAS: Venta[] = [
  {
    id: 'v-1',
    clienteId: 'u-2',
    comandaId: 'c-500',
    total: 56.5,
    paid: true,
    createdAt: '2026-04-04T21:00:00Z',
    payments: [
      {
        id: 'pay-1',
        saleId: 'v-1',
        method: 'CARD',
        amount: 56.5,
        paidAt: '2026-04-04T21:05:00Z'
      }
    ]
  }
];

export const MOCK_NOTIFICACIONES: Notificacion[] = [
  {
    id: 'n-1',
    type: 'ASSISTANCE',
    title: 'Mesa M12 solicita asistencia',
    description: 'Cliente solicita cuenta.',
    createdAt: '2026-04-04T20:40:00Z',
    read: false
  }
];

export const MOCK_DECORACIONES: Decoracion[] = [
  {
    id: 'd-1',
    title: 'Ambientacin Otoo',
    imageUrl: 'https://picsum.photos/seed/decor1/600/400',
    status: 'ACTIVE'
  }
];

export const MOCK_DASHBOARD_METRICS: DashboardMetric[] = [
  { id: 'dm-1', label: 'Ventas del da', value: 3200, trend: 8, tone: 'success' },
  { id: 'dm-2', label: 'Reservas activas', value: 24, trend: 2, tone: 'neutral' },
  { id: 'dm-3', label: 'Comandas pendientes', value: 7, trend: -1, tone: 'danger' }
];