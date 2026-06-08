import {
  Comanda,
  AdminDashboardData,
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
  { id: 'm-2', code: 'M12', seats: 4, zone: 'Salón principal', status: 'RESERVED' },
  { id: 'm-3', code: 'M07', seats: 6, zone: 'Salón principal', status: 'OCCUPIED' }
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
    id: 'VENTA-1001',
    clienteId: 'u-2',
    comandaId: 'c-500',
    total: 56.5,
    paid: true,
    createdAt: '2026-06-03T13:25:00Z',
    payments: [
      {
        id: 'pay-1',
        saleId: 'VENTA-1001',
        method: 'CARD',
        amount: 56.5,
        paidAt: '2026-06-03T13:27:00Z'
      }
    ]
  },
  {
    id: 'VENTA-1002',
    clienteId: 'u-5',
    comandaId: 'c-510',
    total: 72.0,
    paid: true,
    createdAt: '2026-06-02T20:10:00Z',
    payments: [
      {
        id: 'pay-2',
        saleId: 'VENTA-1002',
        method: 'CASH',
        amount: 72.0,
        paidAt: '2026-06-02T20:12:00Z'
      }
    ]
  },
  {
    id: 'VENTA-1003',
    clienteId: 'u-7',
    comandaId: 'c-520',
    total: 134.4,
    paid: true,
    createdAt: '2026-05-28T18:40:00Z',
    payments: [
      {
        id: 'pay-3',
        saleId: 'VENTA-1003',
        method: 'TRANSFER',
        amount: 134.4,
        paidAt: '2026-05-28T18:42:00Z'
      }
    ]
  },
  {
    id: 'VENTA-1004',
    clienteId: 'u-2',
    comandaId: 'c-521',
    total: 45.0,
    paid: true,
    createdAt: '2026-05-28T12:15:00Z',
    payments: [
      {
        id: 'pay-4',
        saleId: 'VENTA-1004',
        method: 'CASH',
        amount: 45.0,
        paidAt: '2026-05-28T12:16:00Z'
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
    title: 'Ambientación Otoño',
    imageUrl: 'https://picsum.photos/seed/decor1/600/400',
    status: 'ACTIVE'
  }
];

export const MOCK_DASHBOARD_METRICS: DashboardMetric[] = [
  { id: 'dm-1', label: 'Ventas del día', value: 3200, trend: 8, tone: 'success' },
  { id: 'dm-2', label: 'Reservas activas', value: 24, trend: 2, tone: 'neutral' },
  { id: 'dm-3', label: 'Comandas pendientes', value: 7, trend: -1, tone: 'danger' }
];

export const MOCK_ADMIN_DASHBOARD: AdminDashboardData = {
  fecha: '2026-06-03',
  ventasDelDia: {
    totalVentas: 4250,
    reservasConcretadas: 18,
  },
  ventasPorMetodo: [
    { metodo: 'CASH', total: 1200 },
    { metodo: 'CARD', total: 1900 },
    { metodo: 'TRANSFER', total: 1150 },
  ],
  ventasPorZona: [
    { zona: 'Terraza', total: 1450 },
    { zona: 'Salon principal', total: 2200 },
    { zona: 'Barra', total: 600 },
  ],
  topPlatos: [
    { nombre: 'Bife Ancho', cantidad: 12, total: 540 },
    { nombre: 'Limonada', cantidad: 20, total: 90 },
    { nombre: 'Ensalada criolla', cantidad: 9, total: 180 },
  ],
  menuEspecialVsCarta: {
    menuEspecial: 980,
    carta: 3270,
  },
  variacionVsAyer: 15,
  rendimientoMeseros: [
    { mesero: 'Laura Morales', mesasAtendidas: 6, totalFacturado: 980, promedioPorMesa: 163, mesasActivas: 2 },
    { mesero: 'Diego Rojas', mesasAtendidas: 5, totalFacturado: 820, promedioPorMesa: 164, mesasActivas: 1 },
    { mesero: 'Camila Ortega', mesasAtendidas: 7, totalFacturado: 1110, promedioPorMesa: 159, mesasActivas: 3 },
  ],
  pedidosProduccion: {
    totalActivos: 8,
    promedioMinutos: 18,
    pedidos: [
      {
        id: 'p-100',
        cliente: 'Mario Gomez',
        mesa: 'M12',
        minutosTranscurridos: 12,
        items: ['Bife Ancho', 'Papas rusticas'],
      },
      {
        id: 'p-101',
        cliente: 'Sofia Alvarez',
        mesa: 'M05',
        minutosTranscurridos: 19,
        items: ['Menu especial', 'Vino tinto'],
      },
      {
        id: 'p-102',
        cliente: 'Carlos Ruiz',
        mesa: 'M03',
        minutosTranscurridos: 24,
        items: ['Lomo al trapo', 'Ensalada'],
      },
      {
        id: 'p-103',
        cliente: 'Ana Perez',
        mesa: 'M08',
        minutosTranscurridos: 9,
        items: ['Limonada', 'Chorizo criollo'],
      },
      {
        id: 'p-104',
        cliente: 'Pedro Ortiz',
        mesa: 'M10',
        minutosTranscurridos: 28,
        items: ['Menu especial', 'Postre de la casa'],
      },
    ],
  },
  pedidosListos: [
    {
      id: 'l-200',
      cliente: 'Valentina Pardo',
      mesa: 'M04',
      items: ['Costillas BBQ', 'Agua con gas'],
    },
    {
      id: 'l-201',
      cliente: 'Felipe Torres',
      mesa: 'M09',
      items: ['Menu especial', 'Cafe americano'],
    },
  ],
  personalTurno: {
    resumen: 'Personal hoy: 8 meseros, 3 cocineros, 2 bartenders',
    grupos: [
      {
        rol: 'Meseros',
        total: 8,
        personal: [
          { nombre: 'Laura Morales', rol: 'MESERO', mesasActivas: 2 },
          { nombre: 'Diego Rojas', rol: 'MESERO', mesasActivas: 1 },
          { nombre: 'Camila Ortega', rol: 'MESERO', mesasActivas: 3 },
        ],
      },
      {
        rol: 'Cocineros',
        total: 3,
        personal: [
          { nombre: 'Ramon Arias', rol: 'COCINERO' },
          { nombre: 'Luis Campos', rol: 'COCINERO' },
          { nombre: 'Marta Rios', rol: 'COCINERO' },
        ],
      },
      {
        rol: 'Bartenders',
        total: 2,
        personal: [
          { nombre: 'Nicolas Franco', rol: 'BARTENDER' },
          { nombre: 'Juliana Gil', rol: 'BARTENDER' },
        ],
      },
    ],
  },
  ocupacion: {
    ocupadas: 12,
    reservasPendientes: 5,
  },
};
