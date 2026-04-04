import { computed, Injectable, signal } from '@angular/core';
import { Observable, delay, map, of, throwError } from 'rxjs';
import { LoginCredentials, MOCK_USERS, ROLE_LANDING_ROUTE } from '../mocks/auth.mock';
import { Role, User } from '../models/domain.models';
import { StorageService } from './storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly currentUserState = signal<User | null>(this.storageService.getSessionUser());
  readonly currentUser = computed(() => this.currentUserState());
  readonly isAuthenticated = computed(() => Boolean(this.currentUserState()));

  constructor(private readonly storageService: StorageService) {}

  login(credentials: LoginCredentials): Observable<User> {
    const foundUser = MOCK_USERS.find(
      (user) =>
        user.email.toLowerCase() === credentials.email.toLowerCase() &&
        user.password === credentials.password &&
        user.status === 'ACTIVE'
    );

    if (!foundUser) {
      return throwError(() => new Error('Credenciales invlidas.'));
    }

    return of(foundUser).pipe(
      delay(450),
      map((user) => {
        this.currentUserState.set(user);
        this.storageService.setSessionUser(user);
        return user;
      })
    );
  }

  register(data: Pick<User, 'fullName' | 'email' | 'phone'> & { password: string }): Observable<User> {
    const newUser: User = {
      id: `u-${Date.now()}`,
      fullName: data.fullName,
      email: data.email,
      phone: data.phone,
      role: 'CLIENTE',
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
      password: data.password
    };

    return of(newUser).pipe(
      delay(500),
      map((user) => {
        this.currentUserState.set(user);
        this.storageService.setSessionUser(user);
        return user;
      })
    );
  }

  updateProfile(payload: Partial<Pick<User, 'fullName' | 'phone' | 'avatarUrl'>>): Observable<User> {
    const current = this.currentUserState();

    if (!current) {
      return throwError(() => new Error('No hay sesin activa.'));
    }

    const updatedUser: User = {
      ...current,
      ...payload
    };

    return of(updatedUser).pipe(
      delay(300),
      map((user) => {
        this.currentUserState.set(user);
        this.storageService.setSessionUser(user);
        return user;
      })
    );
  }

  logout(): void {
    this.currentUserState.set(null);
    this.storageService.clearSessionUser();
  }

  getLandingRouteForRole(role: Role): string {
    return ROLE_LANDING_ROUTE[role] ?? '/app/dashboard';
  }
}