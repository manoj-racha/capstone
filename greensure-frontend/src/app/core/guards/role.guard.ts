import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

/**
 * roleGuard — Checks if the logged-in user's role matches the allowed roles
 * defined in the route's data.roles array.
 *
 * Usage in routes:
 *   { path: 'admin/dashboard', canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } }
 *
 * If role doesn't match → redirects to the user's correct dashboard.
 */
export const roleGuard: CanActivateFn = (route, state) => {
    const router = inject(Router);
    const userRole = localStorage.getItem('role');
    const allowedRoles: string[] = route.data?.['roles'] || [];

    // If no roles specified on the route, allow access
    if (allowedRoles.length === 0) {
        return true;
    }

    // Check if user's role is in the allowed list
    if (userRole && allowedRoles.includes(userRole)) {
        return true;
    }

    // Role mismatch → redirect to the user's own dashboard
    if (userRole === 'ADMIN') {
        router.navigate(['/admin/dashboard']);
    } else if (userRole === 'AGENT') {
        router.navigate(['/agent/dashboard']);
    } else {
        router.navigate(['/user/dashboard']);
    }

    return false;
};
