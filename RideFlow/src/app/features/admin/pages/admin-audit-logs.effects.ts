import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, exhaustMap, map, of } from 'rxjs';
import { AdminAuditLogsApiService } from './admin-audit-logs-api.service';
import { adminAuditLogsActions } from './admin-audit-logs.actions';

@Injectable()
export class AdminAuditLogsEffects {
  private readonly actions$ = inject(Actions);
  private readonly api = inject(AdminAuditLogsApiService);

  readonly load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(adminAuditLogsActions.loadRequested),
      exhaustMap(({ filters }) =>
        this.api.list(filters).pipe(
          map((response) => adminAuditLogsActions.loadSucceeded({ response, filters })),
          catchError((error: HttpErrorResponse) =>
            of(
              adminAuditLogsActions.loadFailed({
                error: this.parseApiError(error, 'Unable to load audit logs.')
              })
            )
          )
        )
      )
    )
  );

  private parseApiError(error: HttpErrorResponse, fallbackMessage: string): string {
    if (error.status === 0) {
      return 'Unable to reach the server. Check your Spring Boot API and try again.';
    }

    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }

    if (typeof error.error?.message === 'string' && error.error.message.trim()) {
      return error.error.message;
    }

    return fallbackMessage;
  }
}
