import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
    selector: 'app-reset-password',
    imports: [FormsModule, RouterLink],
    templateUrl: './reset-password.component.html'
})
export class ResetPasswordComponent implements OnInit {

    // The reset token is auto-read from URL query params (?token=...)
    token = signal('');
    newPassword = signal('');
    confirmPassword = signal('');

    errorMessage = signal('');
    successMessage = signal('');
    tokenExpired = signal(false);

    private authService: AuthService = inject(AuthService);
    private router: Router = inject(Router);
    private route: ActivatedRoute = inject(ActivatedRoute);

    ngOnInit(): void {
        // Auto-read token from URL: /reset-password?token=abc123
        this.route.queryParams.subscribe(params => {
            if (params['token']) {
                this.token.set(params['token']);
            }
        });
    }

    onSubmit(): void {
        this.errorMessage.set('');
        this.successMessage.set('');
        this.tokenExpired.set(false);

        if (!this.token()) {
            this.errorMessage.set('No reset token found. Please use the link from your email.');
            return;
        }

        if (this.newPassword() !== this.confirmPassword()) {
            this.errorMessage.set('Passwords do not match');
            return;
        }

        if (this.newPassword().length < 8) {
            this.errorMessage.set('Password must be at least 8 characters');
            return;
        }

        this.authService.resetPassword(this.token(), this.newPassword()).subscribe({
            next: (res) => {
                if (res.success) {
                    this.successMessage.set('Password reset successfully! Redirecting to login...');
                    setTimeout(() => this.router.navigate(['/login']), 2000);
                } else {
                    this.handleResetError(res.error || 'Reset failed');
                }
            },
            error: (err) => {
                this.handleResetError(err.error?.error || 'Reset failed. Token may have expired.');
            }
        });
    }

    private handleResetError(message: string): void {
        if (message.toLowerCase().includes('expired')) {
            this.tokenExpired.set(true);
        } else {
            this.errorMessage.set(message);
        }
    }
}
