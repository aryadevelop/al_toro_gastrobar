import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, finalize, map, of, throwError } from 'rxjs';

import { API_PATHS } from '../config/api-paths';
import { ROLE_LANDING_ROUTE } from '../config/role-routes';
import { StorageService } from './storage.service';
import { Role, User } from '../models/domain.models';
import { AuthResponse, LoginCredentials, RegisterRequest } from '../models/auth.models';

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
    return this.http.post<AuthResponse>(API_PATHS.auth.login, credentials).pipe(
      map((response) => this.applyAuthResponse(response))
    );
  }

  register(data: RegisterRequest): Observable<User> {
    return this.http.post<AuthResponse>(API_PATHS.auth.register, data).pipe(
      map((response) => this.applyAuthResponse(response))
    );
  }

  bootstrapSession(): Observable<User | null> {
    const token = this.storageService.getAccessToken();

    if (!token) {
      this.clearSession();
      return of(null);
    }

    return this.http.get<User>(API_PATHS.auth.me).pipe(
      map((user) => {
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
      .post<AuthResponse>(API_PATHS.auth.refresh, { refreshToken })
      .pipe(
        map((response) => {
          this.applyAuthResponse(response);
          return response.accessToken;
        })
      );
  }

  updateProfile(
    payload: Partial<Pick<User, 'fullName' | 'phone' | 'avatarUrl'>>
  ): Observable<User> {
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

  private applyAuthResponse(response: AuthResponse): User {
    this.storageService.setAccessToken(response.accessToken);
    this.storageService.setRefreshToken(response.refreshToken);
    this.storageService.setSessionUser(response.user);
    this.currentUserState.set(response.user);
    return response.user;
  }

  private clearSession(): void {
    this.currentUserState.set(null);
    this.storageService.clearAuth();
  }
}