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

    ngOnInit(): void {
        this.adminService.getAgents().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.agents.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load agents.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load agents.');
            }
        });
    }
}
