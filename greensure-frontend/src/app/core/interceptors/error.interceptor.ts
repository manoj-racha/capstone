import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

let lastErrorToastKey = '';
let lastErrorToastAt = 0;

function shouldToastError(key: string): boolean {
    const now = Date.now();
    // Suppress duplicates for a short window to avoid toast storms.
    if (key === lastErrorToastKey && now - lastErrorToastAt < 2500) {
        return false;
    }
    lastErrorToastKey = key;
    lastErrorToastAt = now;
    return true;
}

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
    const router = inject(Router);
    const toastService = inject(ToastService);

    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {
            // If unauthorized, clear session and redirect to login
            if (error.status === 401) {
                localStorage.clear();
                if (shouldToastError('401:session-expired')) {
                    toastService.error('Session expired. Please sign in again.');
                }
                router.navigate(['/login']);
            } else {
                const message = error.error?.error || error.error?.message || 'Something went wrong. Please try again.';
                const key = `${error.status}:${req.method}:${req.url}`;
                if (shouldToastError(key)) {
                    toastService.error(message);
                }
            }

            return throwError(() => error);
        })
    );
};
