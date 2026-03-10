import { Component, signal, computed } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
    selector: 'app-register',
    imports: [FormsModule, RouterLink],
    templateUrl: './register.component.html'
})
export class RegisterComponent {

    // ── User type toggle ─────────────────────────────────────
    // Determines which conditional fields to show
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

    // ── UI state ─────────────────────────────────────────────

    errorMessage = signal('');
    successMessage = signal('');

    constructor(
        private authService: AuthService,
        private router: Router
    ) { }

    // ── REGISTER ─────────────────────────────────────────────
    // Validates passwords match, builds the request object,
    // then calls POST /auth/register.
    onRegister(): void {
        this.errorMessage.set('');
        this.successMessage.set('');

        // Client-side password match check
        if (this.password() !== this.confirmPassword()) {
            this.errorMessage.set('Passwords do not match');
            return;
        }



        // Build request — includes conditional fields based on userType
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

        // Add household-specific fields
        if (this.isHousehold()) {
            request.numberOfMembers = this.numberOfMembers();
            request.dwellingType = this.dwellingType();
        } else {
            // Add MSME-specific fields
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
}
