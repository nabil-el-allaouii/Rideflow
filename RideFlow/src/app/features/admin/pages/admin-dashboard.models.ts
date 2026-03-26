export interface AdminDashboardStatistics {
  totalScooters: number;
  availableScooters: number;
  reservedScooters: number;
  inUseScooters: number;
  lockedScooters: number;
  disabledScooters: number;
  activeRentals: number;
  totalRevenue: number;
  todayRevenue: number;
  lowBatteryScooters: number;
  scootersInMaintenance: number;
  totalUsers: number;
  activeUsers: number;
}

export interface RentalReportPoint {
  date: string;
  rentalCount: number;
}

export interface RevenueReportPoint {
  date: string;
  revenue: number;
}

export interface AdminDashboardLoadRange {
  fromDate: string;
  toDate: string;
}
