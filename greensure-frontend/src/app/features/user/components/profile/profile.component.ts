import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../../features/user/services/user.service';
import { RouterModule } from '@angular/router';

@Component({
    selector: 'app-profile',
    imports: [ReactiveFormsModule, CommonModule, RouterModule],
    templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
    private userService = inject(UserService);
    private fb = inject(FormBuilder);

    profileForm!: FormGroup;

    saving = signal(false);
    successMessage = signal('');
    errorMessage = signal('');

    ngOnInit(): void {
        this.initForm();
        this.loadProfile();
    }

    private initForm(): void {
        this.profileForm = this.fb.group({
            fullName: ['', Validators.required],
            phone: ['', Validators.required],
            dateOfBirth: [''],
            address: [''],
            state: [''],
            city: [''],
            pincode: ['', [Validators.pattern('^[0-9]{5,6}$')]],
            householdSize: ['', [Validators.min(1)]]
        });
    }

    private loadProfile(): void {
        this.userService.getProfile().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.profileForm.patchValue(res.data);
                } else {
                    this.errorMessage.set(res.message || 'Failed to load profile');
                }
            },
            error: (err) => {
                this.errorMessage.set(err.error?.message || 'Failed to load profile');
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

                    if (res.data.fullName) {
                        localStorage.setItem('fullName', res.data.fullName);
                    }
                } else {
                    this.errorMessage.set(res.message || 'Failed to update profile');
                }
            },
            error: (err) => {
                this.saving.set(false);
                this.errorMessage.set(err.error?.message || 'Failed to update profile');
            }
        });
    }
}
