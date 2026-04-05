// src/app/core/services/storage.service.ts
import { Injectable } from '@angular/core';
import { User } from '../models/domain.models';

@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly SESSION_USER_KEY = 'session_user';
  private readonly ACCESS_TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';

  getSessionUser(): User | null {
    const raw = localStorage.getItem(this.SESSION_USER_KEY);
    return raw ? JSON.parse(raw) as User : null;
  }

  setSessionUser(user: User | null): void {
    if (!user) {
      localStorage.removeItem(this.SESSION_USER_KEY);
      return;
    }
    localStorage.setItem(this.SESSION_USER_KEY, JSON.stringify(user));
  }

  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  setAccessToken(token: string): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, token);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  setRefreshToken(token: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }

  clearAuth(): void {
    localStorage.removeItem(this.SESSION_USER_KEY);
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }
}