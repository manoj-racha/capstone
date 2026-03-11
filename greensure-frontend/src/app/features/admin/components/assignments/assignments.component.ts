import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../../core/services/admin.service';
import { AgentTaskResponse } from '../../../../core/models/agent';

@Component({
    selector: 'app-assignments',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './assignments.component.html'
})
export class AssignmentsComponent implements OnInit {
    private adminService = inject(AdminService);

    assignments = signal<AgentTaskResponse[]>([]);

    error = signal<string>('');

    // State for reassignment modal
    reassignModalOpen = signal<boolean>(false);
    selectedAssignmentId = signal<number | null>(null);

    // Agent list for reassignment
    agents = signal<any[]>([]);
    agentsLoading = signal<boolean>(false);
    newAgentId = signal<number | null>(null);
    actioning = signal<boolean>(false);

    // Filters
    statusFilter = signal<string>('');

    // Pagination state
    currentPage = signal<number>(0);
    pageSize = signal<number>(10);
    totalElements = signal<number>(0);
    totalPages = signal<number>(0);

    ngOnInit(): void {
        this.loadAssignments();
    }

    loadAssignments(): void {

        this.error.set('');

        const stat = this.statusFilter() || undefined;

        this.adminService.getAssignments(stat, this.currentPage(), this.pageSize()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.assignments.set(res.data.content);
                    this.totalElements.set(res.data.totalElements);
                    this.totalPages.set(res.data.totalPages);
                } else {
                    this.error.set(res.error || 'Failed to load assignments.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load assignments.');
            }
        });
    }

    onFilterChange(val: string): void {
        this.statusFilter.set(val === 'ALL' ? '' : val);
        this.currentPage.set(0); // Reset to first page
        this.loadAssignments();
    }

    onPageChange(page: number): void {
        if (page >= 0 && page < this.totalPages()) {
            this.currentPage.set(page);
            this.loadAssignments();
        }
    }

    openReassignModal(assignmentId: number): void {
        this.selectedAssignmentId.set(assignmentId);
        this.reassignModalOpen.set(true);

        // Lazy load agents if not already loaded (getting first 100 for drop-down)
        if (this.agents().length === 0) {
            this.agentsLoading.set(true);
            this.adminService.getAgents(0, 100).subscribe({
                next: (res) => {
                    this.agentsLoading.set(false);
                    if (res.success && res.data) {
                        // Only active agents
                        this.agents.set(res.data.content.filter((a: any) => a.status === 'ACTIVE'));
                    }
                },
                error: () => this.agentsLoading.set(false)
            });
        }
    }

    closeReassignModal(): void {
        this.reassignModalOpen.set(false);
        this.selectedAssignmentId.set(null);
        this.newAgentId.set(null);
    }

    onAgentSelect(event: Event): void {
        const select = event.target as HTMLSelectElement;
        this.newAgentId.set(select.value ? Number(select.value) : null);
    }

    reassignTask(): void {
        const assignmentId = this.selectedAssignmentId();
        const agentId = this.newAgentId();

        if (!assignmentId || !agentId) return;

        this.actioning.set(true);
        this.adminService.reassignTask(assignmentId, agentId).subscribe({
            next: (res) => {
                this.actioning.set(false);
                if (res.success) {
                    this.closeReassignModal();
                    this.loadAssignments(); // Reload listing
                } else {
                    alert('Failed to reassign: ' + res.error);
                }
            },
            error: (err) => {
                this.actioning.set(false);
                alert('Failed to reassign task.');
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
            case 'COMPLETED': return 'bg-gs-dark/10 text-gs-dark border-gs-dark/20';
            case 'REASSIGNED': return 'bg-purple-500/20 text-purple-400 border-purple-500/30';
            default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
        }
    }
}
