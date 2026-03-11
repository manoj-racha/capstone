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

    // Pagination state
    currentPage = signal<number>(0);
    pageSize = signal<number>(10);
    totalElements = signal<number>(0);
    totalPages = signal<number>(0);

    ngOnInit(): void {
        this.loadUsers();
    }

    loadUsers(): void {

        this.error.set('');

        const uType = this.userTypeFilter() || undefined;
        const stat = this.statusFilter() || undefined;

        this.adminService.getUsers(uType, stat, this.currentPage(), this.pageSize()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.users.set(res.data.content);
                    this.totalElements.set(res.data.totalElements);
                    this.totalPages.set(res.data.totalPages);
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
        this.currentPage.set(0); // Reset to first page on filter change
        this.loadUsers();
    }

    onPageChange(page: number): void {
        if (page >= 0 && page < this.totalPages()) {
            this.currentPage.set(page);
            this.loadUsers();
        }
    }
}
