import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../../core/services/admin.service';

@Component({
    selector: 'app-agent-detail',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './agent-detail.component.html'
})
export class AgentDetailComponent implements OnInit {
    private adminService = inject(AdminService);
    private route = inject(ActivatedRoute);
    private router = inject(Router);

    agentId = signal<number>(0);
    agent = signal<any | null>(null);

    error = signal<string>('');
    actioning = signal<boolean>(false);

    ngOnInit(): void {
        const idParam = this.route.snapshot.paramMap.get('id');
        if (idParam) {
            this.agentId.set(Number(idParam));
            this.loadAgent();
        } else {
            this.error.set('Invalid agent ID.');
        }
    }

    loadAgent(): void {
        this.adminService.getAgentById(this.agentId()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.agent.set(res.data);
                } else {
                    this.error.set(res.error || 'Agent not found.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load agent details.');
            }
        });
    }

    toggleStatus(): void {
        if (!this.agent()) return;

        this.actioning.set(true);
        const newStatus = this.agent()?.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';

        this.adminService.updateAgentStatus(this.agentId(), newStatus).subscribe({
            next: (res) => {
                this.actioning.set(false);
                if (res.success) {
                    this.agent.update(a => a ? { ...a, status: newStatus } : null);
                } else {
                    alert('Failed to update status: ' + res.error);
                }
            },
            error: (err) => {
                this.actioning.set(false);
                alert('Failed to update status.');
            }
        });
    }

    clearStrikes(): void {
        if (!this.agent() || this.agent()?.strikeCount === 0) return;

        if (confirm('Are you sure you want to clear all strikes for this agent?')) {
            this.actioning.set(true);
            this.adminService.clearStrikes(this.agentId()).subscribe({
                next: (res) => {
                    this.actioning.set(false);
                    if (res.success) {
                        this.agent.update(a => a ? { ...a, strikeCount: 0 } : null);
                        alert('Agent strikes cleared successfully.');
                    } else {
                        alert('Failed to clear strikes: ' + res.error);
                    }
                },
                error: (err) => {
                    this.actioning.set(false);
                    alert('Failed to clear strikes.');
                }
            });
        }
    }
}
