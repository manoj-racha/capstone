import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../../core/services/admin.service';
import { DeclarationResponse } from '../../../../core/models/declaration';

@Component({
    selector: 'app-declarations',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './declarations.component.html'
})
export class DeclarationsComponent implements OnInit {
    private adminService = inject(AdminService);

    declarations = signal<DeclarationResponse[]>([]);

    error = signal<string>('');

    // Filter state
    statusFilter = signal<string>('');

    // Pagination state
    currentPage = signal<number>(0);
    pageSize = signal<number>(10);
    totalElements = signal<number>(0);
    totalPages = signal<number>(0);

    ngOnInit(): void {
        this.loadDeclarations();
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
}
