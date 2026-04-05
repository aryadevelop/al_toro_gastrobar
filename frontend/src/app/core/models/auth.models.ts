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

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    tokenType?: 'Bearer';
    user: User;
}