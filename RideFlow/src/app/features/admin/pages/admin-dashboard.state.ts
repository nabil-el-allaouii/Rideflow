import {
  AdminDashboardLoadRange,
  AdminDashboardStatistics,
  RentalReportPoint,
  RevenueReportPoint
} from './admin-dashboard.models';

export interface AdminDashboardState {
  statistics: AdminDashboardStatistics | null;
  rentalsReport: RentalReportPoint[];
  revenueReport: RevenueReportPoint[];
  range: AdminDashboardLoadRange | null;
  loading: boolean;
  error: string | null;
}

export const initialAdminDashboardState: AdminDashboardState = {
  statistics: null,
  rentalsReport: [],
  revenueReport: [],
  range: null,
  loading: false,
  error: null
};
