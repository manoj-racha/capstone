import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../../core/services/admin.service';
import { AdminOverview } from '../../../../core/models/admin';

@Component({
    selector: 'app-admin-dashboard',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './admin-dashboard.component.html'
})
export class AdminDashboardComponent implements OnInit {
    private adminService = inject(AdminService);

    overview = signal<AdminOverview | null>(null);

    error = signal<string>('');

    ngOnInit(): void {
        this.adminService.getOverview().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.overview.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load dashboard data.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to connect to server.');
            }
        });
    }
}
