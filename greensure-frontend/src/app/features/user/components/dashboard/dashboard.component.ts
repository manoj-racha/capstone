import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../../core/services/user.service';
import { DashboardResponse } from '../../../../core/models/user';

@Component({
    selector: 'app-dashboard',
    imports: [RouterLink, CommonModule],
    templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
    private userService = inject(UserService);

    dashboard = signal<DashboardResponse | null>(null);

    error = signal<string>('');

    ngOnInit(): void {
        this.userService.getDashboard().subscribe({
            next: (res) => {
                if (res.success) {
                    this.dashboard.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load dashboard data');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load dashboard data');
            }
        });
    }
}
