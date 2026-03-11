import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../../core/services/admin.service';
import { AgentPerformanceResponse } from '../../../../core/models/agent';

@Component({
    selector: 'app-reports',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './reports.component.html'
})
export class ReportsComponent implements OnInit {
    private adminService = inject(AdminService);

    // Agent Performance State
    agentPerformance = signal<AgentPerformanceResponse[]>([]);

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
                    // Add default fallback for name/id if they don't exist since it's a list summary
                    // and might be missing base agent profile details depending on backend structure.
                    this.agentPerformance.set([res.data]);
                } else {
                    this.performanceError.set(res.error || 'Failed to load performance data.');
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
