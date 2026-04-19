import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, finalize, map, of, throwError } from 'rxjs';

import { API_PATHS } from '../config/api-paths';
import { ROLE_LANDING_ROUTE } from '../config/role-routes';
import { StorageService } from './storage.service';
import { Role, User } from '../models/domain.models';
import { AuthResponse, BackendAuthUser, LoginCredentials, RegisterRequest, UpdateProfileRequest } from '../models/auth.models';

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
    return this.http.post<AuthApiResponse>(API_PATHS.auth.register, data).pipe(
      map((response) => this.applyAuthResponse(response))
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
    return this.http.patch<User>(API_PATHS.users.me, payload).pipe(
      map((user) => {
        this.currentUserState.set(user);
        this.storageService.setSessionUser(user);
        return user;
      })
    );
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