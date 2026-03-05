import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStorageService } from './auth-storage.service';
import { UserRole } from '../store/auth.models';

export function roleGuard(requiredRole: UserRole): CanActivateFn {
  return (_route, state) => {
    const storage = inject(AuthStorageService);
    const router = inject(Router);

    if (!storage.hasSession()) {
      return router.createUrlTree(['/auth/login'], {
        queryParams: state.url ? { returnUrl: state.url } : undefined
      });
    }

    if (storage.hasRole(requiredRole)) {
      return true;
    }

    return router.createUrlTree([storage.getHomeRoute()]);
  };
}
