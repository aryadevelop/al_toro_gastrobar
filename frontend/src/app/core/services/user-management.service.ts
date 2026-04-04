import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { MOCK_USERS } from '../mocks/auth.mock';
import { User } from '../models/domain.models';
import { MockApiService } from './mock-api.service';

@Injectable({ providedIn: 'root' })
export class UserManagementService {
  constructor(private readonly mockApiService: MockApiService) {}

  listAllUsers(): Observable<User[]> {
    return this.mockApiService.respond([...MOCK_USERS]);
  }

  listClientes(): Observable<User[]> {
    return this.listAllUsers().pipe(map((users) => users.filter((item) => item.role === 'CLIENTE')));
  }

  listStaff(): Observable<User[]> {
    return this.listAllUsers().pipe(map((users) => users.filter((item) => item.role !== 'CLIENTE')));
  }
}