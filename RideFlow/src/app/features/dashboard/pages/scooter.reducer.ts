import { createReducer, on } from '@ngrx/store';
import { scooterActions } from './scooter.actions';
import { Scooter } from './scooter.models';
import { initialScooterState, ScooterMutationContext, ScooterState } from './scooter.state';

export const scooterFeatureKey = 'scooters';

export const scooterReducer = createReducer<ScooterState>(
  initialScooterState,
  on(scooterActions.loadAdminRequested, (state, { filters }) => ({
    ...state,
    adminLoading: true,
    adminError: null,
    adminFilters: filters
  })),
  on(scooterActions.loadAdminSucceeded, (state, { response, filters }) => ({
    ...state,
    adminLoading: false,
    adminScooters: sortAdminScooters(response.content),
    adminFilters: filters
  })),
  on(scooterActions.loadAdminFailed, (state, { error }) => ({
    ...state,
    adminLoading: false,
    adminError: error
  })),
  on(scooterActions.loadAvailableRequested, (state, { filters }) => ({
    ...state,
    availableLoading: true,
    availableError: null,
    availableFilters: filters
  })),
  on(scooterActions.loadAvailableSucceeded, (state, { response, filters }) => ({
    ...state,
    availableLoading: false,
    availableScooters: response.content,
    availableFilters: filters
  })),
  on(scooterActions.loadAvailableFailed, (state, { error }) => ({
    ...state,
    availableLoading: false,
    availableError: error
  })),
  on(scooterActions.createRequested, (state) => setMutationState(state, 'create')),
  on(scooterActions.updateRequested, (state) => setMutationState(state, 'update')),
  on(scooterActions.updateStatusRequested, (state) => setMutationState(state, 'status')),
  on(scooterActions.deleteRequested, (state) => setMutationState(state, 'delete')),
  on(scooterActions.createSucceeded, (state, { scooter }) => ({
    ...state,
    mutationInProgress: false,
    mutationError: null,
    mutationContext: null,
    lastMutation: buildMutation('create', scooter.id),
    adminScooters: sortAdminScooters([
      scooter,
      ...state.adminScooters.filter((item) => item.id !== scooter.id)
    ])
  })),
  on(scooterActions.updateSucceeded, (state, { scooter }) => ({
    ...state,
    mutationInProgress: false,
    mutationError: null,
    mutationContext: null,
    lastMutation: buildMutation('update', scooter.id),
    adminScooters: upsertAdminScooter(state.adminScooters, scooter),
    availableScooters: syncAvailableScooters(state.availableScooters, scooter)
  })),
  on(scooterActions.updateStatusSucceeded, (state, { scooter }) => ({
    ...state,
    mutationInProgress: false,
    mutationError: null,
    mutationContext: null,
    lastMutation: buildMutation('status', scooter.id),
    adminScooters: upsertAdminScooter(state.adminScooters, scooter),
    availableScooters: syncAvailableScooters(state.availableScooters, scooter)
  })),
  on(scooterActions.deleteSucceeded, (state, { scooterId }) => ({
    ...state,
    mutationInProgress: false,
    mutationError: null,
    mutationContext: null,
    lastMutation: buildMutation('delete', scooterId),
    adminScooters: state.adminScooters.filter((scooter) => scooter.id !== scooterId),
    availableScooters: state.availableScooters.filter((scooter) => scooter.id !== scooterId)
  })),
  on(scooterActions.createFailed, (state, { error }) => mutationFailed(state, 'create', error)),
  on(scooterActions.updateFailed, (state, { error }) => mutationFailed(state, 'update', error)),
  on(scooterActions.updateStatusFailed, (state, { error }) => mutationFailed(state, 'status', error)),
  on(scooterActions.deleteFailed, (state, { error }) => mutationFailed(state, 'delete', error)),
  on(scooterActions.clearMutationError, (state) => ({
    ...state,
    mutationError: null
  }))
);

function setMutationState(state: ScooterState, context: ScooterMutationContext): ScooterState {
  return {
    ...state,
    mutationInProgress: true,
    mutationError: null,
    mutationContext: context
  };
}

function mutationFailed(
  state: ScooterState,
  context: ScooterMutationContext,
  error: string
): ScooterState {
  return {
    ...state,
    mutationInProgress: false,
    mutationContext: context,
    mutationError: error
  };
}

function buildMutation(type: Exclude<ScooterMutationContext, null>, scooterId?: number) {
  return {
    type,
    scooterId,
    occurredAt: Date.now()
  };
}

function sortAdminScooters(scooters: Scooter[]): Scooter[] {
  return [...scooters].sort((left, right) => right.id - left.id);
}

function upsertAdminScooter(scooters: Scooter[], scooter: Scooter): Scooter[] {
  return sortAdminScooters([
    scooter,
    ...scooters.filter((currentScooter) => currentScooter.id !== scooter.id)
  ]);
}

function syncAvailableScooters(scooters: Scooter[], scooter: Scooter): Scooter[] {
  const exists = scooters.some((currentScooter) => currentScooter.id === scooter.id);

  if (!exists) {
    return scooters;
  }

  if (scooter.status !== 'AVAILABLE') {
    return scooters.filter((currentScooter) => currentScooter.id !== scooter.id);
  }

  return scooters.map((currentScooter) =>
    currentScooter.id === scooter.id ? scooter : currentScooter
  );
}
