import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../../features/admin/services/admin.service';
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

        this.adminService.getUsers(undefined, this.statusFilter(), this.currentPage(), this.pageSize()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    const page = res.data as any;
                    this.users.set(Array.isArray(page?.content) ? page.content : []);
                    this.totalElements.set(typeof page?.totalElements === 'number' ? page.totalElements : 0);
                    this.totalPages.set(typeof page?.totalPages === 'number' ? page.totalPages : 0);
                } else {
                    this.error.set(res.message || 'Failed to load users.');
                    this.users.set([]);
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load users.');
                this.users.set([]);
            }
        });
    }

    onFilterChange(type: string, val: string): void {
        this.statusFilter.set(val === 'ALL' ? '' : val);
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
