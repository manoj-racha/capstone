import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../../core/services/admin.service';

@Component({
    selector: 'app-agents',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './agents.component.html'
})
export class AgentsComponent implements OnInit {
    private adminService = inject(AdminService);

    agents = signal<any[]>([]);
    error = signal<string>('');

    // Pagination state
    currentPage = signal<number>(0);
    pageSize = signal<number>(9); // 9 agents per page looks good for a 3-column grid
    totalElements = signal<number>(0);
    totalPages = signal<number>(0);

    ngOnInit(): void {
        this.loadAgents();
    }

    loadAgents(): void {
        this.adminService.getAgents(this.currentPage(), this.pageSize()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.agents.set(res.data.content);
                    this.totalElements.set(res.data.totalElements);
                    this.totalPages.set(res.data.totalPages);
                } else {
                    this.error.set(res.error || 'Failed to load agents.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load agents.');
            }
        });
    }

    onPageChange(page: number): void {
        if (page >= 0 && page < this.totalPages()) {
            this.currentPage.set(page);
            this.loadAgents();
        }
    }
}
