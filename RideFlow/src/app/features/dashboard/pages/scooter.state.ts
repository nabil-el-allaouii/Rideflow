import { Scooter, ScooterFilters } from './scooter.models';

export type ScooterMutationContext = 'create' | 'update' | 'status' | 'delete' | null;

export interface ScooterMutationEvent {
  type: Exclude<ScooterMutationContext, null>;
  scooterId?: number;
  occurredAt: number;
}

export interface ScooterState {
  adminScooters: Scooter[];
  availableScooters: Scooter[];
  adminLoading: boolean;
  availableLoading: boolean;
  adminError: string | null;
  availableError: string | null;
  mutationInProgress: boolean;
  mutationError: string | null;
  mutationContext: ScooterMutationContext;
  lastMutation: ScooterMutationEvent | null;
  adminFilters: ScooterFilters;
  availableFilters: ScooterFilters;
}

export const defaultAdminScooterFilters: ScooterFilters = {
  minBattery: 0,
  size: 100
};

export const initialScooterState: ScooterState = {
  adminScooters: [],
  availableScooters: [],
  adminLoading: false,
  availableLoading: false,
  adminError: null,
  availableError: null,
  mutationInProgress: false,
  mutationError: null,
  mutationContext: null,
  lastMutation: null,
  adminFilters: defaultAdminScooterFilters,
  availableFilters: {}
};
