import {
    HttpErrorResponse,
    HttpEvent,
    HttpHandler,
    HttpInterceptor,
    HttpRequest,
    HttpResponse,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, delay, throwError } from 'rxjs';
import { API_PATHS } from '../config/api-paths';

interface MockUser {
    id: string;
    fullName: string;
    email: string;
    phone: string;
    role: 'ADMIN' | 'CLIENTE' | 'MESERO' | 'PRODUCCION' | 'CAJERO';
    status: 'ACTIVE' | 'INACTIVE';
    createdAt: string;
    password: string;
    passwordUpdatedAt?: string;
}

interface LoginBody {
    email: string;
    password: string;
    forceSessionOverride?: boolean;
}

interface RegisterBody {
    fullName: string;
    email: string;
    phone: string;
    password: string;
}

interface UpdateProfileBody {
    fullName: string;
    email: string;
    phone: string;
    currentPassword?: string;
    newPassword?: string;
    confirmNewPassword?: string;
}

@Injectable()
export class FakeBackendInterceptor implements HttpInterceptor {
    private mockUsers: MockUser[] = [
        {
            id: 'u-1', fullName: 'Ana Admin', email: 'admin@altoro.local',
            phone: '3000000000', role: 'ADMIN', status: 'ACTIVE',
            createdAt: '2026-01-01', password: 'Admin123*'
        },
        {
            id: 'u-2', fullName: 'Carlos Cliente', email: 'cliente@altoro.local',
            phone: '3011111111', role: 'CLIENTE', status: 'ACTIVE',
            createdAt: '2026-01-02', password: 'Cliente123*'
        },
        {
            id: 'u-3', fullName: 'Marta Mesera', email: 'mesero@altoro.local',
            phone: '3022222222', role: 'MESERO', status: 'ACTIVE',
            createdAt: '2026-01-03', password: 'Mesero123*'
        },
        {
            id: 'u-4', fullName: 'Paco Cocina', email: 'produccion@altoro.local',
            phone: '3033333333', role: 'PRODUCCION', status: 'ACTIVE',
            createdAt: '2026-01-03', password: 'Prod123*'
        },
        {
            id: 'u-5', fullName: 'Cami Caja', email: 'cajero@altoro.local',
            phone: '3044444444', role: 'CAJERO', status: 'ACTIVE',
            createdAt: '2026-01-04', password: 'Cajero123*'
        },
        {
            id: 'u-6', fullName: 'Luis Suspendido', email: 'suspendido@altoro.local',
            phone: '3055555555', role: 'MESERO', status: 'INACTIVE',
            createdAt: '2026-01-05', password: 'Susp123*'
        },
        {
            // Criterio 1: Contraseña expirada (fecha de creación antigua simulada)
            id: 'u-7', fullName: 'Laura Expirada', email: 'expirado@altoro.local',
            phone: '3066666666', role: 'CLIENTE', status: 'ACTIVE',
            createdAt: '2026-01-06', password: 'Exp123*', passwordUpdatedAt: '2025-01-01'
        }
    ];

    intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
        if (req.method === 'POST' && req.url === API_PATHS.auth.login) {
            return this.handleLogin(req);
        }
        if (req.method === 'POST' && req.url === API_PATHS.auth.register) {
            return this.handleRegister(req);
        }
        if (req.method === 'POST' && req.url === API_PATHS.auth.refresh) {
            return this.handleRefresh(req);
        }
        if (req.method === 'GET' && req.url === API_PATHS.auth.me) {
            return this.handleMe(req);
        }
        if (req.method === 'PATCH' && req.url === API_PATHS.users.me) {
            return this.handleUpdateProfile(req);
        }
        if (req.method === 'POST' && req.url === API_PATHS.auth.logout) {
            // Extraer el ID de usuario del token para quitar su sesión activa
            const authHeader = req.headers.get('Authorization') || '';
            const match = authHeader.match(/mock-token-(u-\d+)/);
            if (match && match[1]) {
                const activeSessions = JSON.parse(localStorage.getItem('MOCK_ACTIVE_SESSIONS') || '[]');
                const filtered = activeSessions.filter((id: string) => id !== match[1]);
                localStorage.setItem('MOCK_ACTIVE_SESSIONS', JSON.stringify(filtered));
            }
            return of(new HttpResponse({ status: 200, body: null })).pipe(delay(150));
        }
        return next.handle(req);
    }

    private handleRefresh(req: HttpRequest<unknown>): Observable<HttpEvent<unknown>> {
        const body = (req.body as { refreshToken?: string } | null) ?? {};
        const refreshToken = String(body.refreshToken ?? '');
        const refreshMatch = refreshToken.match(/mock-refresh-(u-\d+)/);
        const user = this.mockUsers.find((candidate) => candidate.id === refreshMatch?.[1]) ?? this.mockUsers[0];

        return of(new HttpResponse({
            status: 200,
            body: {
                accessToken: `mock-token-${user.id}`,
                refreshToken: `mock-refresh-${user.id}`,
                tokenType: 'Bearer',
                user: this.toSafeUser(user)
            }
        })).pipe(delay(250));
    }

    private handleMe(req: HttpRequest<unknown>): Observable<HttpEvent<unknown>> {
        const user = this.resolveUserFromRequest(req);

        if (!user) {
            return throwError(() => new HttpErrorResponse({
                status: 401,
                statusText: 'Unauthorized',
                error: { message: 'Sesión inválida o expirada' }
            })).pipe(delay(250));
        }

        return of(new HttpResponse({
            status: 200,
            body: this.toSafeUser(user)
        })).pipe(delay(250));
    }

    private handleUpdateProfile(req: HttpRequest<unknown>): Observable<HttpEvent<unknown>> {
        const user = this.resolveUserFromRequest(req);

        if (!user) {
            return throwError(() => new HttpErrorResponse({
                status: 401,
                statusText: 'Unauthorized',
                error: { message: 'Debes iniciar sesión para actualizar tus datos' }
            })).pipe(delay(300));
        }

        const body = req.body as UpdateProfileBody;
        const fullName = String(body.fullName ?? '').trim().replace(/\s+/g, ' ');
        const email = String(body.email ?? '').trim().toLowerCase();
        const phone = String(body.phone ?? '').trim().replace(/\D/g, '');

        if (!fullName || !email || !phone) {
            return throwError(() => new HttpErrorResponse({
                status: 400,
                statusText: 'Bad Request',
                error: { code: 'VALIDATION_ERROR', message: 'Hay campos obligatorios incompletos' }
            })).pipe(delay(300));
        }

        if (!/^\d{10}$/.test(phone)) {
            return throwError(() => new HttpErrorResponse({
                status: 400,
                statusText: 'Bad Request',
                error: { code: 'INVALID_PHONE_FORMAT', message: 'Ingresa un número de teléfono válido de 10 dígitos' }
            })).pipe(delay(300));
        }

        if (this.mockUsers.some((candidate) => candidate.id !== user.id && candidate.email.toLowerCase() === email)) {
            return throwError(() => new HttpErrorResponse({
                status: 409,
                statusText: 'Conflict',
                error: {
                    code: 'DUPLICATE_EMAIL',
                    message: 'Este correo electrónico ya está en uso por otro usuario. Por favor utiliza otro'
                }
            })).pipe(delay(350));
        }

        if (this.mockUsers.some((candidate) => candidate.id !== user.id && candidate.phone.replace(/\D/g, '') === phone)) {
            return throwError(() => new HttpErrorResponse({
                status: 409,
                statusText: 'Conflict',
                error: {
                    code: 'DUPLICATE_PHONE',
                    message: 'Este número de teléfono ya está en uso por otro usuario. Por favor utiliza otro'
                }
            })).pipe(delay(350));
        }

        const wantsPasswordChange = Boolean(body.currentPassword || body.newPassword || body.confirmNewPassword);
        if (wantsPasswordChange) {
            const currentPassword = String(body.currentPassword ?? '').trim();
            const newPassword = String(body.newPassword ?? '').trim();
            const confirmNewPassword = String(body.confirmNewPassword ?? '').trim();

            if (!currentPassword || !newPassword || !confirmNewPassword) {
                return throwError(() => new HttpErrorResponse({
                    status: 400,
                    statusText: 'Bad Request',
                    error: {
                        code: 'PASSWORD_FIELDS_REQUIRED',
                        message: 'Completa todos los campos para cambiar contraseña'
                    }
                })).pipe(delay(300));
            }

            if (currentPassword !== user.password) {
                return throwError(() => new HttpErrorResponse({
                    status: 400,
                    statusText: 'Bad Request',
                    error: {
                        code: 'INVALID_CURRENT_PASSWORD',
                        message: 'La contraseña actual no es correcta'
                    }
                })).pipe(delay(300));
            }

            if (newPassword !== confirmNewPassword) {
                return throwError(() => new HttpErrorResponse({
                    status: 400,
                    statusText: 'Bad Request',
                    error: {
                        code: 'PASSWORD_MISMATCH',
                        message: 'La nueva contraseña y su confirmación no coinciden'
                    }
                })).pipe(delay(300));
            }

            if (!this.isValidPassword(newPassword)) {
                return throwError(() => new HttpErrorResponse({
                    status: 400,
                    statusText: 'Bad Request',
                    error: {
                        code: 'INVALID_NEW_PASSWORD',
                        message: 'La nueva contraseña no cumple con la complejidad mínima'
                    }
                })).pipe(delay(300));
            }

            user.password = newPassword;
            user.passwordUpdatedAt = new Date().toISOString();
        }

        user.fullName = fullName;
        user.email = email;
        user.phone = phone;

        return of(new HttpResponse({
            status: 200,
            body: this.toSafeUser(user)
        })).pipe(delay(350));
    }

    private handleLogin(req: HttpRequest<unknown>): Observable<HttpEvent<unknown>> {
        const { email, password, forceSessionOverride } = req.body as LoginBody;
        const normalizedEmail = String(email ?? '').trim().toLowerCase();
        const normalizedPassword = String(password ?? '');

        // Criterio 3: Inyección SQL o caracteres maliciosos (Sanitización básica y rechazo genérico)
        const hasMaliciousChars = /['";=\\]|(--)/.test(normalizedEmail) || /['";=\\]|(--)/.test(normalizedPassword);
        if (hasMaliciousChars) {
            // Rechazo y mensaje genérico para no dar pistas al atacante
            return throwError(() => new HttpErrorResponse({
                status: 401, statusText: 'Unauthorized',
                error: { message: 'Credenciales incorrectas, por favor verifica tu correo y/o contraseña' }
            })).pipe(delay(400));
        }

        const user = this.mockUsers.find(u => u.email.toLowerCase() === normalizedEmail);

        if (!user || user.password !== normalizedPassword) {
            return throwError(() => new HttpErrorResponse({
                status: 401, statusText: 'Unauthorized',
                error: { message: 'Credenciales incorrectas, por favor verifica tu correo y/o contraseña' }
            })).pipe(delay(400));
        }
        
        if (user.status !== 'ACTIVE') {
            return throwError(() => new HttpErrorResponse({
                status: 403, statusText: 'Forbidden',
                error: { message: 'Tu cuenta se encuentra suspendida. Por favor contacta al administrador' }
            })).pipe(delay(400));
        }

        // Criterio 1: Contraseña expirada (Simulamos expiración si passwordUpdatedAt existe y es muy antiguo)
        if ('passwordUpdatedAt' in user) {
            const updatedAt = new Date(user.passwordUpdatedAt as string);
            const daysSinceUpdate = (new Date().getTime() - updatedAt.getTime()) / (1000 * 3600 * 24);
            if (daysSinceUpdate > 90) {
                return throwError(() => new HttpErrorResponse({
                    status: 403, statusText: 'Forbidden',
                    error: { 
                        code: 'PASSWORD_EXPIRED', 
                        message: 'Tu contraseña ha expirado. Por favor crea una nueva' 
                    }
                })).pipe(delay(400));
            }
        }

        // Criterio 2: Sesión activa en otro dispositivo (leído desde localStorage para persistencia mock)
        const activeSessions = JSON.parse(localStorage.getItem('MOCK_ACTIVE_SESSIONS') || '[]');
        const hasSessionActive = activeSessions.includes(user.id);

        if (hasSessionActive && !forceSessionOverride) {
            return throwError(() => new HttpErrorResponse({
                status: 409, statusText: 'Conflict',
                error: { 
                    code: 'ACTIVE_SESSION', 
                    message: 'Ya tienes una sesión activa en otro dispositivo. ¿Deseas cerrar la otra sesión y continuar?' 
                }
            })).pipe(delay(400));
        }

        // Registrar la sesión activa
        if (!hasSessionActive) {
            activeSessions.push(user.id);
            localStorage.setItem('MOCK_ACTIVE_SESSIONS', JSON.stringify(activeSessions));
        }

        return of(new HttpResponse({
            status: 200,
            body: {
                accessToken: `mock-token-${user.id}`,
                refreshToken: `mock-refresh-${user.id}`,
                tokenType: 'Bearer',
                user: this.toSafeUser(user)
            }
        })).pipe(delay(400));
    }

    private handleRegister(req: HttpRequest<unknown>): Observable<HttpEvent<unknown>> {
        const body = req.body as RegisterBody;
        const email = String(body.email ?? '').trim().toLowerCase();

        if (this.mockUsers.some(u => u.email.toLowerCase() === email)) {
            return throwError(() => new HttpErrorResponse({
                status: 409, statusText: 'Conflict',
                error: { message: 'Ya existe una cuenta registrada con este correo' }
            })).pipe(delay(400));
        }

        const newUser: MockUser = {
            id: `u-${Date.now()}`,
            fullName: String(body.fullName ?? '').trim().replace(/\s+/g, ' '),
            email,
            phone: String(body.phone ?? '').trim().replace(/\D/g, ''),
            role: 'CLIENTE',
            status: 'ACTIVE',
            createdAt: new Date().toISOString(),
            password: String(body.password ?? '')
        };

        this.mockUsers.push(newUser);

        return of(new HttpResponse({
            status: 201,
            body: {
                accessToken: `mock-token-${newUser.id}`,
                refreshToken: `mock-refresh-${newUser.id}`,
                tokenType: 'Bearer',
                user: this.toSafeUser(newUser)
            }
        })).pipe(delay(400));
    }

    private resolveUserFromRequest(req: HttpRequest<unknown>): MockUser | null {
        const userId = this.getUserIdFromRequest(req);

        if (!userId) {
            return null;
        }

        return this.mockUsers.find((candidate) => candidate.id === userId) ?? null;
    }

    private getUserIdFromRequest(req: HttpRequest<unknown>): string | null {
        const authHeader = req.headers.get('Authorization') || '';
        const match = authHeader.match(/mock-token-(u-\d+)/);
        return match?.[1] ?? null;
    }

    private toSafeUser(user: MockUser): Omit<MockUser, 'password' | 'passwordUpdatedAt'> {
        const { password: _password, passwordUpdatedAt: _passwordUpdatedAt, ...safeUser } = user;
        return safeUser;
    }

    private isValidPassword(value: string): boolean {
        const hasUppercase = /[A-Z]/.test(value);
        const hasLowercase = /[a-z]/.test(value);
        const hasNumber = /\d/.test(value);
        const hasSpecialChar = /[^A-Za-z0-9]/.test(value);

        return hasUppercase && hasLowercase && hasNumber && hasSpecialChar && value.length >= 8;
    }
}