import { ProfileResponse } from './profile.models';

export interface ProfileState {
  profile: ProfileResponse | null;
  loading: boolean;
  saving: boolean;
  error: string | null;
  successMessage: string | null;
}

export const initialProfileState: ProfileState = {
  profile: null,
  loading: false,
  saving: false,
  error: null,
  successMessage: null
};
