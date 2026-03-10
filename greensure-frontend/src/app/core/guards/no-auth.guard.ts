import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

/**
 * noAuthGuard — Prevents logged-in users from visiting auth pages
 * (login, register, forgot-password, reset-password).
 *
 * If already logged in → redirects to role-based dashboard.
 * If NOT logged in → allows access to the auth page.
 */
export const noAuthGuard: CanActivateFn = (route, state) => {
    const router = inject(Router);
    const token = localStorage.getItem('token');

    if (!token) {
        // Not logged in → let them access login/register pages
        return true;
    }

    // Already logged in → redirect to their dashboard
    const role = localStorage.getItem('role');

    if (role === 'ADMIN') {
        router.navigate(['/admin/dashboard']);
    } else if (role === 'AGENT') {
        router.navigate(['/agent/dashboard']);
    } else {
        router.navigate(['/user/dashboard']);
    }

    return false;
};
