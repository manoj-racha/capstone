import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * authGuard — Checks if user is logged in (JWT token exists AND is not expired).
 * If NOT logged in → redirects to /login.
 * Apply this to all protected routes.
 */
export const authGuard: CanActivateFn = (route, state) => {
    const router = inject(Router);
    const authService = inject(AuthService);

    if (authService.isLoggedIn()) {
        return true;
    }

    // Not logged in or token expired → go to login page
    router.navigate(['/login']);
    return false;
};
