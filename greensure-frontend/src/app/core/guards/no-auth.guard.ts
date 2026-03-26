import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * noAuthGuard — Prevents logged-in users from visiting auth pages.
 * If already logged in → redirects to role-based dashboard.
 */
export const noAuthGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    return true;
  }

  const role = auth.getRole();
  if (role === 'ADMIN') {
    return router.createUrlTree(['/admin/dashboard']);
  }
  if (role === 'AGENT') {
    return router.createUrlTree(['/agent/dashboard']);
  }
  return router.createUrlTree(['/user/dashboard']);
};
