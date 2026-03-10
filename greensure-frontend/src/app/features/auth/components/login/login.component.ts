import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
    selector: 'app-login',
    imports: [FormsModule, RouterLink],
    templateUrl: './login.component.html'
})
export class LoginComponent {

    // ── Form fields ──────────────────────────────────────────
    email = signal('');
    password = signal('');

    // ── UI state ─────────────────────────────────────────────

    errorMessage = signal('');

    constructor(
        private authService: AuthService,
        private router: Router
    ) { }

    // ── LOGIN ────────────────────────────────────────────────
    // Called when the form is submitted.
    // Sends credentials to POST /auth/login, saves session data,
    // then redirects based on role.
    onLogin(): void {
        this.errorMessage.set('');

        this.authService.login({
            email: this.email(),
            password: this.password()
        }).subscribe({
            next: (res) => {
                if (res.success) {
                    const data = res.data;

                    // Save all session values to localStorage
                    localStorage.setItem('token', data.token);
                    localStorage.setItem('role', data.role);
                    localStorage.setItem('userId', String(data.id));
                    localStorage.setItem('fullName', data.fullName);
                    localStorage.setItem('userType', data.userType);

                    // Redirect based on role
                    if (data.role === 'ADMIN') {
                        this.router.navigate(['/admin/dashboard']);
                    } else if (data.role === 'AGENT') {
                        this.router.navigate(['/agent/dashboard']);
                    } else if (data.isFirstLogin) {
                        this.router.navigate(['/user/welcome']);
                    } else {
                        this.router.navigate(['/user/dashboard']);
                    }
                } else {
                    this.errorMessage.set(res.error || 'Login failed');
                }
            },
            error: (err) => {
                this.errorMessage.set(
                    err.error?.error || 'Login failed. Please check your credentials.'
                );
            }
        });
    }
}
