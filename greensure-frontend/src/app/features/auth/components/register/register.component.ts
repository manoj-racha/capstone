import { Component, signal, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import {
    LucideAngularModule,
    Leaf,
    AlertTriangle,
    CheckCircle,
    ArrowLeft
} from 'lucide-angular';

@Component({
    selector: 'app-register',
    imports: [FormsModule, RouterLink, LucideAngularModule],
    templateUrl: './register.component.html'
})
export class RegisterComponent {

    // ── User type toggle ─────────────────────────────────────
    userType = signal<'HOUSEHOLD' | 'MSME'>('HOUSEHOLD');
    isHousehold = computed(() => this.userType() === 'HOUSEHOLD');

    // ── Common fields ────────────────────────────────────────
    fullName = signal('');
    email = signal('');
    mobile = signal('');
    password = signal('');
    confirmPassword = signal('');
    address = signal('');
    pinCode = signal('');
    city = signal('');
    state = signal('');

    // ── Household-only fields ────────────────────────────────
    numberOfMembers = signal<number | null>(null);
    dwellingType = signal('');

    // ── MSME-only fields ─────────────────────────────────────
    businessName = signal('');
    gstNumber = signal('');
    businessType = signal('');
    numEmployees = signal<number | null>(null);

    // ── Icons ────────────────────────────────────────────────
    readonly Leaf = Leaf;
    readonly AlertTriangle = AlertTriangle;
    readonly CheckCircle = CheckCircle;
    readonly ArrowLeft = ArrowLeft;

    // ── Touched state for validation ─────────────────────────
    pinCodeTouched = signal(false);
    dwellingTypeTouched = signal(false);

    // ── Computed validation ──────────────────────────────────
    pinCodeError = computed(() => {
        if (!this.pinCodeTouched()) return '';
        const val = this.pinCode();
        if (!val) return 'Pin code is required';
        if (!/^[0-9]{6}$/.test(val)) return 'Enter a valid 6-digit pin code';
        return '';
    });

    dwellingTypeError = computed(() => {
        if (!this.dwellingTypeTouched()) return '';
        if (!this.dwellingType()) return 'Please select your dwelling type';
        return '';
    });

    // ── UI state ─────────────────────────────────────────────
    errorMessage = signal('');
    successMessage = signal('');
    submitted = signal(false);

    private authService: AuthService = inject(AuthService);
    private router: Router = inject(Router);

    // ── REGISTER ─────────────────────────────────────────────
    onRegister(form?: any): void {
        this.submitted.set(true);
        this.errorMessage.set('');
        this.successMessage.set('');

        // Trigger touched for inline validations
        this.pinCodeTouched.set(true);
        if (this.isHousehold()) {
            this.dwellingTypeTouched.set(true);
        }

        if (form && form.invalid) {
            this.errorMessage.set('Please fill out all required fields correctly.');
            this.scrollToFirstInvalid();
            return;
        }

        // Client-side password match check
        if (this.password() !== this.confirmPassword()) {
            this.errorMessage.set('Passwords do not match');
            return;
        }

        // Client-side pin code check
        if (this.pinCodeError()) {
            this.errorMessage.set(this.pinCodeError());
            this.scrollToFirstInvalid();
            return;
        }

        // Client-side dwelling type check
        if (this.isHousehold() && this.dwellingTypeError()) {
            this.errorMessage.set(this.dwellingTypeError());
            this.scrollToFirstInvalid();
            return;
        }

        // Build request
        const request: any = {
            userType: this.userType(),
            fullName: this.fullName(),
            email: this.email(),
            mobile: this.mobile(),
            password: this.password(),
            address: this.address(),
            pinCode: this.pinCode(),
            city: this.city(),
            state: this.state()
        };

        if (this.isHousehold()) {
            request.numberOfMembers = this.numberOfMembers();
            request.dwellingType = this.dwellingType();
        } else {
            request.businessName = this.businessName();
            request.gstNumber = this.gstNumber();
            request.businessType = this.businessType();
            request.numEmployees = this.numEmployees();
        }

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
            const firstInvalidControl = document.querySelector('.ng-invalid');
            if (firstInvalidControl) {
                firstInvalidControl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
        }, 100);
    }
}