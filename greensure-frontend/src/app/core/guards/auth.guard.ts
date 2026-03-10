import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

/**
 * authGuard — Checks if user is logged in (JWT token exists).
 * If NOT logged in → redirects to /login.
 * Apply this to all protected routes.
 */
export const authGuard: CanActivateFn = (route, state) => {
    const router = inject(Router);
    const token = localStorage.getItem('token');

    if (token) {
        return true;
    }

    // Not logged in → go to login page
    router.navigate(['/login']);
    return false;
};
