import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../../core/services/admin.service';
import { UserProfile } from '../../../../core/models/user';

@Component({
    selector: 'app-users',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './users.component.html'
})
export class UsersComponent implements OnInit {
    private adminService = inject(AdminService);

    users = signal<UserProfile[]>([]);

    error = signal<string>('');

    // Filter state
    userTypeFilter = signal<string>('');
    statusFilter = signal<string>('');

    ngOnInit(): void {
        this.loadUsers();
    }

    loadUsers(): void {

        this.error.set('');

        const uType = this.userTypeFilter() || undefined;
        const stat = this.statusFilter() || undefined;

        this.adminService.getUsers(uType, stat).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.users.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load users.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load users.');
            }
        });
    }

    onFilterChange(type: string, val: string): void {
        if (type === 'type') {
            this.userTypeFilter.set(val === 'ALL' ? '' : val);
        } else {
            this.statusFilter.set(val === 'ALL' ? '' : val);
        }
        this.loadUsers();
    }
}
