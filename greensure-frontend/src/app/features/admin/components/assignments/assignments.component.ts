import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../../features/admin/services/admin.service';
import { AgentTaskResponse } from '../../../../core/models/agent';
import { AvailableAgent, UnassignedDeclaration } from '../../../../core/models/admin';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
    selector: 'app-assignments',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './assignments.component.html'
})
export class AssignmentsComponent implements OnInit {
    private adminService = inject(AdminService);
    private toast = inject(ToastService);

    activeTab = signal<'UNASSIGNED' | 'ACTIVE'>('UNASSIGNED');
    unassignedDeclarations = signal<UnassignedDeclaration[]>([]);
    activeAssignments = signal<AgentTaskResponse[]>([]);
    availableAgents = signal<AvailableAgent[]>([]);

    error = signal('');
    loading = signal(false);
    actioning = signal(false);

    assignModalOpen = signal(false);
    selectedDeclaration = signal<UnassignedDeclaration | null>(null);
    selectedAgentId = signal<number | null>(null);

    reassignModalOpen = signal(false);
    selectedAssignment = signal<AgentTaskResponse | null>(null);
    reassignAgentId = signal<number | null>(null);
    reassignReason = signal('');

    cancelConfirmOpen = signal(false);

    ngOnInit(): void {
        this.loadPage();
        this.loadAvailableAgents();
    }

    setTab(tab: 'UNASSIGNED' | 'ACTIVE'): void {
        this.activeTab.set(tab);
        this.loadPage();
    }

    loadPage(): void {
        this.error.set('');
        this.loading.set(true);

        if (this.activeTab() === 'UNASSIGNED') {
            this.adminService.getUnassignedDeclarations().subscribe({
                next: (res) => {
                    this.loading.set(false);
                    if (res.success && res.data) {
                        this.unassignedDeclarations.set(res.data);
                    } else {
                        this.error.set(res.error || 'Failed to load unassigned declarations.');
                    }
                },
                error: (err) => {
                    this.loading.set(false);
                    this.error.set(err.error?.error || 'Failed to load unassigned declarations.');
                }
            });
            return;
        }

        this.adminService.getAssignedDeclarations().subscribe({
            next: (res) => {
                this.loading.set(false);
                if (res.success && res.data) {
                    this.activeAssignments.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load active assignments.');
                }
            },
            error: (err) => {
                this.loading.set(false);
                this.error.set(err.error?.error || 'Failed to load active assignments.');
            }
        });
    }

    loadAvailableAgents(): void {
        this.adminService.getAvailableAgents().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.availableAgents.set(res.data);
                }
            }
        });
    }

    openAssignModal(declaration: UnassignedDeclaration): void {
        this.selectedDeclaration.set(declaration);
        this.selectedAgentId.set(null);
        this.assignModalOpen.set(true);
    }

    closeAssignModal(): void {
        this.assignModalOpen.set(false);
        this.selectedDeclaration.set(null);
        this.selectedAgentId.set(null);
    }

    onAssignAgentSelect(event: Event): void {
        const select = event.target as HTMLSelectElement;
        this.selectedAgentId.set(select.value ? Number(select.value) : null);
    }

    assignAgent(): void {
        const declaration = this.selectedDeclaration();
        const agentId = this.selectedAgentId();

        if (!declaration || !agentId) {
            return;
        }

        this.actioning.set(true);
        this.adminService.assignAgent(declaration.declarationId, agentId).subscribe({
            next: (res) => {
                this.actioning.set(false);
                if (res.success) {
                    this.toast.success('Agent assigned successfully');
                    this.closeAssignModal();
                    this.loadAvailableAgents();
                    this.loadPage();
                    return;
                }
                this.toast.error(res.error || 'Failed to assign agent.');
            },
            error: (err) => {
                this.actioning.set(false);
                this.toast.error(err.error?.error || 'Failed to assign agent.');
            }
        });
    }

    openReassignModal(assignment: AgentTaskResponse): void {
        this.selectedAssignment.set(assignment);
        this.reassignAgentId.set(null);
        this.reassignReason.set('');
        this.reassignModalOpen.set(true);
    }

    closeReassignModal(): void {
        this.reassignModalOpen.set(false);
        this.selectedAssignment.set(null);
        this.reassignAgentId.set(null);
        this.reassignReason.set('');
    }

    onReassignAgentSelect(event: Event): void {
        const select = event.target as HTMLSelectElement;
        this.reassignAgentId.set(select.value ? Number(select.value) : null);
    }

    setReassignReason(event: Event): void {
        const input = event.target as HTMLInputElement;
        this.reassignReason.set(input.value);
    }

    reassignAgent(): void {
        const assignment = this.selectedAssignment();
        const agentId = this.reassignAgentId();
        const reason = this.reassignReason().trim();

        if (!assignment || !agentId || !reason) {
            this.toast.warning('Please select a new agent and provide a reason.');
            return;
        }

        this.actioning.set(true);
        this.adminService.reassignDeclaration(assignment.declarationId, agentId, reason).subscribe({
            next: (res) => {
                this.actioning.set(false);
                if (res.success) {
                    this.toast.success('Assignment reassigned successfully');
                    this.closeReassignModal();
                    this.loadAvailableAgents();
                    this.loadPage();
                    return;
                }
                this.toast.error(res.error || 'Failed to reassign assignment.');
            },
            error: (err) => {
                this.actioning.set(false);
                this.toast.error(err.error?.error || 'Failed to reassign assignment.');
            }
        });
    }

    openCancelConfirm(assignment: AgentTaskResponse): void {
        this.selectedAssignment.set(assignment);
        this.cancelConfirmOpen.set(true);
    }

    closeCancelConfirm(): void {
        this.cancelConfirmOpen.set(false);
        this.selectedAssignment.set(null);
    }

    confirmCancel(): void {
        const assignment = this.selectedAssignment();
        if (!assignment) {
            return;
        }

        this.actioning.set(true);
        this.adminService.cancelAssignment(assignment.declarationId).subscribe({
            next: (res) => {
                this.actioning.set(false);
                if (res.success) {
                    this.toast.success('Assignment cancelled successfully');
                    this.closeCancelConfirm();
                    this.loadAvailableAgents();
                    this.loadPage();
                    return;
                }
                this.toast.error(res.error || 'Failed to cancel assignment.');
            },
            error: (err) => {
                this.actioning.set(false);
                this.toast.error(err.error?.error || 'Failed to cancel assignment.');
            }
        });
    }

    canReassign(assignment: AgentTaskResponse): boolean {
        return assignment.assignmentStatus === 'ACTIVE' || !assignment.assignmentStatus;
    }

    getStatusBadgeClass(status?: string, isOverdue?: boolean): string {
        if (!status) return '';
        if (isOverdue && status !== 'COMPLETED') {
            return 'bg-red-100 text-red-700 border-red-300';
        }
        switch (status) {
            case 'ASSIGNED': return 'bg-blue-100 text-blue-700 border-blue-300';
            case 'IN_PROGRESS': return 'bg-amber-100 text-amber-700 border-amber-300';
            case 'COMPLETED': return 'bg-green-100 text-green-700 border-green-300';
            case 'REASSIGNED': return 'bg-gray-100 text-gray-700 border-gray-300';
            default: return 'bg-gray-100 text-gray-700 border-gray-300';
        }
    }
}
