import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AgentService } from '../../../../core/services/agent.service';
import { AgentTaskResponse } from '../../../../core/models/agent';

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
    task = signal<AgentTaskResponse | null>(null);

    starting = signal<boolean>(false);
    error = signal<string>('');

    ngOnInit(): void {
        const idParam = this.route.snapshot.paramMap.get('id');
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
                    this.error.set(res.error || 'Failed to load task details.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load task details.');
            }
        });
    }

    onStartVerification(): void {
        if (!this.task() || this.task()?.status !== 'ASSIGNED') {
            // If already started, just jump to verify page
            this.router.navigate(['/agent/verify', this.assignmentId()]);
            return;
        }

        this.starting.set(true);
        this.error.set('');

        this.agentService.startAssignment(this.assignmentId()).subscribe({
            next: (res) => {
                this.starting.set(false);
                if (res.success) {
                    this.router.navigate(['/agent/verify', this.assignmentId()]);
                } else {
                    this.error.set(res.error || 'Failed to start verification.');
                }
            },
            error: (err) => {
                this.starting.set(false);
                this.error.set(err.error?.error || 'Failed to start verification.');
            }
        });
    }

    getStatusBadgeClass(status?: string, isOverdue?: boolean): string {
        if (!status) return '';
        if (isOverdue && status !== 'COMPLETED') {
            return 'bg-red-500/20 text-red-500 border-red-500/30';
        }

        switch (status) {
            case 'ASSIGNED': return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
            case 'IN_PROGRESS': return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
            case 'COMPLETED': return 'bg-gs-green/20 text-gs-green border-gs-green/30';
            case 'REASSIGNED': return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
            default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
        }
    }

    getStatusDisplay(status?: string): string {
        return status ? status.replace('_', ' ') : '';
    }
}
