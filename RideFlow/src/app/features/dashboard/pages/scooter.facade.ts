import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { scooterActions } from './scooter.actions';
import {
  ScooterFilters,
  ScooterFormPayload,
  ScooterStatus,
  ScooterUpdatePayload
} from './scooter.models';
import {
  selectAdminError,
  selectAdminLoading,
  selectAdminScooters,
  selectAvailableError,
  selectAvailableLoading,
  selectAvailableScooters,
  selectLastScooterMutation,
  selectScooterMutationContext,
  selectScooterMutationError,
  selectScooterMutationInProgress
} from './scooter.selectors';

@Injectable({ providedIn: 'root' })
export class ScooterFacade {
  private readonly store = inject(Store);

  readonly adminScooters$ = this.store.select(selectAdminScooters);
  readonly adminLoading$ = this.store.select(selectAdminLoading);
  readonly adminError$ = this.store.select(selectAdminError);
  readonly availableScooters$ = this.store.select(selectAvailableScooters);
  readonly availableLoading$ = this.store.select(selectAvailableLoading);
  readonly availableError$ = this.store.select(selectAvailableError);
  readonly mutationInProgress$ = this.store.select(selectScooterMutationInProgress);
  readonly mutationError$ = this.store.select(selectScooterMutationError);
  readonly mutationContext$ = this.store.select(selectScooterMutationContext);
  readonly lastMutation$ = this.store.select(selectLastScooterMutation);

  loadAdmin(filters: ScooterFilters): void {
    this.store.dispatch(scooterActions.loadAdminRequested({ filters }));
  }

  loadAvailable(filters: ScooterFilters): void {
    this.store.dispatch(scooterActions.loadAvailableRequested({ filters }));
  }

  create(payload: ScooterFormPayload): void {
    this.store.dispatch(scooterActions.createRequested({ payload }));
  }

  update(scooterId: number, payload: ScooterUpdatePayload): void {
    this.store.dispatch(scooterActions.updateRequested({ scooterId, payload }));
  }

  updateStatus(scooterId: number, status: ScooterStatus): void {
    this.store.dispatch(scooterActions.updateStatusRequested({ scooterId, status }));
  }

  delete(scooterId: number): void {
    this.store.dispatch(scooterActions.deleteRequested({ scooterId }));
  }

  clearMutationError(): void {
    this.store.dispatch(scooterActions.clearMutationError());
  }
}
