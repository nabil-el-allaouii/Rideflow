import { Rental, RentalStatus } from '../../dashboard/pages/rental.models';

export interface AdminRentalFilters {
  query?: string | null;
  status?: RentalStatus | null;
  fromDate?: string | null;
  toDate?: string | null;
  userId?: number | null;
  scooterId?: number | null;
  page?: number | null;
  size?: number | null;
}

export interface AdminRentalPageResponse {
  content: Rental[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
