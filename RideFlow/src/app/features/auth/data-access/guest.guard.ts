import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStorageService } from './auth-storage.service';

export const guestGuard: CanActivateFn = () => {
  const storage = inject(AuthStorageService);
  const router = inject(Router);

  if (!storage.hasSession()) {
    return true;
  }

  return router.createUrlTree([storage.getHomeRoute()]);
};
