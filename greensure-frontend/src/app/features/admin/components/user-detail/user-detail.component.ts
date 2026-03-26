import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../../features/admin/services/admin.service';
import { UserProfile } from '../../../../core/models/user';

@Component({
    selector: 'app-user-detail',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './user-detail.component.html'
})
export class UserDetailComponent implements OnInit {
    private adminService = inject(AdminService);
    private route = inject(ActivatedRoute);

    userId = signal<number>(0);
    user = signal<UserProfile | null>(null);

    error = signal<string>('');
    actioning = signal<boolean>(false);

    ngOnInit(): void {
        const idParam = this.route.snapshot.paramMap.get('id');
        if (idParam) {
            this.userId.set(Number(idParam));
            this.loadUser();
        } else {
            this.error.set('Invalid user ID.');
        }
    }

    loadUser(): void {
        this.adminService.getUserById(this.userId()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.user.set(res.data);
                } else {
                    this.error.set(res.message || 'Failed to load user profile.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load user profile.');
            }
        });
    }

    toggleStatus(): void {
        /*
        */
    }

    unlockResubmission(): void {
        if (!this.user()) return;

        if (confirm('Are you sure you want to grant an extra resubmission attempt for this user?')) {
            this.actioning.set(true);
            this.adminService.unlockResubmission(this.userId()).subscribe({
                next: (res) => {
                    this.actioning.set(false);
                    if (res.success) {
                        alert('User resubmission unlocked successfully.');
                    } else {
                        alert('Failed to unlock resubmission: ' + res.message);
                    }
                },
                error: (err) => {
                    this.actioning.set(false);
                    alert('Failed to unlock resubmission.');
                }
            });
        }
    }
}
