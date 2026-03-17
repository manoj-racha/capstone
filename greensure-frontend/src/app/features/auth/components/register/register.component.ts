import { Component, signal, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { AuthService } from '../../../../features/auth/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import {
    LucideAngularModule,
    Leaf,
    AlertTriangle,
    CheckCircle,
    ArrowLeft
} from 'lucide-angular';

@Component({
    selector: 'app-register',
    imports: [ReactiveFormsModule, RouterLink, LucideAngularModule],
    templateUrl: './register.component.html'
})
export class RegisterComponent {

    // ── User type toggle ─────────────────────────────────────
    userType = signal<'HOUSEHOLD' | 'MSME'>('HOUSEHOLD');

    // ── Icons ────────────────────────────────────────────────
    readonly Leaf = Leaf;
    readonly AlertTriangle = AlertTriangle;
    readonly CheckCircle = CheckCircle;
    readonly ArrowLeft = ArrowLeft;

    // ── UI state ─────────────────────────────────────────────
    errorMessage = signal('');
    successMessage = signal('');
    submitted = signal(false);

    private fb = inject(FormBuilder);
    private authService: AuthService = inject(AuthService);
    private router: Router = inject(Router);
    private toastService = inject(ToastService);

    registerForm = this.fb.group(
        {
            userType: ['HOUSEHOLD', [Validators.required]],
            fullName: ['', [Validators.required]],
            email: ['', [Validators.required, Validators.email]],
            mobile: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
            address: ['', [Validators.required]],
            pinCode: ['', [Validators.required, Validators.pattern(/^[1-9]\d{5}$/)]],
            city: ['', [Validators.required]],
            state: ['', [Validators.required]],

            numberOfMembers: [null as number | null],
            dwellingType: [''],

            businessName: [''],
            gstNumber: [''],
            businessType: [''],
            numEmployees: [null as number | null],

            password: ['', [Validators.required, Validators.minLength(8)]],
            confirmPassword: ['', [Validators.required]]
        },
        { validators: this.passwordsMatchValidator() }
    );

    constructor() {
        this.updateDynamicValidators('HOUSEHOLD');
    }

    get isHousehold(): boolean {
        return this.userType() === 'HOUSEHOLD';
    }

    setUserType(type: 'HOUSEHOLD' | 'MSME'): void {
        this.userType.set(type);
        this.registerForm.controls.userType.setValue(type);
        this.updateDynamicValidators(type);
    }

    private updateDynamicValidators(type: 'HOUSEHOLD' | 'MSME'): void {
        const numberOfMembers = this.registerForm.controls.numberOfMembers;
        const dwellingType = this.registerForm.controls.dwellingType;
        const businessName = this.registerForm.controls.businessName;
        const gstNumber = this.registerForm.controls.gstNumber;
        const businessType = this.registerForm.controls.businessType;
        const numEmployees = this.registerForm.controls.numEmployees;

        if (type === 'HOUSEHOLD') {
            numberOfMembers.setValidators([Validators.required, Validators.min(1), Validators.max(20)]);
            dwellingType.setValidators([Validators.required]);

            businessName.clearValidators();
            gstNumber.clearValidators();
            businessType.clearValidators();
            numEmployees.clearValidators();

            businessName.setValue('');
            gstNumber.setValue('');
            businessType.setValue('');
            numEmployees.setValue(null);
        } else {
            businessName.setValidators([Validators.required]);
            gstNumber.setValidators([
                Validators.required,
                Validators.pattern(/^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/)
            ]);
            businessType.setValidators([Validators.required]);
            numEmployees.setValidators([Validators.required, Validators.min(1)]);

            numberOfMembers.clearValidators();
            dwellingType.clearValidators();

            numberOfMembers.setValue(null);
            dwellingType.setValue('');
        }

        numberOfMembers.updateValueAndValidity();
        dwellingType.updateValueAndValidity();
        businessName.updateValueAndValidity();
        gstNumber.updateValueAndValidity();
        businessType.updateValueAndValidity();
        numEmployees.updateValueAndValidity();
    }

    private passwordsMatchValidator(): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const password = control.get('password')?.value;
            const confirmPassword = control.get('confirmPassword')?.value;
            if (!password || !confirmPassword) {
                return null;
            }
            return password === confirmPassword ? null : { passwordsMismatch: true };
        };
    }

    // ── REGISTER ─────────────────────────────────────────────
    onRegister(): void {
        this.submitted.set(true);
        this.errorMessage.set('');
        this.successMessage.set('');

        if (this.registerForm.invalid) {
            this.registerForm.markAllAsTouched();
            this.errorMessage.set('Please fill out all required fields correctly.');
            this.toastService.warning('Please fix the highlighted fields before creating your account.');
            this.scrollToFirstInvalid();
            return;
        }

        const baseRequest = {
            userType: this.userType(),
            fullName: this.registerForm.controls.fullName.value?.trim() ?? '',
            email: this.registerForm.controls.email.value?.trim() ?? '',
            mobile: this.registerForm.controls.mobile.value?.trim() ?? '',
            address: this.registerForm.controls.address.value?.trim() ?? '',
            pinCode: this.registerForm.controls.pinCode.value?.trim() ?? '',
            city: this.registerForm.controls.city.value?.trim() ?? '',
            state: this.registerForm.controls.state.value?.trim() ?? '',
            password: this.registerForm.controls.password.value ?? ''
        };

        const request: any = this.isHousehold
            ? {
                ...baseRequest,
                numberOfMembers: this.registerForm.controls.numberOfMembers.value,
                dwellingType: this.registerForm.controls.dwellingType.value
            }
            : {
                ...baseRequest,
                businessName: this.registerForm.controls.businessName.value?.trim() ?? '',
                gstNumber: (this.registerForm.controls.gstNumber.value?.trim() ?? '').toUpperCase(),
                businessType: (this.registerForm.controls.businessType.value ?? '').toUpperCase(),
                numEmployees: this.registerForm.controls.numEmployees.value
            };

        this.authService.register(request).subscribe({
            next: (res) => {
                if (res.success) {
                    this.successMessage.set('Registration successful! Redirecting to login...');
                    setTimeout(() => this.router.navigate(['/login']), 2000);
                } else {
                    this.errorMessage.set(res.error || 'Registration failed');
                }
            },
            error: (err) => {
                this.errorMessage.set(
                    err.error?.error || 'Registration failed. Please try again.'
                );
            }
        });
    }

    private scrollToFirstInvalid(): void {
        setTimeout(() => {
            const firstInvalidControl = document.querySelector('form .ng-invalid');
            if (firstInvalidControl) {
                firstInvalidControl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                (firstInvalidControl as HTMLElement).focus();
            }
        }, 100);
    }

    isInvalid(controlName: string): boolean {
        const control = this.registerForm.get(controlName);
        return !!control && control.invalid && (control.dirty || control.touched || this.submitted());
    }
}