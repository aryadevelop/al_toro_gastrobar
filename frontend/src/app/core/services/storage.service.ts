import { Injectable } from '@angular/core';
import { User } from '../models/domain.models';

const SESSION_USER_KEY = 'al_toro_session_user';

@Injectable({ providedIn: 'root' })
export class StorageService {
  getSessionUser(): User | null {
    const rawValue = localStorage.getItem(SESSION_USER_KEY);
    if (!rawValue) {
      return null;
    }

    try {
      return JSON.parse(rawValue) as User;
    } catch {
      localStorage.removeItem(SESSION_USER_KEY);
      return null;
    }
  }

  setSessionUser(user: User): void {
    localStorage.setItem(SESSION_USER_KEY, JSON.stringify(user));
  }

  clearSessionUser(): void {
    localStorage.removeItem(SESSION_USER_KEY);
  }
}