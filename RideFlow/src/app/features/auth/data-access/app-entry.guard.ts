import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStorageService } from './auth-storage.service';

export const appEntryGuard: CanActivateFn = () => {
  const storage = inject(AuthStorageService);
  const router = inject(Router);

  return router.createUrlTree([storage.hasSession() ? storage.getHomeRoute() : '/auth/login']);
};
