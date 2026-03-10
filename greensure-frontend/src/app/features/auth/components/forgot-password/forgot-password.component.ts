import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
    selector: 'app-forgot-password',
    imports: [FormsModule, RouterLink],
    templateUrl: './forgot-password.component.html'
})
export class ForgotPasswordComponent {

    email = signal('');

    errorMessage = signal('');
    successMessage = signal('');

    constructor(private authService: AuthService) { }

    // Sends email to POST /auth/forgot-password
    // Backend sends a reset link/OTP to the user's email
    onSubmit(): void {
        this.errorMessage.set('');
        this.successMessage.set('');

        this.authService.forgotPassword(this.email()).subscribe({
            next: (res) => {
                if (res.success) {
                    this.successMessage.set(res.message || 'If this email exists, a reset link has been sent.');
                } else {
                    this.errorMessage.set(res.error || 'Something went wrong');
                }
            },
            error: (err) => {
                this.errorMessage.set(err.error?.error || 'Something went wrong. Please try again.');
            }
        });
    }
}
