import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../../features/admin/services/admin.service';
import { AdminAnalytics } from '../../../../core/models/admin';

@Component({
    selector: 'app-admin-dashboard',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './admin-dashboard.component.html'
})
export class AdminDashboardComponent implements OnInit {
    private adminService = inject(AdminService);

    overview = signal<AdminAnalytics | null>(null);

    error = signal<string>('');

    ngOnInit(): void {
        this.adminService.getOverview().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.overview.set(res.data);
                } else {
                    this.error.set(res.message || 'Failed to load dashboard data.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to connect to server.');
            }
        });
    }

    get totalAgents(): number {
        return this.overview()?.agentPerformance?.length || 0;
    }

    // Derived computations
    get flaggedAgents(): number {
        return this.overview()?.agentPerformance?.filter(a => a.strikes >= 3 || !a.active).length || 0;
    }

    get pendingVerifications(): number {
        return this.overview()?.declarationsByStatus?.['SUBMITTED'] || 
               this.overview()?.declarationsByStatus?.['UNDER_VERIFICATION'] || 0;
    }

    get totalScoresGenerated(): number {
        return this.overview()?.declarationsByStatus?.['VERIFIED'] || 0;
    }
}
