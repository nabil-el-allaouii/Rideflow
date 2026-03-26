import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, exhaustMap, forkJoin, map, of } from 'rxjs';
import { adminDashboardActions } from './admin-dashboard.actions';
import { AdminDashboardApiService } from './admin-dashboard-api.service';

@Injectable()
export class AdminDashboardEffects {
  private readonly actions$ = inject(Actions);
  private readonly api = inject(AdminDashboardApiService);

  readonly load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(adminDashboardActions.loadRequested),
      exhaustMap(({ range }) =>
        forkJoin({
          statistics: this.api.getStatistics(),
          rentalsReport: this.api.getRentalsReport(range.fromDate, range.toDate),
          revenueReport: this.api.getRevenueReport(range.fromDate, range.toDate)
        }).pipe(
          map(({ statistics, rentalsReport, revenueReport }) =>
            adminDashboardActions.loadSucceeded({
              statistics,
              rentalsReport,
              revenueReport,
              range
            })
          ),
          catchError((error: HttpErrorResponse) =>
            of(
              adminDashboardActions.loadFailed({
                error: this.parseApiError(error, 'Unable to load dashboard data.')
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
