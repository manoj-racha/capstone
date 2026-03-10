import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
    selector: 'app-reset-password',
    imports: [FormsModule, RouterLink],
    templateUrl: './reset-password.component.html'
})
export class ResetPasswordComponent {

    // The reset token comes from the URL or email link
    token = signal('');
    newPassword = signal('');
    confirmPassword = signal('');


    errorMessage = signal('');
    successMessage = signal('');

    constructor(
        private authService: AuthService,
        private router: Router
    ) { }

    // Sends token + new password to POST /auth/reset-password
    onSubmit(): void {
        this.errorMessage.set('');
        this.successMessage.set('');

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
                    this.errorMessage.set(res.error || 'Reset failed');
                }
            },
            error: (err) => {
                this.errorMessage.set(err.error?.error || 'Reset failed. Token may have expired.');
            }
        });
    }
}
