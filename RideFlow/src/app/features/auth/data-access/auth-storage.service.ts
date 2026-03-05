import { Injectable } from '@angular/core';
import { AuthResponse, AuthUser, StoredAuthSession, UserRole } from '../store/auth.models';

const AUTH_SESSION_KEY = 'rf.auth_session';

@Injectable({ providedIn: 'root' })
export class AuthStorageService {
  private static readonly SESSION_EXPIRY_SKEW_MS = 5000;

  saveSession(response: AuthResponse): StoredAuthSession {
    const session: StoredAuthSession = {
      ...response,
      expiresAt: Date.now() + response.expiresIn * 1000
    };

    localStorage.setItem(AUTH_SESSION_KEY, JSON.stringify(session));
    return session;
  }

  getSession(): StoredAuthSession | null {
    const raw = localStorage.getItem(AUTH_SESSION_KEY);

    if (!raw) {
      return null;
    }

    try {
      const session = JSON.parse(raw) as StoredAuthSession;
      if (!this.isSessionValid(session)) {
        this.clearSession();
        return null;
      }

      return session;
    } catch {
      localStorage.removeItem(AUTH_SESSION_KEY);
      return null;
    }
  }

  updateUser(user: AuthUser): StoredAuthSession | null {
    const session = this.getSession();

    if (!session) {
      return null;
    }

    const nextSession: StoredAuthSession = {
      ...session,
      user
    };

    localStorage.setItem(AUTH_SESSION_KEY, JSON.stringify(nextSession));
    return nextSession;
  }

  getAccessToken(): string | null {
    return this.getSession()?.accessToken ?? null;
  }

  getRefreshToken(): string | null {
    return this.getSession()?.refreshToken ?? null;
  }

  getRole(): UserRole | null {
    return this.getSession()?.user.role ?? null;
  }

  hasSession(): boolean {
    return this.getSession() !== null;
  }

  hasRole(role: UserRole): boolean {
    return this.getRole() === role;
  }

  clearSession(): void {
    localStorage.removeItem(AUTH_SESSION_KEY);
  }

  getHomeRoute(): string {
    return this.hasRole('ADMIN') ? '/admin/dashboard' : '/dashboard';
  }

  private isSessionValid(session: StoredAuthSession | null): session is StoredAuthSession {
    if (!session?.accessToken || !session?.refreshToken || !session?.user?.role) {
      return false;
    }

    if (typeof session.expiresAt !== 'number') {
      return false;
    }

    return session.expiresAt > Date.now() + AuthStorageService.SESSION_EXPIRY_SKEW_MS;
  }
}
