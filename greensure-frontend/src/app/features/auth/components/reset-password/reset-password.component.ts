import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../features/auth/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
    selector: 'app-reset-password',
    imports: [ReactiveFormsModule, RouterLink],
    templateUrl: './reset-password.component.html'
})
export class ResetPasswordComponent implements OnInit {

    token = signal('');
    tokenExpired = signal(false);
    submitted = signal(false);
    submitting = signal(false);

    private fb = inject(FormBuilder);
    private authService: AuthService = inject(AuthService);
    private toast = inject(ToastService);
    private router: Router = inject(Router);
    private route: ActivatedRoute = inject(ActivatedRoute);

    form = this.fb.group({
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]]
    });

    ngOnInit(): void {
        this.route.queryParams.subscribe(params => {
            if (params['token']) {
                this.token.set(params['token']);
            }
        });
    }

    onSubmit(): void {
        this.submitted.set(true);
        this.tokenExpired.set(false);

        if (!this.token()) {
            this.tokenExpired.set(true);
            return;
        }

        if (this.form.invalid || this.passwordMismatch()) {
            this.form.markAllAsTouched();
            this.scrollToFirstError();
            return;
        }

        const newPassword = this.form.controls.newPassword.value || '';

        this.submitting.set(true);
        this.authService.resetPassword({ token: this.token(), newPassword }).subscribe({
            next: (res) => {
                this.submitting.set(false);
                if (!res.success) {
                    this.handleResetError(res.error || 'Unable to reset password.');
                    return;
                }

                this.toast.success('Password reset successfully. Please login.');
                setTimeout(() => this.router.navigate(['/login']), 2000);
            },
            error: (err) => {
                this.submitting.set(false);
                this.handleResetError(err.error?.error || 'Unable to reset password.');
            }
        });
    }

    passwordMismatch(): boolean {
        const password = this.form.controls.newPassword.value || '';
        const confirm = this.form.controls.confirmPassword.value || '';
        return !!password && !!confirm && password !== confirm;
    }

    isInvalid(field: 'newPassword' | 'confirmPassword'): boolean {
        const control = this.form.controls[field];
        return control.invalid && (control.touched || this.submitted());
    }

    private handleResetError(message: string): void {
        const normalized = message.toLowerCase();
        if (normalized.includes('invalid reset link') || normalized.includes('expired')) {
            this.tokenExpired.set(true);
        } else {
            this.toast.error(message);
        }
    }

    private scrollToFirstError(): void {
        setTimeout(() => {
            const firstInvalid = document.querySelector('form .ng-invalid');
            if (firstInvalid) {
                firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                (firstInvalid as HTMLElement).focus();
            }
        }, 100);
    }
}
