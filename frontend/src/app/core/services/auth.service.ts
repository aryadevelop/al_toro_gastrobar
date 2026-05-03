import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, finalize, map, of, switchMap, throwError } from 'rxjs';

import { API_PATHS } from '../config/api-paths';
import { ROLE_LANDING_ROUTE } from '../config/role-routes';
import { StorageService } from './storage.service';
import { Role, User } from '../models/domain.models';
import {
  AuthResponse,
  BackendAuthUser,
  BackendRegisterRequest,
  BackendRegisterResponse,
  LoginCredentials,
  RegisterRequest,
  UpdateProfileRequest,
} from '../models/auth.models';

type AuthApiResponse = Omit<AuthResponse, 'user'> & { user: BackendAuthUser | User };

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storageService = inject(StorageService);

  private readonly currentUserState = signal<User | null>(
    this.storageService.getSessionUser()
  );

  readonly currentUser = computed(() => this.currentUserState());

  readonly isAuthenticated = computed(() =>
    Boolean(this.storageService.getAccessToken() && this.currentUserState())
  );

  login(credentials: LoginCredentials): Observable<User> {
    return this.http.post<AuthApiResponse>(API_PATHS.auth.login, credentials).pipe(
      map((response) => this.applyAuthResponse(response))
    );
  }

  register(data: RegisterRequest): Observable<User> {
    const registerPayload: BackendRegisterRequest = {
      email: data.email.trim(),
      nombre: data.fullName.trim(),
      telefono: data.phone.trim(),
      password: data.password,
      passwordConfirmation: data.password,
      aceptaTerminos: true,
    };

    return this.http.post<BackendRegisterResponse>(API_PATHS.auth.register, registerPayload).pipe(
      switchMap(() => this.login({ email: data.email.trim(), password: data.password }))
    );
  }

  bootstrapSession(): Observable<User | null> {
    const token = this.storageService.getAccessToken();

    if (!token) {
      this.clearSession();
      return of(null);
    }

    return this.http.get<BackendAuthUser>(API_PATHS.auth.me).pipe(
      map((backendUser) => {
        const user = this.toFrontendUser(backendUser);
        this.currentUserState.set(user);
        this.storageService.setSessionUser(user);
        return user;
      }),
      catchError(() => {
        this.clearSession();
        return of(null);
      })
    );
  }

  refreshToken(): Observable<string> {
    const refreshToken = this.storageService.getRefreshToken();

    if (!refreshToken) {
      return throwError(() => new Error('No hay refresh token disponible.'));
    }

    return this.http
      .post<AuthApiResponse>(API_PATHS.auth.refresh, { refreshToken })
      .pipe(
        map((response) => {
          this.applyAuthResponse(response);
          return response.accessToken;
        })
      );
  }

  updateProfile(payload: UpdateProfileRequest): Observable<User> {
    const backendPayload = {
      nombre: payload.fullName,
      email: payload.email,
      telefono: payload.phone,
      direccion: payload.address ?? '',
      aceptaTerminos: true,
    };

    return this.http
      .put<ApiEnvelope<BackendUpdateClienteResponse>>(API_PATHS.clientes.updateMe, backendPayload)
      .pipe(
        map((envelope) => {
          const cliente = envelope.data.cliente;
          const currentUser = this.currentUserState();

          const updatedUser: User = {
            id: currentUser?.id ?? String(cliente.id),
            fullName: cliente.nombre,
            email: cliente.email,
            phone: cliente.telefono,
            role: currentUser?.role ?? 'CLIENTE',
            status: currentUser?.status ?? 'ACTIVE',
            createdAt: currentUser?.createdAt ?? new Date().toISOString(),
          };

          this.currentUserState.set(updatedUser);
          this.storageService.setSessionUser(updatedUser);
          return updatedUser;
        })
      );
  }

  changePassword(currentPassword: string, newPassword: string, confirmation: string): Observable<string> {
    const backendPayload = {
      'contraseñaActual': currentPassword,
      'nuevaContraseña': newPassword,
      'confirmacion': confirmation,
    };

    return this.http
      .post<ApiEnvelope<BackendChangePasswordResponse>>(API_PATHS.clientes.changePassword, backendPayload)
      .pipe(map((envelope) => envelope.data.message));
  }

  getMyProfile(): Observable<BackendClienteData> {
    return this.http
      .get<ApiEnvelope<BackendClienteData>>(API_PATHS.clientes.me)
      .pipe(map((envelope) => envelope.data));
  }

  logout(): Observable<void> {
    return this.http.post<void>(API_PATHS.auth.logout, {}).pipe(
      finalize(() => this.clearSession())
    );
  }

  forceLogout(): void {
    this.clearSession();
  }

  getAccessToken(): string | null {
    return this.storageService.getAccessToken();
  }

  getLandingRouteForRole(role: Role): string {
    return ROLE_LANDING_ROUTE[role] ?? '/app/dashboard';
  }

  private applyAuthResponse(response: AuthApiResponse): User {
    const user = this.toFrontendUser(response.user);

    this.storageService.setAccessToken(response.accessToken);
    this.storageService.setRefreshToken(response.refreshToken);
    this.storageService.setSessionUser(user);
    this.currentUserState.set(user);
    return user;
  }

  private clearSession(): void {
    const stateUser = this.currentUserState();
    const persistedUser = this.storageService.getSessionUser();
    const tokenUserId = this.extractUserIdFromToken(this.storageService.getAccessToken());
    const userId = stateUser?.id ?? persistedUser?.id ?? tokenUserId ?? undefined;

    this.currentUserState.set(null);
    this.storageService.clearAuth(userId);
  }

  private extractUserIdFromToken(token: string | null): string | null {
    if (!token || !token.includes('.')) {
      return null;
    }

    try {
      const rawPayload = token.split('.')[1] ?? '';
      const normalizedPayload = rawPayload.replace(/-/g, '+').replace(/_/g, '/');
      const paddedPayload = normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, '=');
      const payload = JSON.parse(atob(paddedPayload)) as { sub?: string };
      return payload.sub ?? null;
    } catch {
      return null;
    }
  }

  private toFrontendUser(user: BackendAuthUser | User): User {
    if ('nombre' in user) {
      const backendUser = user as BackendAuthUser;
      const roleSource =
        backendUser.role ??
        backendUser.roles?.find((currentRole: string | null | undefined): currentRole is string =>
          typeof currentRole === 'string' && currentRole.trim().length > 0
        ) ??
        'CLIENTE';

      return {
        id: backendUser.id,
        fullName: backendUser.nombre,
        email: backendUser.email,
        role: this.normalizeRole(roleSource),
        status: this.normalizeStatus(backendUser.status),
        createdAt: backendUser.createdAt,
      };
    }

    return user;
  }

  private normalizeRole(role: string | null | undefined): Role {
    const normalized = String(role ?? '').toUpperCase();

    if (normalized === 'ADM' || normalized === 'ADMIN') {
      return 'ADMIN';
    }

    if (normalized === 'COCINERO' || normalized === 'BARTENDER' || normalized === 'PRODUCCION') {
      return 'PRODUCCION';
    }

    if (normalized === 'MESERO' || normalized === 'CLIENTE' || normalized === 'CAJERO') {
      return normalized;
    }

    return 'CLIENTE';
  }

  private normalizeStatus(status: string): User['status'] {
    const normalized = status.toUpperCase();
    return normalized === 'INACTIVE' || normalized === 'INACTIVO' ? 'INACTIVE' : 'ACTIVE';
  }
}