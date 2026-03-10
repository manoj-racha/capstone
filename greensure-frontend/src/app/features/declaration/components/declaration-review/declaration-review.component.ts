import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { DeclarationResponse } from '../../../../core/models/declaration';

@Component({
    selector: 'app-declaration-review',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './declaration-review.component.html'
})
export class DeclarationReviewComponent implements OnInit {
    private declarationService = inject(DeclarationService);
    private route = inject(ActivatedRoute);
    private router = inject(Router);

    declarationId = signal<number>(0);
    declaration = signal<DeclarationResponse | null>(null);

    error = signal<string>('');

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
        this.declarationService.getDeclaration(this.declarationId()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.declaration.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load declaration details.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load declaration details.');
            }
        });
    }

    onSubmit(): void {
        this.error.set('');

        this.declarationService.submitDeclaration(this.declarationId()).subscribe({
            next: (res) => {
                if (res.success) {
                    // Success! Navigate to user dashboard or declaration history
                    this.router.navigate(['/user/dashboard']);
                } else {
                    this.error.set(res.error || 'Failed to submit declaration.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'An error occurred during submission.');
            }
        });
    }
}
