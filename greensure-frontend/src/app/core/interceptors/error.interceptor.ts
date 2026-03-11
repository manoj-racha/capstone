import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
    const router = inject(Router);

    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {
            // If unauthorized, clear session and redirect to login
            if (error.status === 401) {
                localStorage.clear();
                router.navigate(['/login']);
            }

            // We could also show a global toast/snackbar here for 500s or 400s

            return throwError(() => error);
        })
    );
};
