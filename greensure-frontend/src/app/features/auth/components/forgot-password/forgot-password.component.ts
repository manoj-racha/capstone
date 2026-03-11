import { Component, signal, computed, inject } from '@angular/core';
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
    emailSent = signal(false);

    // Mask the email for confirmation display: j***@example.com
    maskedEmail = computed(() => {
        const e = this.email();
        if (!e.includes('@')) return e;
        const [local, domain] = e.split('@');
        const masked = local.charAt(0) + '***';
        return masked + '@' + domain;
    });

    private authService: AuthService = inject(AuthService);

    onSubmit(): void {
        this.errorMessage.set('');
        this.emailSent.set(false);

        if (!this.email()) {
            this.errorMessage.set('Please enter your email address');
            return;
        }

        this.authService.forgotPassword(this.email()).subscribe({
            next: () => {
                this.emailSent.set(true);
            },
            error: () => {
                // Always show success to prevent email enumeration
                this.emailSent.set(true);
            }
        });
    }
}
