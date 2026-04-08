// src/app/core/interceptors/auth.interceptor.ts
import {
    HttpErrorResponse,
    HttpEvent,
    HttpHandler,
    HttpInterceptor,
    HttpRequest,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { API_PATHS } from '../config/api-paths';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
    private isRefreshing = false;
    private refreshTokenSubject = new BehaviorSubject<string | null>(null);

    constructor(private readonly authService: AuthService) { }

    intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
        const isApiUrl = req.url.startsWith(environment.apiBaseUrl);
        const isAuthRoute = [
            API_PATHS.auth.login,
            API_PATHS.auth.register,
            API_PATHS.auth.refresh,
        ].some((url) => req.url.startsWith(url));

        let authReq = req;
        const token = this.authService.getAccessToken();

        if (token && isApiUrl && !isAuthRoute) {
            authReq = this.addAuthHeader(req, token);
        }

        return next.handle(authReq).pipe(
            catchError((error: HttpErrorResponse) => {
                if (error.status === 401 && isApiUrl && !isAuthRoute) {
                    return this.handle401Error(authReq, next);
                }

                return throwError(() => error);
            })
        );
    }

    private handle401Error(
        request: HttpRequest<unknown>,
        next: HttpHandler
    ): Observable<HttpEvent<unknown>> {
        if (!this.isRefreshing) {
            this.isRefreshing = true;
            this.refreshTokenSubject.next(null);

            return this.authService.refreshToken().pipe(
                switchMap((newAccessToken) => {
                    this.isRefreshing = false;
                    this.refreshTokenSubject.next(newAccessToken);
                    return next.handle(this.addAuthHeader(request, newAccessToken));
                }),
                catchError((error) => {
                    this.isRefreshing = false;
                    this.authService.forceLogout();
                    return throwError(() => error);
                })
            );
        }

        return this.refreshTokenSubject.pipe(
            filter((token): token is string => token !== null),
            take(1),
            switchMap((token) => next.handle(this.addAuthHeader(request, token)))
        );
    }

    private addAuthHeader(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
        return req.clone({
            setHeaders: {
                Authorization: `Bearer ${token}`,
            },
        });
    }
}