import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

interface LoginRequest {
  email: string;
  password: string;
}

interface RegisterRequest extends LoginRequest {}

interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
}

export interface AuthUser {
  userId: string;
  email: string;
  role: 'USER' | 'ADMIN';
  exp: number;
}

export interface RegisterResponse {
  id: string;
  email: string;
  role: 'USER' | 'ADMIN';
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storageKey = 'order-payment-access-token';
  private readonly tokenState = signal<string | null>(this.readStoredToken());

  readonly token = this.tokenState.asReadonly();
  readonly user = (): AuthUser | null => this.decodeToken(this.tokenState());
  readonly isAuthenticated = (): boolean => this.user() !== null;

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', request).pipe(
      tap((response) => {
        localStorage.setItem(this.storageKey, response.accessToken);
        this.tokenState.set(response.accessToken);
      }),
    );
  }

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>('/api/auth/register', request);
  }

  logout(): void {
    localStorage.removeItem(this.storageKey);
    this.tokenState.set(null);
  }

  private readStoredToken(): string | null {
    const token = localStorage.getItem(this.storageKey);
    return this.decodeToken(token) ? token : null;
  }

  private decodeToken(token: string | null): AuthUser | null {
    if (!token) {
      return null;
    }

    try {
      const payload = token.split('.')[1];
      if (!payload) {
        return null;
      }
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const decoded = JSON.parse(atob(normalized)) as AuthUser;
      return decoded.exp * 1000 > Date.now() ? decoded : null;
    } catch {
      return null;
    }
  }
}
