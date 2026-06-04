import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MOCK_ADMIN_DASHBOARD, MOCK_DASHBOARD_METRICS } from '../mocks/restaurant.mock';
import { AdminDashboardData, DashboardMetric } from '../models/domain.models';
import { MockApiService } from './mock-api.service';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private readonly mockApiService: MockApiService) {}

  getMetrics(): Observable<DashboardMetric[]> {
    return this.mockApiService.respond([...MOCK_DASHBOARD_METRICS], 300);
  }

  getAdminDashboard(): Observable<AdminDashboardData> {
    return this.mockApiService.respond({ ...MOCK_ADMIN_DASHBOARD }, 400);
  }
}