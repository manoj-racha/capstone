import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AgentService } from '../../../../core/services/agent.service';
import { AgentTaskResponse } from '../../../../core/models/agent';

@Component({
    selector: 'app-agent-dashboard',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './agent-dashboard.component.html'
})
export class AgentDashboardComponent implements OnInit {
    private agentService = inject(AgentService);

    tasks = signal<AgentTaskResponse[]>([]);

    error = signal<string>('');

    // Filter state
    currentFilter = signal<string>('ALL');

    ngOnInit(): void {
        this.loadTasks();
    }

    loadTasks(status?: string): void {
        this.error.set('');

        this.currentFilter.set(status || 'ALL');

        this.agentService.getAssignments(status).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.tasks.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load assignments.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load assignments.');
            }
        });
    }

    getStatusBadgeClass(status: string, isOverdue: boolean): string {
        if (isOverdue && status !== 'COMPLETED') {
            return 'bg-red-500/20 text-red-500 border-red-500/30 font-bold animate-pulse';
        }

        switch (status) {
            case 'ASSIGNED': return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
            case 'IN_PROGRESS': return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
            case 'COMPLETED': return 'bg-gs-dark/10 text-gs-dark border-gs-dark/20';
            case 'REASSIGNED': return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
            default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
        }
    }

    getStatusDisplay(status: string): string {
        return status.replace('_', ' ');
    }
}
