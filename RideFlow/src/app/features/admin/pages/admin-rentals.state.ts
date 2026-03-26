import { Rental } from '../../dashboard/pages/rental.models';
import { AdminRentalFilters } from './admin-rentals.models';

export interface AdminRentalsState {
  rentals: Rental[];
  filters: AdminRentalFilters;
  loading: boolean;
  exporting: boolean;
  error: string | null;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const defaultAdminRentalFilters: AdminRentalFilters = {
  query: null,
  status: null,
  fromDate: null,
  toDate: null,
  userId: null,
  scooterId: null,
  page: 0,
  size: 50
};

export const initialAdminRentalsState: AdminRentalsState = {
  rentals: [],
  filters: defaultAdminRentalFilters,
  loading: false,
  exporting: false,
  error: null,
  page: 0,
  size: 50,
  totalElements: 0,
  totalPages: 0
};
