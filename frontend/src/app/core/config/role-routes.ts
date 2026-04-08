import { Role } from '../models/domain.models';

export const ROLE_LANDING_ROUTE: Record<Role, string> = {
  ADMIN: '/app/admin',
  CLIENTE: '/app/cliente',
  MESERO: '/app/mesero',
  PRODUCCION: '/app/produccion',
  CAJERO: '/app/cajero'
};