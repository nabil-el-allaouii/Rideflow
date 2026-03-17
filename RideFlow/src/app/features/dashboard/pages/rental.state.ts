import { ActiveRental, Rental, RentalFilters } from './rental.models';

export type RentalMutationContext = 'unlock' | 'cancel' | 'start' | 'end' | null;

export interface RentalMutationEvent {
  type: Exclude<RentalMutationContext, null>;
  rentalId: number;
  occurredAt: number;
}

export interface RentalState {
  activeRental: ActiveRental | null;
  history: Rental[];
  activeLoading: boolean;
  historyLoading: boolean;
  mutationInProgress: boolean;
  error: string | null;
  mutationError: string | null;
  mutationContext: RentalMutationContext;
  lastMutation: RentalMutationEvent | null;
  historyFilters: RentalFilters;
}

export const defaultRentalHistoryFilters: RentalFilters = {
  query: null,
  status: null,
  fromDate: null,
  toDate: null,
  page: 0,
  size: 50
};

export const initialRentalState: RentalState = {
  activeRental: null,
  history: [],
  activeLoading: false,
  historyLoading: false,
  mutationInProgress: false,
  error: null,
  mutationError: null,
  mutationContext: null,
  lastMutation: null,
  historyFilters: defaultRentalHistoryFilters
};
