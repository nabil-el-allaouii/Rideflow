import { createReducer, on } from '@ngrx/store';
import { rentalActions } from './rental.actions';
import { Rental } from './rental.models';
import { initialRentalState, RentalMutationContext, RentalState } from './rental.state';

export const rentalFeatureKey = 'rentals';

export const rentalReducer = createReducer<RentalState>(
  initialRentalState,
  on(rentalActions.loadActiveRequested, (state) => ({
    ...state,
    activeLoading: true,
    error: null
  })),
  on(rentalActions.loadActiveSucceeded, (state, { activeRental }) => ({
    ...state,
    activeLoading: false,
    activeRental
  })),
  on(rentalActions.loadActiveFailed, (state, { error }) => ({
    ...state,
    activeLoading: false,
    error
  })),
  on(rentalActions.loadHistoryRequested, (state, { filters }) => ({
    ...state,
    historyLoading: true,
    error: null,
    historyFilters: filters
  })),
  on(rentalActions.loadHistorySucceeded, (state, { response, filters }) => ({
    ...state,
    historyLoading: false,
    history: response.content,
    historyFilters: filters
  })),
  on(rentalActions.loadHistoryFailed, (state, { error }) => ({
    ...state,
    historyLoading: false,
    error
  })),
  on(rentalActions.unlockRequested, (state) => setMutationState(state, 'unlock')),
  on(rentalActions.cancelRideRequested, (state) => setMutationState(state, 'cancel')),
  on(rentalActions.startRideRequested, (state) => setMutationState(state, 'start')),
  on(rentalActions.endRideRequested, (state) => setMutationState(state, 'end')),
  on(rentalActions.unlockSucceeded, (state, { rental }) => ({
    ...state,
    mutationInProgress: false,
    mutationError: null,
    mutationContext: null,
    lastMutation: buildMutation('unlock', rental.id),
    activeRental: toActiveRental(rental)
  })),
  on(rentalActions.cancelRideSucceeded, (state, { rental }) => ({
    ...state,
    mutationInProgress: false,
    mutationError: null,
    mutationContext: null,
    lastMutation: buildMutation('cancel', rental.id),
    activeRental: null,
    history: [rental, ...state.history.filter((item) => item.id !== rental.id)]
  })),
  on(rentalActions.startRideSucceeded, (state, { rental }) => ({
    ...state,
    mutationInProgress: false,
    mutationError: null,
    mutationContext: null,
    lastMutation: buildMutation('start', rental.id),
    activeRental: toActiveRental(rental)
  })),
  on(rentalActions.endRideSucceeded, (state, { rental }) => ({
    ...state,
    mutationInProgress: false,
    mutationError: null,
    mutationContext: null,
    lastMutation: buildMutation('end', rental.id),
    activeRental: null,
    history: [rental, ...state.history.filter((item) => item.id !== rental.id)]
  })),
  on(rentalActions.unlockFailed, (state, { error }) => mutationFailed(state, 'unlock', error)),
  on(rentalActions.cancelRideFailed, (state, { error }) => mutationFailed(state, 'cancel', error)),
  on(rentalActions.startRideFailed, (state, { error }) => mutationFailed(state, 'start', error)),
  on(rentalActions.endRideFailed, (state, { error }) => mutationFailed(state, 'end', error)),
  on(rentalActions.clearMutationError, (state) => ({
    ...state,
    mutationError: null
  }))
);

function setMutationState(state: RentalState, context: RentalMutationContext): RentalState {
  return {
    ...state,
    mutationInProgress: true,
    mutationError: null,
    mutationContext: context
  };
}

function mutationFailed(state: RentalState, context: RentalMutationContext, error: string): RentalState {
  return {
    ...state,
    mutationInProgress: false,
    mutationContext: context,
    mutationError: error
  };
}

function buildMutation(type: Exclude<RentalMutationContext, null>, rentalId: number) {
  return {
    type,
    rentalId,
    occurredAt: Date.now()
  };
}

function toActiveRental(rental: Rental) {
  return {
    rentalId: rental.id,
    scooterId: rental.scooterId,
    scooterCode: rental.scooterCode,
    scooterModel: rental.scooterModel,
    status: rental.status,
    createdAt: rental.createdAt,
    startTime: rental.startTime
  };
}
