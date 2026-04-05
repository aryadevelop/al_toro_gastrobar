import { ApplicationConfig } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { appRoutes } from './app.routes';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { environment } from '../environments/environment';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';
import { FakeBackendInterceptor } from './core/interceptors/fake-backend.interceptor';

const interceptorProviders = [
  ...(environment.useMockApi
    ? [{ provide: HTTP_INTERCEPTORS, useClass: FakeBackendInterceptor, multi: true }]
    : []),
  { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
];

export const appConfig: ApplicationConfig = {
  providers: [
    provideAnimations(),
    provideRouter(
      appRoutes,
      withInMemoryScrolling({
        scrollPositionRestoration: 'enabled',
        anchorScrolling: 'enabled'
      })
    ),
    provideHttpClient(withInterceptorsFromDi()),
    ...interceptorProviders
  ]
};