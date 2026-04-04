import { Role } from './domain.models';

export interface MenuItem {
  label: string;
  path: string;
  icon: string;
  roles: Role[];
}