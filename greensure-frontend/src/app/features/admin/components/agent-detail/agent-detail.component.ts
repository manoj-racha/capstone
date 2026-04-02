import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../../features/admin/services/admin.service';
import { ToastService } from '../../../../core/services/toast.service';

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
    private toast = inject(ToastService);

    agentId = signal<number>(0);
    agent = signal<any | null>(null);

    error = signal<string>('');
    actioning = signal<boolean>(false);
    confirmModalOpen = signal<boolean>(false);
    confirmTitle = signal<string>('');
    confirmMessage = signal<string>('');
    pendingAction = signal<'toggle' | 'clear' | null>(null);

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
                    this.error.set(res.message || 'Agent not found.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load agent details.');
            }
        });
    }

    toggleStatus(): void {
        if (!this.agent()) return;

        const newStatus = this.agent()?.active ? 'SUSPENDED' : 'ACTIVE';
        this.pendingAction.set('toggle');
        this.confirmTitle.set(newStatus === 'SUSPENDED' ? 'Suspend Agent' : 'Activate Agent');
        this.confirmMessage.set(
            newStatus === 'SUSPENDED'
                ? 'Are you sure you want to suspend this agent account?'
                : 'Are you sure you want to activate this agent account?'
        );
        this.confirmModalOpen.set(true);
    }

    clearStrikes(): void {
        if (!this.agent() || this.agent()?.strikes === 0) return;

        this.pendingAction.set('clear');
        this.confirmTitle.set('Clear Strikes');
        this.confirmMessage.set('Are you sure you want to clear all strikes for this agent?');
        this.confirmModalOpen.set(true);
    }

    closeConfirmModal(): void {
        this.confirmModalOpen.set(false);
        this.pendingAction.set(null);
        this.confirmTitle.set('');
        this.confirmMessage.set('');
    }

    confirmAction(): void {
        const action = this.pendingAction();
        this.closeConfirmModal();

        if (action === 'toggle') {
            this.executeToggleStatus();
            return;
        }
        if (action === 'clear') {
            this.executeClearStrikes();
        }
    }

    private executeToggleStatus(): void {
        if (!this.agent()) return;

        this.actioning.set(true);
        // Backend expects 'ACTIVE' or 'SUSPENDED'
        const newStatus = this.agent()?.active ? 'SUSPENDED' : 'ACTIVE';

        this.adminService.updateAgentStatus(this.agentId(), newStatus).subscribe({
            next: (res) => {
                this.actioning.set(false);
                if (res.success) {
                    // Update frontend model boolean
                    this.agent.update(a => a ? { ...a, active: newStatus === 'ACTIVE' } : null);
                    this.toast.success(newStatus === 'SUSPENDED' ? 'Agent suspended successfully.' : 'Agent activated successfully.');
                } else {
                    this.toast.error('Failed to update status: ' + res.message);
                }
            },
            error: () => {
                this.actioning.set(false);
                this.toast.error('Failed to update status.');
            }
        });
    }

    private executeClearStrikes(): void {
        if (!this.agent() || this.agent()?.strikes === 0) return;

        this.actioning.set(true);
        this.adminService.clearStrikes(this.agentId()).subscribe({
            next: (res) => {
                this.actioning.set(false);
                if (res.success) {
                    this.agent.update(a => a ? { ...a, strikes: 0 } : null);
                    this.toast.success('Agent strikes cleared successfully.');
                } else {
                    this.toast.error('Failed to clear strikes: ' + res.message);
                }
            },
            error: () => {
                this.actioning.set(false);
                this.toast.error('Failed to clear strikes.');
            }
        });
    }
}
