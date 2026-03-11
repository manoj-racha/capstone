import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { DeclarationResponse } from '../../../../core/models/declaration';

@Component({
    selector: 'app-declaration-history',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './declaration-history.component.html'
})
export class DeclarationHistoryComponent implements OnInit {
    private declarationService = inject(DeclarationService);

    history = signal<DeclarationResponse[]>([]);

    error = signal<string>('');

    ngOnInit(): void {
        this.declarationService.getHistory().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.history.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load history.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load history.');
            }
        });
    }

    getStatusBadgeClass(status: string): string {
        switch (status) {
            case 'DRAFT': return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
            case 'SUBMITTED': return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
            case 'UNDER_VERIFICATION': return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
            case 'VERIFIED': return 'bg-gs-dark/10 text-gs-dark border-gs-dark/20';
            case 'REJECTED': return 'bg-red-500/20 text-red-400 border-red-500/30';
            default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
        }
    }

    getStatusDisplay(status: string): string {
        return status.replace('_', ' ');
    }
}
