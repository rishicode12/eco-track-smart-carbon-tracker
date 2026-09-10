import { HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../../features/auth/auth.service';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuthEndpoint =
    req.url.includes('/api/auth/login') ||
    req.url.includes('/api/auth/register') ||
    req.url.includes('/api/auth/refresh') ||
    req.url.includes('/api/auth/logout') ||
    req.url.includes('/api/users/login') ||
    req.url.includes('/api/users/register');

  const token = authService.getToken();

  // Clone request to ensure withCredentials is true (for HttpOnly cookies)
  // and attach Bearer token if present and not an unauthenticated auth endpoint
  let authReq = req.clone({
    withCredentials: true,
    ...(token && !isAuthEndpoint
      ? {
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        }
      : {}),
  });

  return next(authReq).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isAuthEndpoint) {
        return handle401Unauthorized(authReq, next, authService, router);
      }
      return throwError(() => error);
    })
  );
};

function handle401Unauthorized(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  router: Router
) {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return authService.refreshToken().pipe(
      switchMap((newToken: string) => {
        isRefreshing = false;
        refreshTokenSubject.next(newToken);

        return next(
          req.clone({
            setHeaders: {
              Authorization: `Bearer ${newToken}`,
            },
            withCredentials: true,
          })
        );
      }),
      catchError((refreshError: unknown) => {
        isRefreshing = false;
        refreshTokenSubject.next(null);

        // Refresh failed (expired or invalid token) -> Log out & redirect
        authService.logout();
        router.navigate(['/auth/login']);

        return throwError(() => refreshError);
      })
    );
  } else {
    // Another request triggered refresh -> Pause and wait for new token to emit
    return refreshTokenSubject.pipe(
      filter((token): token is string => token !== null),
      take(1),
      switchMap((newToken: string) => {
        return next(
          req.clone({
            setHeaders: {
              Authorization: `Bearer ${newToken}`,
            },
            withCredentials: true,
          })
        );
      })
    );
  }
}