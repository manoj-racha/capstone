import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../../features/admin/services/admin.service';


@Component({
    selector: 'app-reports',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './reports.component.html'
})
export class ReportsComponent implements OnInit {
    private adminService = inject(AdminService);

    // Agent Performance State
    agentPerformance = signal<any[]>([]);

    performanceError = signal<string>('');

    // UI State
    activeTab = signal<'PERFORMANCE'>('PERFORMANCE');

    ngOnInit(): void {
        this.loadAgentPerformance();
    }

    loadAgentPerformance(): void {
        this.adminService.getAgentPerformanceReport().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.agentPerformance.set(res.data);
                } else {
                    this.performanceError.set(res.message || 'Failed to load performance metrics.');
                }
            },
            error: (err) => {
                this.performanceError.set(err.error?.error || 'Failed to load performance data.');
            }
        });
    }

    setActiveTab(tab: 'PERFORMANCE'): void {
        this.activeTab.set(tab);
    }

    // Helpers
    formatPct(val?: number): string {
        return val !== undefined ? val.toFixed(1) + '%' : '0%';
    }
}
