import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService } from '../../../../features/admin/services/admin.service';
import { DeclarationService } from '../../../../features/declaration/services/declaration.service';
import { DeclarationDetail } from '../../../../core/models/declaration';
import { ApiResponse } from '../../../../core/models/api-response';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
    selector: 'app-declaration-detail',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './declaration-detail.component.html'
})
export class DeclarationDetailComponent implements OnInit {
    private adminService = inject(AdminService);
    private declarationService = inject(DeclarationService);
    private route = inject(ActivatedRoute);
    private toast = inject(ToastService);

    declarationId = signal<number>(0);
    declaration = signal<DeclarationDetail | null>(null);


    error = signal<string>('');
    actioning = signal<boolean>(false);
    confirmModalOpen = signal<boolean>(false);

    ngOnInit(): void {
        const idParam = this.route.snapshot.paramMap.get('id');
        if (idParam) {
            this.declarationId.set(Number(idParam));
            this.loadDeclaration();
        } else {
            this.error.set('Invalid declaration ID.');
        }
    }

    loadDeclaration(): void {
        this.declarationService.getDeclarationById(this.declarationId()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.declaration.set(res.data);
                } else {
                    this.error.set(res.message || 'Failed to load declaration details.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load declaration details.');
            }
        });
    }

    unlockDeclaration(): void {
        if (!this.declaration() || this.declaration()?.status !== 'REJECTED') return;

        this.confirmModalOpen.set(true);
    }

    closeConfirmModal(): void {
        this.confirmModalOpen.set(false);
    }

    confirmUnlockDeclaration(): void {
        this.closeConfirmModal();
        if (!this.declaration() || this.declaration()?.status !== 'REJECTED') return;

        this.actioning.set(true);
        this.adminService.unlockDeclaration(this.declarationId()).subscribe({
            next: (res: ApiResponse<void>) => {
                this.actioning.set(false);
                if (res.success) {
                    this.declaration.update(d => d ? { ...d, status: 'DRAFT' } : null); // Assumes it returns to draft state
                    this.toast.success('Declaration unlocked successfully.');
                } else {
                    this.toast.error('Failed to unlock declaration: ' + res.message);
                }
            },
            error: (_err: unknown) => {
                this.actioning.set(false);
                this.toast.error('Failed to unlock declaration.');
            }
        });
    }

    getStatusBadgeClass(status?: string): string {
        if (!status) return '';
        switch (status) {
            case 'DRAFT': return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
            case 'SUBMITTED': return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
            case 'VERIFIED': return 'bg-gs-dark/10 text-gs-dark border-gs-dark/20';
            case 'REJECTED': return 'bg-red-500/20 text-red-500 border-red-500/30';
            default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
        }
    }
}
