import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  OtpVerifyRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
} from '../models/auth';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly base = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'gs_token';
  private readonly USER_KEY = 'gs_user';

  // ── Reactive state ──────────────────────────────────────────
  readonly currentUser = signal<AuthResponse | null>(null);
  readonly isLoggedIn = computed(() => !!this.currentUser());
  readonly userRole = computed(() => this.currentUser()?.role ?? null);

  constructor() {
    // Restore session from localStorage on app start
    const stored = localStorage.getItem(this.USER_KEY);
    if (stored) {
      try {
        const parsed: AuthResponse = JSON.parse(stored);
        // Verify token is not expired
        if (!this.isTokenExpired(parsed.token)) {
          this.currentUser.set(parsed);
        } else {
          this.clearStorage();
        }
      } catch {
        this.clearStorage();
      }
    }
  }

  // ── Registration (2-step flow) ─────────────────────────────

  register(req: RegisterRequest): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.base}/register`, req);
  }

  verifyOtp(req: OtpVerifyRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.base}/verify-otp`, req).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.saveSession(res.data);
        }
      })
    );
  }

  // ── Login ──────────────────────────────────────────────────

  login(req: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.base}/login`, req).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.saveSession(res.data);
        }
      })
    );
  }

  // ── Password Reset ─────────────────────────────────────────

  forgotPassword(req: ForgotPasswordRequest): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.base}/forgot-password`, req);
  }

  resetPassword(req: ResetPasswordRequest): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.base}/reset-password`, req);
  }

  // ── Session Management ─────────────────────────────────────

  saveSession(response: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(response));
    this.currentUser.set(response);
  }

  logout(): void {
    this.clearStorage();
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getUser(): AuthResponse | null {
    return this.currentUser();
  }

  getRole(): string | null {
    return this.currentUser()?.role ?? null;
  }

  getUserId(): number | null {
    return this.currentUser()?.userId ?? null;
  }

  getFullName(): string | null {
    return this.currentUser()?.fullName ?? null;
  }

  // ── Private helpers ────────────────────────────────────────

  private clearStorage(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiry = payload.exp * 1000;
      return Date.now() > expiry;
    } catch {
      return true;
    }
  }

  clearSession(): void {
    this.clearStorage();
    this.currentUser.set(null);
  }
}
