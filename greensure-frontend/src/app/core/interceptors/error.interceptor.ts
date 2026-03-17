import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
    const router = inject(Router);
    const toastService = inject(ToastService);

    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {
            // If unauthorized, clear session and redirect to login
            if (error.status === 401) {
                localStorage.clear();
                toastService.error('Session expired. Please sign in again.');
                router.navigate(['/login']);
            } else {
                const message = error.error?.error || error.error?.message || 'Something went wrong. Please try again.';
                toastService.error(message);
            }

            return throwError(() => error);
        })
    );
};
