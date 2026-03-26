import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, concatMap, exhaustMap, map, of, switchMap } from 'rxjs';
import { adminUsersActions } from './admin-users.actions';
import { AdminUsersApiService } from './admin-users-api.service';

@Injectable()
export class AdminUsersEffects {
  private readonly actions$ = inject(Actions);
  private readonly api = inject(AdminUsersApiService);

  readonly loadList$ = createEffect(() =>
    this.actions$.pipe(
      ofType(adminUsersActions.loadListRequested),
      exhaustMap(({ filters }) =>
        this.api.list(filters).pipe(
          map((response) => adminUsersActions.loadListSucceeded({ response, filters })),
          catchError((error: HttpErrorResponse) =>
            of(adminUsersActions.loadListFailed({ error: this.parseApiError(error, 'Unable to load users.') }))
          )
        )
      )
    )
  );

  readonly loadDetail$ = createEffect(() =>
    this.actions$.pipe(
      ofType(adminUsersActions.loadDetailRequested),
      switchMap(({ userId }) =>
        this.api.getById(userId).pipe(
          map((user) => adminUsersActions.loadDetailSucceeded({ user })),
          catchError((error: HttpErrorResponse) =>
            of(adminUsersActions.loadDetailFailed({ error: this.parseApiError(error, 'Unable to load user details.') }))
          )
        )
      )
    )
  );

  readonly updateStatus$ = createEffect(() =>
    this.actions$.pipe(
      ofType(adminUsersActions.updateStatusRequested),
      concatMap(({ userId, status }) =>
        this.api.updateStatus(userId, { status }).pipe(
          map((user) => adminUsersActions.updateStatusSucceeded({ user })),
          catchError((error: HttpErrorResponse) =>
            of(adminUsersActions.updateStatusFailed({ error: this.parseApiError(error, 'Unable to update user status.') }))
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
