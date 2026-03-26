import { createActionGroup, props } from '@ngrx/store';
import {
  AdminDashboardLoadRange,
  AdminDashboardStatistics,
  RentalReportPoint,
  RevenueReportPoint
} from './admin-dashboard.models';

export const adminDashboardActions = createActionGroup({
  source: 'Admin Dashboard',
  events: {
    'Load Requested': props<{ range: AdminDashboardLoadRange }>(),
    'Load Succeeded': props<{
      statistics: AdminDashboardStatistics;
      rentalsReport: RentalReportPoint[];
      revenueReport: RevenueReportPoint[];
      range: AdminDashboardLoadRange;
    }>(),
    'Load Failed': props<{ error: string }>()
  }
});
