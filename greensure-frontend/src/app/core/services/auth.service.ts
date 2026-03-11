import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth';

@Injectable({
    providedIn: 'root'
})
export class AuthService {

    // Inject HttpClient using inject() — no constructor needed
    private httpClient: HttpClient = inject(HttpClient);

    // Base URL from environment — http://localhost:9090
    private apiUrl: string = environment.apiUrl;

    // ── LOGIN ─────────────────────────────────────────────────
    // POST /auth/login
    // Sends email + password, returns AuthResponse with token, role, etc.
    // The component will handle saving to localStorage and redirecting.
    login(request: LoginRequest): Observable<ApiResponse<AuthResponse>> {
        return this.httpClient.post<ApiResponse<AuthResponse>>(
            `${this.apiUrl}/auth/login`,
            request
        );
    }

    // ── REGISTER ──────────────────────────────────────────────
    // POST /auth/register
    // Sends full registration form (HOUSEHOLD or MSME fields).
    // Returns ApiResponse with AuthResponse (backend returns token on register too).
    register(request: RegisterRequest): Observable<ApiResponse<AuthResponse>> {
        return this.httpClient.post<ApiResponse<AuthResponse>>(
            `${this.apiUrl}/auth/register`,
            request
        );
    }

    // ── LOGOUT ────────────────────────────────────────────────
    // POST /auth/logout
    // Notifies backend that the user logged out.
    // The component also clears localStorage and navigates to /login.
    logout(): Observable<ApiResponse<any>> {
        return this.httpClient.post<ApiResponse<any>>(
            `${this.apiUrl}/auth/logout`,
            {}
        );
    }

    // ── FORGOT PASSWORD ───────────────────────────────────────
    // POST /auth/forgot-password
    // Sends the user's email so the backend can send a reset link/OTP.
    forgotPassword(email: string): Observable<ApiResponse<any>> {
        return this.httpClient.post<ApiResponse<any>>(
            `${this.apiUrl}/auth/forgot-password`,
            { email }
        );
    }

    // ── RESET PASSWORD ────────────────────────────────────────
    // POST /auth/reset-password
    // Sends the reset token/OTP + new password to complete password reset.
    resetPassword(token: string, newPassword: string): Observable<ApiResponse<any>> {
        return this.httpClient.post<ApiResponse<any>>(
            `${this.apiUrl}/auth/reset-password`,
            { token, newPassword }
        );
    }

    // ── HELPER: Check if user is logged in ────────────────────
    // Simply checks if a JWT token exists in localStorage.
    isLoggedIn(): boolean {
        const token = localStorage.getItem('token');
        if (!token) return false;

        // Check if the JWT is expired
        if (this.isTokenExpired(token)) {
            this.clearSession();
            return false;
        }
        return true;
    }

    // ── HELPER: Check if JWT token is expired ──────────────────
    private isTokenExpired(token: string): boolean {
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const expiry = payload.exp * 1000; // convert to ms
            return Date.now() > expiry;
        } catch {
            return true; // malformed token = treat as expired
        }
    }

    // ── HELPER: Get current role ──────────────────────────────
    getRole(): string | null {
        return localStorage.getItem('role');
    }

    // ── HELPER: Get stored full name ──────────────────────────
    getFullName(): string | null {
        return localStorage.getItem('fullName');
    }

    // ── HELPER: Get stored user ID ────────────────────────────
    getUserId(): string | null {
        return localStorage.getItem('userId');
    }

    // ── HELPER: Clear session ─────────────────────────────────
    clearSession(): void {
        localStorage.clear();
    }
}
