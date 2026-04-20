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

export interface UpdateProfileRequest {
    fullName: string;
    email: string;
    phone: string;
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
    role: string;
    roles?: string[];
    status: string;
    createdAt: string;
}