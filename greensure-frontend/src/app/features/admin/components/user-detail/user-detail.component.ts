import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../../features/admin/services/admin.service';
import { UserProfile } from '../../../../core/models/user';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
    selector: 'app-user-detail',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './user-detail.component.html'
})
export class UserDetailComponent implements OnInit {
    private adminService = inject(AdminService);
    private route = inject(ActivatedRoute);
    private toast = inject(ToastService);

    userId = signal<number>(0);
    user = signal<UserProfile | null>(null);

    error = signal<string>('');
    actioning = signal<boolean>(false);
    confirmModalOpen = signal<boolean>(false);
    confirmTitle = signal<string>('');
    confirmMessage = signal<string>('');
    pendingAction = signal<'toggle' | 'unlock' | null>(null);

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
        if (!this.user()) return;

        const nextStatus = this.user()!.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED';
        this.pendingAction.set('toggle');
        this.confirmTitle.set(nextStatus === 'SUSPENDED' ? 'Suspend User' : 'Activate User');
        this.confirmMessage.set(
            nextStatus === 'SUSPENDED'
                ? 'Are you sure you want to suspend this user account?'
                : 'Are you sure you want to activate this user account?'
        );
        this.confirmModalOpen.set(true);
    }

    unlockResubmission(): void {
        if (!this.user()) return;

        this.pendingAction.set('unlock');
        this.confirmTitle.set('Unlock Resubmission');
        this.confirmMessage.set('Grant an extra resubmission attempt for this user?');
        this.confirmModalOpen.set(true);
    }

    closeConfirmModal(): void {
        this.confirmModalOpen.set(false);
        this.pendingAction.set(null);
        this.confirmTitle.set('');
        this.confirmMessage.set('');
    }

    confirmAction(): void {
        const action = this.pendingAction();
        this.closeConfirmModal();

        if (action === 'toggle') {
            this.executeToggleStatus();
            return;
        }
        if (action === 'unlock') {
            this.executeUnlockResubmission();
        }
    }

    private executeToggleStatus(): void {
        if (!this.user()) return;

        const nextStatus = this.user()!.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED';

        this.actioning.set(true);
        this.adminService.updateUserStatus(this.userId(), nextStatus).subscribe({
            next: (res) => {
                this.actioning.set(false);
                if (res.success) {
                    this.user.update(u => u ? { ...u, status: nextStatus } : null);
                    this.toast.success(nextStatus === 'SUSPENDED' ? 'User suspended successfully.' : 'User activated successfully.');
                } else {
                    this.toast.error('Failed to update status: ' + res.message);
                }
            },
            error: () => {
                this.actioning.set(false);
                this.toast.error('Failed to update status.');
            }
        });
    }

    private executeUnlockResubmission(): void {
        if (!this.user()) return;

        this.actioning.set(true);
        this.adminService.unlockResubmission(this.userId()).subscribe({
            next: (res) => {
                this.actioning.set(false);
                if (res.success) {
                    this.toast.success('User resubmission unlocked successfully.');
                } else {
                    this.toast.error('Failed to unlock resubmission: ' + res.message);
                }
            },
            error: () => {
                this.actioning.set(false);
                this.toast.error('Failed to unlock resubmission.');
            }
        });
    }
}
