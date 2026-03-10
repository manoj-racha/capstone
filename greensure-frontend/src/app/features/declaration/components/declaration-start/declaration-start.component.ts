import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { DeclarationService } from '../../../../core/services/declaration.service';

@Component({
    selector: 'app-declaration-start',
    imports: [CommonModule],
    templateUrl: './declaration-start.component.html'
})
export class DeclarationStartComponent {
    private declarationService = inject(DeclarationService);
    private router = inject(Router);


    error = signal('');

    startDeclaration(): void {
        this.error.set('');

        this.declarationService.startDeclaration().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    // Navigate to fill form with declaration ID
                    this.router.navigate(['/declaration/fill', res.data.declarationId]);
                } else {
                    this.error.set(res.error || 'Failed to start declaration.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to start declaration. Only one declaration per year is allowed.');
            }
        });
    }
}
