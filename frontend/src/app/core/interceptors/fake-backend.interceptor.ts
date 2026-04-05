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

@Injectable()
export class FakeBackendInterceptor implements HttpInterceptor {
    private mockUsers = [
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
            return of(new HttpResponse({
                status: 200,
                body: { accessToken: 'mock-renewed', refreshToken: 'mock-refresh', tokenType: 'Bearer', user: this.mockUsers[0] }
            })).pipe(delay(250));
        }
        if (req.method === 'GET' && req.url === API_PATHS.auth.me) {
            return of(new HttpResponse({ status: 200, body: this.mockUsers[0] })).pipe(delay(250));
        }
        if (req.method === 'PATCH' && req.url === API_PATHS.users.me) {
            return of(new HttpResponse({ status: 200, body: { ...this.mockUsers[0], ...(req.body as object) } })).pipe(delay(250));
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

    private handleLogin(req: HttpRequest<unknown>): Observable<HttpEvent<unknown>> {
        const { email, password, forceSessionOverride } = req.body as { email: string; password: string; forceSessionOverride?: boolean };

        // Criterio 3: Inyección SQL o caracteres maliciosos (Sanitización básica y rechazo genérico)
        const hasMaliciousChars = /['";=\\]|(--)/.test(email) || /['";=\\]|(--)/.test(password);
        if (hasMaliciousChars) {
            // Rechazo y mensaje genérico para no dar pistas al atacante
            return throwError(() => new HttpErrorResponse({
                status: 401, statusText: 'Unauthorized',
                error: { message: 'Credenciales incorrectas, por favor verifica tu correo y/o contraseña' }
            })).pipe(delay(400));
        }

        const user = this.mockUsers.find(u => u.email.toLowerCase() === email.toLowerCase());

        if (!user || user.password !== password) {
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

        const { password: _, ...safeUser } = user;
        return of(new HttpResponse({
            status: 200,
            body: { accessToken: `mock-token-${user.id}`, refreshToken: `mock-refresh-${user.id}`, tokenType: 'Bearer', user: safeUser }
        })).pipe(delay(400));
    }

    private handleRegister(req: HttpRequest<unknown>): Observable<HttpEvent<unknown>> {
        const body = req.body as { fullName: string; email: string; phone: string; password: string };
        if (this.mockUsers.some(u => u.email.toLowerCase() === body.email.toLowerCase())) {
            return throwError(() => new HttpErrorResponse({
                status: 409, statusText: 'Conflict',
                error: { message: 'Ya existe una cuenta registrada con este correo' }
            })).pipe(delay(400));
        }
        const newUser = {
            id: `u-${Date.now()}`, fullName: body.fullName, email: body.email,
            phone: body.phone, role: 'CLIENTE', status: 'ACTIVE',
            createdAt: new Date().toISOString(), password: body.password
        };
        this.mockUsers.push(newUser);
        const { password: _, ...safeUser } = newUser;
        return of(new HttpResponse({
            status: 201,
            body: { accessToken: `mock-token-${newUser.id}`, refreshToken: `mock-refresh-${newUser.id}`, tokenType: 'Bearer', user: safeUser }
        })).pipe(delay(400));
    }
}