import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AgentService } from '../../../../features/agent/services/agent.service';
import { AgentPerformance } from '../../../../core/models/agent';


@Component({
    selector: 'app-performance',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './performance.component.html'
})
export class PerformanceComponent implements OnInit {
    private agentService = inject(AgentService);

    performance = signal<AgentPerformance | null>(null);
    error = signal<string>('');

    ngOnInit(): void {
        this.agentService.getPerformance().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.performance.set(res.data);
                } else {
                    this.error.set(res.message || 'Failed to load performance data.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load performance metrics.');
            }
        });
    }

    get completionRate(): number {
        const p = this.performance();
        if (!p || p.totalAssignments === 0) return 0;
        return Math.round((p.completedAssignments / p.totalAssignments) * 100);
    }
}
