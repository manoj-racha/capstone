import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AgentService } from '../../../../features/agent/services/agent.service';
import { AgentTaskSummary } from '../../../../core/models/agent';

@Component({
    selector: 'app-task-detail',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './task-detail.component.html'
})
export class TaskDetailComponent implements OnInit {
    private agentService = inject(AgentService);
    private route = inject(ActivatedRoute);
    private router = inject(Router);

    assignmentId = signal<number>(0);
    task = signal<AgentTaskSummary | null>(null);

    starting = signal<boolean>(false);
    error = signal<string>('');

    ngOnInit(): void {
        const idParam = this.route.snapshot.paramMap.get('assignmentId');
        if (idParam) {
            this.assignmentId.set(Number(idParam));
            this.loadTask();
        } else {
            this.error.set('Invalid assignment ID.');
        }
    }

    loadTask(): void {
        this.agentService.getAssignment(this.assignmentId()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.task.set(res.data);
                } else {
                    this.error.set(res.message || 'Failed to load task details');
                }
            },
            error: (err) => {
                this.error.set(err.error?.message || 'Failed to load task details');
            }
        });
    }

    onStartVerification(): void {
        if (!this.task() || this.task()?.status !== 'ACTIVE') {
            // Only active tasks can be verified
            return;
        }
        this.router.navigate(['/agent/workspace', this.assignmentId()]);
    }

    getStatusBadgeClass(status?: string, isOverdue?: boolean): string {
        if (!status) return '';
        if (isOverdue && status !== 'COMPLETED') {
            return 'bg-red-500/20 text-red-500 border-red-500/30';
        }

        switch (status) {
            case 'ACTIVE': return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
            case 'COMPLETED': return 'bg-gs-dark/10 text-gs-dark border-gs-dark/20';
            case 'REASSIGNED': return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
            default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
        }
    }

    getStatusDisplay(status?: string): string {
        return status ? status.replace('_', ' ') : '';
    }
}
