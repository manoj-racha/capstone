import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgentService } from '../../../../core/services/agent.service';
import { AgentPerformanceResponse } from '../../../../core/models/agent';

@Component({
    selector: 'app-performance',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './performance.component.html'
})
export class PerformanceComponent implements OnInit {
    private agentService = inject(AgentService);

    performance = signal<AgentPerformanceResponse | null>(null);

    error = signal<string>('');

    ngOnInit(): void {
        this.agentService.getPerformance().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.performance.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load performance metrics.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load performance metrics.');
            }
        });
    }

    // Helper to safely format percentages
    formatPct(val?: number): string {
        return val !== undefined ? val.toFixed(1) + '%' : '0%';
    }
}
