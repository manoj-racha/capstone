import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * roleGuard — Checks if the logged-in user's role matches the allowed roles
 * defined in the route's data.roles array.
 *
 * Usage: { path: 'admin/dashboard', canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } }
 */
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const allowedRoles: string[] = route.data?.['roles'] || [];

  if (allowedRoles.length === 0) {
    return true;
  }

  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }

  const userRole = auth.getRole();
  if (userRole && allowedRoles.includes(userRole)) {
    return true;
  }

  // Role mismatch → redirect to correct dashboard
  if (userRole === 'ADMIN') {
    return router.createUrlTree(['/admin/dashboard']);
  }
  if (userRole === 'AGENT') {
    return router.createUrlTree(['/agent/dashboard']);
  }
  return router.createUrlTree(['/user/dashboard']);
};
