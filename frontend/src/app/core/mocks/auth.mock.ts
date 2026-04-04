import { Role, User } from '../models/domain.models';

export interface LoginCredentials {
  email: string;
  password: string;
}

export const ROLE_LANDING_ROUTE: Record<Role, string> = {
  ADMIN: '/app/admin',
  CLIENTE: '/app/cliente',
  MESERO: '/app/mesero',
  PRODUCCION: '/app/produccion',
  CAJERO: '/app/cajero'
};

export const MOCK_USERS: User[] = [
  {
    id: 'u-1',
    fullName: 'Ana Admin',
    email: 'admin@altoro.local',
    role: 'ADMIN',
    status: 'ACTIVE',
    password: 'Admin123*',
    createdAt: '2026-01-01'
  },
  {
    id: 'u-2',
    fullName: 'Carlos Cliente',
    email: 'cliente@altoro.local',
    role: 'CLIENTE',
    status: 'ACTIVE',
    password: 'Cliente123*',
    createdAt: '2026-01-02'
  },
  {
    id: 'u-3',
    fullName: 'Marta Mesera',
    email: 'mesero@altoro.local',
    role: 'MESERO',
    status: 'ACTIVE',
    password: 'Mesero123*',
    createdAt: '2026-01-03'
  },
  {
    id: 'u-4',
    fullName: 'Paco Cocina',
    email: 'produccion@altoro.local',
    role: 'PRODUCCION',
    status: 'ACTIVE',
    password: 'Prod123*',
    createdAt: '2026-01-03'
  },
  {
    id: 'u-5',
    fullName: 'Cami Caja',
    email: 'cajero@altoro.local',
    role: 'CAJERO',
    status: 'ACTIVE',
    password: 'Cajero123*',
    createdAt: '2026-01-04'
  }
];