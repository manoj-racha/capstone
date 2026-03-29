import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-declaration-fill',
  standalone: true,
  imports: [],
  template: `
    <div class="min-h-screen bg-gs-bg flex items-center justify-center">
      <div class="text-center space-y-4">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto"></div>
        <p class="text-gs-body font-medium">Resuming your declaration...</p>
      </div>
    </div>
  `
})
export class DeclarationFillComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly declarationService = inject(DeclarationService);
  private readonly toast = inject(ToastService);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/user/dashboard']);
      return;
    }

    this.declarationService.getDeclarationById(id).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const d = res.data;
          
          // Determine where the user left off
          if (!d.householdSize || d.householdSize <= 0) {
            this.router.navigate(['/declaration', id, 'household']);
          } else if (d.vehicles.length === 0) {
            this.router.navigate(['/declaration', id, 'electricity']);
          } else if (!d.electricityData) {
            this.router.navigate(['/declaration', id, 'electricity']);
          } else if (!d.solarData) {
            this.router.navigate(['/declaration', id, 'solar']);
          } else if (!d.cookingData) {
            this.router.navigate(['/declaration', id, 'cooking']);
          } else if (!d.lifestyleData) {
            this.router.navigate(['/declaration', id, 'lifestyle']);
          } else {
            this.router.navigate(['/declaration', id, 'review']);
          }
        } else {
          this.toast.error('Failed to load declaration.');
          this.router.navigate(['/user/dashboard']);
        }
      },
      error: () => {
        this.toast.error('Could not resume declaration.');
        this.router.navigate(['/user/dashboard']);
      }
    });
  }
}
