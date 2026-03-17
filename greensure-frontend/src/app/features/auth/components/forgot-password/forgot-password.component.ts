import { Component, signal, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../features/auth/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
    selector: 'app-forgot-password',
    imports: [ReactiveFormsModule, RouterLink],
    templateUrl: './forgot-password.component.html'
})
export class ForgotPasswordComponent {

    emailSent = signal(false);
    submitted = signal(false);

    private fb = inject(FormBuilder);
    private authService: AuthService = inject(AuthService);
    private toast = inject(ToastService);

    form = this.fb.group({
        email: ['', [Validators.required, Validators.email]]
    });

    onSubmit(): void {
        this.submitted.set(true);

        if (this.form.invalid) {
            this.form.markAllAsTouched();
            this.scrollToFirstInvalid();
            return;
        }

        const email = this.form.controls.email.value || '';

        this.authService.forgotPassword(email).subscribe({
            next: () => this.emailSent.set(true),
            error: () => {
                this.toast.error('Unable to send reset request right now. Please try again.');
            }
        });
    }

    isInvalid(field: 'email'): boolean {
        const control = this.form.controls[field];
        return control.invalid && (control.touched || this.submitted());
    }

    private scrollToFirstInvalid(): void {
        setTimeout(() => {
            const firstInvalid = document.querySelector('form .ng-invalid');
            if (firstInvalid) {
                firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                (firstInvalid as HTMLElement).focus();
            }
        }, 100);
    }
}
