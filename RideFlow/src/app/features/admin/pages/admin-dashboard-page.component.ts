import { Component, computed, inject, OnInit } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { AuthFacade } from '../../auth/store/auth.facade';
import {
  AdminDashboardStatistics,
  RentalReportPoint,
  RevenueReportPoint
} from './admin-dashboard.models';
import { AdminDashboardFacade } from './admin-dashboard.facade';

type MenuIcon =
  | 'dashboard'
  | 'scooter'
  | 'users'
  | 'rentals'
  | 'payments'
  | 'pricing'
  | 'audit'
  | 'logout';

interface MenuItem {
  label: string;
  icon: MenuIcon;
  route?: string;
  active?: boolean;
}

interface AdminMetricCard {
  value: string;
  label: string;
  helper: string;
  helperTone?: 'positive' | 'warning';
  icon: 'revenue' | 'active' | 'fleet' | 'users';
}

interface FleetStatusItem {
  label: string;
  value: number;
  color: string;
}

interface RevenueBar {
  day: string;
  amount: number;
}

interface RidePoint {
  time: string;
  value: number;
}

@Component({
  selector: 'app-admin-dashboard-page',
  imports: [],
  templateUrl: './admin-dashboard-page.component.html',
  styleUrl: './admin-dashboard-page.component.css'
})
export class AdminDashboardPageComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authFacade = inject(AuthFacade);
  private readonly adminDashboardFacade = inject(AdminDashboardFacade);

  readonly authUser = toSignal(this.authFacade.user$, { initialValue: null });
  readonly statistics = toSignal(this.adminDashboardFacade.statistics$, { initialValue: null as AdminDashboardStatistics | null });
  readonly rentalsReport = toSignal(this.adminDashboardFacade.rentalsReport$, { initialValue: [] as RentalReportPoint[] });
  readonly revenueReport = toSignal(this.adminDashboardFacade.revenueReport$, { initialValue: [] as RevenueReportPoint[] });
  readonly loading = toSignal(this.adminDashboardFacade.loading$, { initialValue: false });
  readonly error = toSignal(this.adminDashboardFacade.error$, { initialValue: null });

  readonly mainMenuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', active: true, route: '/admin/dashboard' },
    { label: 'Scooter Fleet', icon: 'scooter', route: '/admin/fleet' },
    { label: 'User Management', icon: 'users', route: '/admin/users' },
    { label: 'Rental Monitor', icon: 'rentals', route: '/admin/rentals' },
    { label: 'Payments', icon: 'payments', route: '/admin/payments' },
    { label: 'Pricing', icon: 'pricing', route: '/admin/pricing' },
    { label: 'Audit Logs', icon: 'audit', route: '/admin/audit-logs' }
  ];

  readonly footerMenuItems: MenuItem[] = [{ label: 'Sign Out', icon: 'logout' }];

  readonly initials = computed(() => {
    const fullName = this.authUser()?.fullName?.trim();
    if (!fullName) {
      return 'AD';
    }

    return fullName
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  });

  readonly metricCards = computed<AdminMetricCard[]>(() => {
    const stats = this.statistics();
    if (!stats) {
      return [
        { value: '$0.00', label: 'Total Revenue', helper: 'Loading...', icon: 'revenue' },
        { value: '0', label: 'Active Rides', helper: 'Loading...', icon: 'active' },
        { value: '0', label: 'Fleet Size', helper: 'Loading...', icon: 'fleet' },
        { value: '0', label: 'Registered Users', helper: 'Loading...', icon: 'users' }
      ];
    }

    return [
      {
        value: this.formatCurrency(stats.totalRevenue),
        label: 'Total Revenue',
        helper: `${this.formatCurrency(stats.todayRevenue)} today`,
        helperTone: 'positive',
        icon: 'revenue'
      },
      {
        value: String(stats.activeRentals),
        label: 'Active Rides',
        helper: `${stats.inUseScooters} scooters in use`,
        helperTone: 'positive',
        icon: 'active'
      },
      {
        value: String(stats.totalScooters),
        label: 'Fleet Size',
        helper: `${stats.scootersInMaintenance} in maintenance`,
        helperTone: stats.scootersInMaintenance > 0 ? 'warning' : 'positive',
        icon: 'fleet'
      },
      {
        value: String(stats.totalUsers),
        label: 'Registered Users',
        helper: `${stats.activeUsers} active accounts`,
        helperTone: 'positive',
        icon: 'users'
      }
    ];
  });

  readonly weeklyRevenue = computed<RevenueBar[]>(() =>
    this.revenueReport().map((point) => ({
      day: this.formatDay(point.date),
      amount: point.revenue ?? 0
    }))
  );

  readonly fleetStatus = computed<FleetStatusItem[]>(() => {
    const stats = this.statistics();
    if (!stats) {
      return [];
    }

    return [
      { label: 'Available', value: stats.availableScooters, color: '#2faa85' },
      { label: 'Reserved', value: stats.reservedScooters, color: '#2f84e3' },
      { label: 'In Use', value: stats.inUseScooters, color: '#17a2b8' },
      { label: 'Locked', value: stats.lockedScooters, color: '#f2a007' },
      { label: 'Maintenance', value: stats.scootersInMaintenance, color: '#e33e3a' },
      { label: 'Disabled', value: stats.disabledScooters, color: '#6c757d' }
    ].filter((item) => item.value > 0);
  });

  readonly ridesThisWeek = computed<RidePoint[]>(() =>
    this.rentalsReport().map((point) => ({
      time: this.formatDay(point.date),
      value: point.rentalCount ?? 0
    }))
  );

  readonly revenueAxisTicks = computed(() => this.buildAxisTicks(this.weeklyRevenue().map((item) => item.amount)));
  readonly ridesAxisTicks = computed(() => this.buildAxisTicks(this.ridesThisWeek().map((item) => item.value)));

  readonly maxRevenue = computed(() => Math.max(...this.revenueAxisTicks(), 1));
  readonly maxRides = computed(() => Math.max(...this.ridesAxisTicks(), 1));

  readonly fleetChartBackground = computed(() => this.buildFleetChartBackground());
  readonly ridesPath = computed(() => this.buildLinePath());

  ngOnInit(): void {
    const today = new Date();
    const from = new Date(today);
    from.setDate(today.getDate() - 6);

    this.adminDashboardFacade.load({
      fromDate: this.formatIsoDate(from),
      toDate: this.formatIsoDate(today)
    });
  }

  onMainMenuClick(item: MenuItem): void {
    if (item.route) {
      void this.router.navigate([item.route]);
    }
  }

  onFooterMenuClick(item: MenuItem): void {
    if (item.icon === 'logout') {
      this.authFacade.logout();
      return;
    }

    if (item.route) {
      void this.router.navigate([item.route]);
    }
  }

  getRevenueBarHeight(value: number): number {
    return Math.max(12, (value / this.maxRevenue()) * 100);
  }

  getRideLabelOffset(index: number): number {
    const rides = this.ridesThisWeek();
    if (rides.length < 2) {
      return 0;
    }

    return (index / (rides.length - 1)) * 100;
  }

  private buildFleetChartBackground(): string {
    const fleet = this.fleetStatus();
    const total = fleet.reduce((sum, item) => sum + item.value, 0);
    if (total <= 0) {
      return 'conic-gradient(#d8dfdf 0deg 360deg)';
    }

    let currentAngle = 0;
    const slices = fleet.map((item) => {
      const angle = (item.value / total) * 360;
      const start = currentAngle;
      const end = currentAngle + angle;
      currentAngle = end;
      return `${item.color} ${start.toFixed(2)}deg ${end.toFixed(2)}deg`;
    });

    return `conic-gradient(${slices.join(', ')})`;
  }

  private buildLinePath(): string {
    const rides = this.ridesThisWeek();
    if (!rides.length) {
      return '';
    }

    const width = 960;
    const height = 220;
    const topPadding = 12;
    const bottomPadding = 12;
    const usableHeight = height - topPadding - bottomPadding;
    const stepX = rides.length > 1 ? width / (rides.length - 1) : 0;

    const points = rides.map((point, index) => {
      const x = index * stepX;
      const normalized = Math.min(point.value, this.maxRides()) / this.maxRides();
      const y = height - bottomPadding - normalized * usableHeight;
      return { x, y };
    });

    let path = `M ${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`;

    for (let index = 1; index < points.length; index += 1) {
      const previous = points[index - 1];
      const current = points[index];
      const controlX = ((previous.x + current.x) / 2).toFixed(2);

      path += ` C ${controlX} ${previous.y.toFixed(2)} ${controlX} ${current.y.toFixed(2)} ${current.x.toFixed(2)} ${current.y.toFixed(2)}`;
    }

    return path;
  }

  private buildAxisTicks(values: number[]): number[] {
    const maxValue = Math.max(...values, 0);
    const roundedMax = Math.max(4, Math.ceil(maxValue / 4) * 4);
    return [roundedMax, roundedMax * 0.75, roundedMax * 0.5, roundedMax * 0.25, 0].map((value) =>
      Number(value.toFixed(0))
    );
  }

  private formatCurrency(value: number | null | undefined): string {
    return `$${(value ?? 0).toFixed(2)}`;
  }

  private formatDay(value: string): string {
    return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, { weekday: 'short' });
  }

  private formatIsoDate(date: Date): string {
    return date.toISOString().slice(0, 10);
  }
}
