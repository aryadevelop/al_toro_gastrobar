// src/app/core/models/auth.models.ts
import { User } from './domain.models';

export interface LoginCredentials {
    email: string;
    password: string;
    forceSessionOverride?: boolean;
}

export interface RegisterRequest {
    fullName: string;
    email: string;
    phone: string;
    password: string;
}

export interface BackendRegisterRequest {
    email: string;
    nombre: string;
    telefono: string;
    password: string;
    passwordConfirmation: string;
    aceptaTerminos: boolean;
    fechaNacimiento?: string;
}

export interface BackendRegisterResponse {
    success: boolean;
    message: string;
    user: {
        id: string;
        nombre: string;
        email: string;
        telefono: string;
        role: string;
    };
}

export interface UpdateProfileRequest {
    fullName: string;
    email: string;
    phone: string;
<<<<<<< HEAD
    aceptaTerminos?: boolean;
=======
    address?: string;
>>>>>>> 76fdc2a (fix(modificarCliente) corregir modificar campos cliente)
    currentPassword?: string;
    newPassword?: string;
    confirmNewPassword?: string;
}

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    tokenType?: 'Bearer';
    expiresIn?: number;
    user: User;
}

export interface BackendAuthUser {
    id: string;
    nombre: string;
    email: string;
    telefono?: string;
    role: string;
    roles?: string[];
    status: string;
    createdAt: string;
}

export interface BackendClienteData {
    id: number;
    nombre: string;
    email: string;
    telefono: string;
    direccion?: string;
    puntosActuales: number;
    puntosAcumulados: number;
    aceptaTerminos: boolean;
}

export interface BackendUpdateClienteResponse {
    success: boolean;
    message: string;
    cliente: BackendClienteData;
}

export interface BackendChangePasswordResponse {
    success: boolean;
    message: string;
}