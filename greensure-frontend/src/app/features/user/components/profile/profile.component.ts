import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../../core/services/user.service';

@Component({
    selector: 'app-profile',
    imports: [ReactiveFormsModule, CommonModule],
    templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
    private userService = inject(UserService);
    private fb = inject(FormBuilder);

    profileForm!: FormGroup;

    saving = signal(false);
    successMessage = signal('');
    errorMessage = signal('');
    userType = signal('');

    ngOnInit(): void {
        // Initial empty form, wait for profile load
        this.initForm();
        this.loadProfile();
    }

    private initForm(): void {
        this.profileForm = this.fb.group({
            fullName: ['', Validators.required],
            mobile: ['', Validators.required],
            address: ['', Validators.required],
            city: ['', Validators.required],
            pinCode: ['', [Validators.required, Validators.pattern('^[0-9]{5,6}$')]],
            // Household
            numberOfMembers: [''],
            dwellingType: [''],
            // MSME
            businessName: [''],
            industrySector: [''],
            employeeCount: [''],
            facilityAreaSqFt: ['']
        });
    }

    private loadProfile(): void {
        this.userService.getProfile().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.userType.set(res.data.userType);
                    this.profileForm.patchValue(res.data);

                    // Add mandatory validators dynamically based on user type
                    if (res.data.userType === 'HOUSEHOLD') {
                        this.profileForm.get('numberOfMembers')?.setValidators([Validators.required, Validators.min(1)]);
                    } else if (res.data.userType === 'MSME') {
                        this.profileForm.get('businessName')?.setValidators(Validators.required);
                        this.profileForm.get('industrySector')?.setValidators(Validators.required);
                        this.profileForm.get('employeeCount')?.setValidators([Validators.required, Validators.min(1)]);
                    }
                    this.profileForm.updateValueAndValidity();
                } else {
                    this.errorMessage.set(res.error || 'Failed to load profile');
                }
            },
            error: (err) => {
                this.errorMessage.set(err.error?.error || 'Failed to load profile');
            }
        });
    }

    onSubmit(): void {
        if (this.profileForm.invalid) {
            this.profileForm.markAllAsTouched();
            return;
        }

        this.saving.set(true);
        this.successMessage.set('');
        this.errorMessage.set('');

        this.userService.updateProfile(this.profileForm.value).subscribe({
            next: (res) => {
                this.saving.set(false);
                if (res.success && res.data) {
                    this.successMessage.set('Profile updated successfully!');
                    this.profileForm.patchValue(res.data);

                    // Update fullname in localstorage if it changed
                    if (res.data.fullName) {
                        localStorage.setItem('fullName', res.data.fullName);
                    }
                } else {
                    this.errorMessage.set(res.error || 'Failed to update profile');
                }
            },
            error: (err) => {
                this.saving.set(false);
                this.errorMessage.set(err.error?.error || 'Failed to update profile');
            }
        });
    }
}
