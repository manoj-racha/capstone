import { Component, inject, signal, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-declaration-start',
  imports: [],
  template: `
    <div class="min-h-screen bg-gs-bg flex items-center justify-center px-4">
      <div class="max-w-md w-full text-center space-y-6">
        @if (loading()) {
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto"></div>
          <p class="text-gs-body font-medium">Starting your declaration...</p>
        }
        @if (error()) {
          <div class="bg-red-50 border border-red-200 rounded-xl p-6 space-y-4">
            <p class="text-red-600 font-medium">{{ error() }}</p>
            <div class="flex gap-3 justify-center">
              <button (click)="startDeclaration()" class="px-6 py-2.5 bg-green-700 text-white rounded-lg font-semibold hover:bg-green-800 transition-colors">
                Try Again
              </button>
              <button (click)="goToDashboard()" class="px-6 py-2.5 bg-gray-100 text-gray-700 rounded-lg font-semibold hover:bg-gray-200 transition-colors">
                Back to Dashboard
              </button>
            </div>
          </div>
        }
      </div>
    </div>
  `
})
export class DeclarationStartComponent implements OnInit {
  private readonly declarationService = inject(DeclarationService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  loading = signal(true);
  error = signal('');

  ngOnInit(): void {
    this.startDeclaration();
  }

  startDeclaration(): void {
    this.error.set('');
    this.loading.set(true);

    this.declarationService.startDeclaration().subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.router.navigate(['/declaration', res.data, 'household']);
        } else {
          this.error.set(res.message || 'Failed to start declaration.');
        }
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 409) {
          this.error.set('You already have a declaration for this year.');
        } else {
          this.error.set(err.error?.message || 'Failed to start declaration.');
        }
      }
    });
  }

  goToDashboard(): void {
    this.router.navigate(['/user/dashboard']);
  }
}
