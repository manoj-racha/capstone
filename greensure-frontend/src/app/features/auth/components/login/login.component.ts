import { Component, signal, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import {
    LucideAngularModule,
    Leaf,
    AlertTriangle,
    ArrowLeft
} from 'lucide-angular';

@Component({
    selector: 'app-login',
    imports: [FormsModule, RouterLink, LucideAngularModule],
    templateUrl: './login.component.html'
})

export class LoginComponent {

    // ── Form fields ──────────────────────────────────────────
    email = signal('');
    password = signal('');

    // ── UI state ─────────────────────────────────────────────
    errorMessage = signal('');
    isSuspended = signal(false);
    submitted = signal(false);

    // ── Icons ────────────────────────────────────────────────
    readonly Leaf = Leaf;
    readonly AlertTriangle = AlertTriangle;
    readonly ArrowLeft = ArrowLeft;

    private authService: AuthService = inject(AuthService);
    private router: Router = inject(Router);

    // ── LOGIN ────────────────────────────────────────────────
    onLogin(form?: any): void {
        this.submitted.set(true);
        if (form && form.invalid) {
            this.scrollToFirstInvalid();
            return;
        }

        this.errorMessage.set('');
        this.isSuspended.set(false);

        this.authService.login({
            email: this.email(),
            password: this.password()
        }).subscribe({
            next: (res) => {
                if (res.success) {
                    const data = res.data;

                    localStorage.setItem('token', data.token);
                    localStorage.setItem('role', data.role);
                    localStorage.setItem('userId', String(data.id));
                    localStorage.setItem('fullName', data.fullName);
                    localStorage.setItem('userType', data.userType);

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
                    this.handleLoginError(res.error || 'Login failed');
                }
            },
            error: (err) => {
                this.handleLoginError(
                    err.error?.error || 'Login failed. Please check your credentials.'
                );
            }
        });
    }

    private handleLoginError(message: string): void {
        if (message.toLowerCase().includes('suspended')) {
            this.isSuspended.set(true);
        } else {
            this.errorMessage.set(message);
        }
    }

    private scrollToFirstInvalid(): void {
        setTimeout(() => {
            const firstInvalidControl = document.querySelector('.ng-invalid');
            if (firstInvalidControl) {
                firstInvalidControl.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }, 100);
    }
}
