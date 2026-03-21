import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { profileActions } from './profile.actions';
import { ProfileUpdateRequest } from './profile.models';
import {
  selectCurrentProfile,
  selectProfileError,
  selectProfileLoading,
  selectProfileSaving,
  selectProfileSuccessMessage
} from './profile.selectors';

@Injectable({ providedIn: 'root' })
export class ProfileFacade {
  private readonly store = inject(Store);

  readonly profile$ = this.store.select(selectCurrentProfile);
  readonly loading$ = this.store.select(selectProfileLoading);
  readonly saving$ = this.store.select(selectProfileSaving);
  readonly error$ = this.store.select(selectProfileError);
  readonly successMessage$ = this.store.select(selectProfileSuccessMessage);

  load(): void {
    this.store.dispatch(profileActions.loadRequested());
  }

  update(payload: ProfileUpdateRequest): void {
    this.store.dispatch(profileActions.updateRequested({ payload }));
  }

  clearFeedback(): void {
    this.store.dispatch(profileActions.clearFeedback());
  }
}
