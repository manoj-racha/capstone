import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../../features/admin/services/admin.service';
import { DeclarationResponse } from '../../../../core/models/declaration';
import { AvailableAgent } from '../../../../core/models/admin';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
    selector: 'app-declarations',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './declarations.component.html'
})
export class DeclarationsComponent implements OnInit {
    private adminService = inject(AdminService);
    private toast = inject(ToastService);

    declarations = signal<DeclarationResponse[]>([]);

    error = signal<string>('');

    // Filter state
    statusFilter = signal<string>('');

    // Pagination state
    currentPage = signal<number>(0);
    pageSize = signal<number>(10);
    totalElements = signal<number>(0);
    totalPages = signal<number>(0);

    availableAgents = signal<AvailableAgent[]>([]);
    assignmentModalOpen = signal(false);
    reassignMode = signal(false);
    selectedDeclaration = signal<DeclarationResponse | null>(null);
    selectedAgentId = signal<number | null>(null);
    reason = signal('');
    actioning = signal(false);

    ngOnInit(): void {
        this.loadDeclarations();
        this.loadAvailableAgents();
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

    loadDeclarations(): void {

        this.error.set('');

        const stat = this.statusFilter() || undefined;

        this.adminService.getDeclarations(stat, this.currentPage(), this.pageSize()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.declarations.set(res.data.content);
                    this.totalElements.set(res.data.totalElements);
                    this.totalPages.set(res.data.totalPages);
                } else {
                    this.error.set(res.error || 'Failed to load declarations.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load declarations.');
            }
        });
    }

    onFilterChange(val: string): void {
        this.statusFilter.set(val === 'ALL' ? '' : val);
        this.currentPage.set(0); // Reset to first page
        this.loadDeclarations();
    }

    onPageChange(page: number): void {
        if (page >= 0 && page < this.totalPages()) {
            this.currentPage.set(page);
            this.loadDeclarations();
        }
    }

    getStatusBadgeClass(status: string): string {
        switch (status) {
            case 'DRAFT': return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
            case 'SUBMITTED': return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
            case 'VERIFIED': return 'bg-gs-dark/10 text-gs-dark border-gs-dark/20';
            case 'REJECTED': return 'bg-red-500/20 text-red-500 border-red-500/30';
            default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
        }
    }

    openAssignmentModal(declaration: DeclarationResponse): void {
        this.selectedDeclaration.set(declaration);
        this.reassignMode.set(!!declaration.assignedAgentId);
        this.selectedAgentId.set(null);
        this.reason.set('');
        this.assignmentModalOpen.set(true);
    }

    closeAssignmentModal(): void {
        this.assignmentModalOpen.set(false);
        this.selectedDeclaration.set(null);
        this.selectedAgentId.set(null);
        this.reason.set('');
        this.reassignMode.set(false);
    }

    onAgentChange(event: Event): void {
        const select = event.target as HTMLSelectElement;
        this.selectedAgentId.set(select.value ? Number(select.value) : null);
    }

    onReasonInput(event: Event): void {
        const input = event.target as HTMLInputElement;
        this.reason.set(input.value);
    }

    confirmAssignmentAction(): void {
        const declaration = this.selectedDeclaration();
        const agentId = this.selectedAgentId();

        if (!declaration || !agentId) {
            return;
        }

        if (this.reassignMode() && !this.reason().trim()) {
            this.toast.warning('Reason is required for reassignment.');
            return;
        }

        this.actioning.set(true);

        const request = this.reassignMode()
            ? this.adminService.reassignDeclaration(declaration.declarationId, agentId, this.reason().trim())
            : this.adminService.assignAgent(declaration.declarationId, agentId);

        request.subscribe({
            next: (res) => {
                this.actioning.set(false);
                if (res.success) {
                    this.toast.success(this.reassignMode() ? 'Assignment reassigned successfully' : 'Agent assigned successfully');
                    this.closeAssignmentModal();
                    this.loadAvailableAgents();
                    this.loadDeclarations();
                    return;
                }

                this.toast.error(res.error || 'Assignment action failed.');
            },
            error: (err) => {
                this.actioning.set(false);
                this.toast.error(err.error?.error || 'Assignment action failed.');
            }
        });
    }
}
