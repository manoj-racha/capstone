import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { DeclarationProgressComponent } from '../../../../shared/components/declaration-progress/declaration-progress.component';
import { PublicTransportUsage } from '../../../../core/models/declaration';

@Component({
  selector: 'app-declaration-lifestyle',
  imports: [ReactiveFormsModule, DeclarationProgressComponent],
  template: `
    <div class="min-h-screen bg-gs-bg py-10 px-4">
      <div class="max-w-2xl mx-auto">
        <app-declaration-progress [currentStep]="6" [completedSteps]="[1,2,3,4,5]" />

        <div class="bg-white rounded-xl border border-gray-200 p-8 shadow-sm">
          <div class="flex items-center gap-3 mb-2">
            <h2 class="text-2xl font-bold text-gray-900">Lifestyle & Habits</h2>
            <span class="px-2.5 py-0.5 bg-green-100 text-green-700 rounded-full text-xs font-medium">Optional — earn bonus discounts</span>
          </div>
          <p class="text-gray-500 text-sm mb-6">Earn additional discounts based on your eco-friendly habits.</p>

          <form [formGroup]="form" (ngSubmit)="onSave()" class="space-y-6">
            <!-- Public Transport Usage -->
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-3">How often do you use public transport?</label>
              <div class="grid grid-cols-5 gap-2">
                @for (opt of transportOptions; track opt.value) {
                  <button type="button" (click)="form.controls.publicTransportUsage.setValue(opt.value)"
                    [class]="form.controls.publicTransportUsage.value === opt.value ?
                      'py-3 px-2 rounded-lg border-2 border-green-500 bg-green-50 text-green-700 font-semibold text-xs text-center' :
                      'py-3 px-2 rounded-lg border-2 border-gray-200 text-gray-500 text-xs text-center hover:border-gray-300'">
                    {{ opt.label }}
                  </button>
                }
              </div>
              @if (form.controls.publicTransportUsage.value === 'OFTEN' || form.controls.publicTransportUsage.value === 'ALWAYS') {
                <p class="text-green-600 text-xs mt-2 font-medium">🎉 Using public transport often can earn up to 3% vehicle emission reduction</p>
              }
            </div>

            <!-- Recycling Toggle -->
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-3">Do you actively recycle waste?</label>
              <div class="flex gap-3">
                <button type="button" (click)="form.controls.wastesRecycling.setValue(true)"
                  [class]="form.controls.wastesRecycling.value === true ? 'flex-1 py-3 rounded-lg border-2 border-green-500 bg-green-50 text-green-700 font-semibold text-sm' : 'flex-1 py-3 rounded-lg border-2 border-gray-200 text-gray-500 text-sm'">
                  ♻️ Yes, I recycle
                </button>
                <button type="button" (click)="form.controls.wastesRecycling.setValue(false)"
                  [class]="form.controls.wastesRecycling.value === false ? 'flex-1 py-3 rounded-lg border-2 border-green-500 bg-green-50 text-green-700 font-semibold text-sm' : 'flex-1 py-3 rounded-lg border-2 border-gray-200 text-gray-500 text-sm'">
                  Not yet
                </button>
              </div>
              @if (form.controls.wastesRecycling.value === true) {
                <p class="text-green-600 text-xs mt-2 font-medium">🌱 Earns a flat 50 kg CO₂ bonus reduction</p>
              }
            </div>

            <div class="flex justify-between pt-4">
              <button type="button" (click)="goBack()" class="px-6 py-3 bg-gray-100 text-gray-700 rounded-lg font-semibold hover:bg-gray-200 transition-colors">
                ← Back
              </button>
              <div class="flex gap-3">
                <button type="button" (click)="skip()" class="px-6 py-3 bg-gray-50 text-gray-500 rounded-lg font-medium hover:bg-gray-100 transition-colors text-sm">
                  Skip this step →
                </button>
                <button type="submit" [disabled]="loading()"
                  class="px-8 py-3 bg-green-700 text-white rounded-lg font-semibold hover:bg-green-800 transition-colors disabled:opacity-50">
                  @if (loading()) { Saving... } @else { Save & Continue → }
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  `
})
export class DeclarationLifestyleComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly declarationService = inject(DeclarationService);

  loading = signal(false);
  declarationId = 0;

  readonly transportOptions: { value: PublicTransportUsage; label: string }[] = [
    { value: 'NEVER', label: 'Never' },
    { value: 'RARELY', label: 'Rarely' },
    { value: 'SOMETIMES', label: 'Sometimes' },
    { value: 'OFTEN', label: 'Often' },
    { value: 'ALWAYS', label: 'Always' },
  ];

  form = this.fb.group({
    publicTransportUsage: ['SOMETIMES' as PublicTransportUsage],
    wastesRecycling: [false as boolean],
  });

  ngOnInit(): void {
    this.declarationId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadExistingData();
  }

  private loadExistingData(): void {
    this.declarationService.getDeclarationById(this.declarationId).subscribe({
      next: (res) => {
        const l = res?.data?.lifestyleData;
        if (res.success && l) {
          this.form.patchValue({
            publicTransportUsage: l.publicTransportUsage,
            wastesRecycling: !!l.wastesRecycling
          });
        }
      }
    });
  }

  onSave(): void {
    this.loading.set(true);
    this.declarationService.saveLifestyle(this.declarationId, {
      publicTransportUsage: this.form.controls.publicTransportUsage.value!,
      wastesRecycling: this.form.controls.wastesRecycling.value ?? false,
    }).subscribe({
      next: () => { 
        this.loading.set(false); 
        const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
        if (returnTo === 'review') {
          this.router.navigate(['/declaration', this.declarationId, 'review']);
        } else {
          this.router.navigate(['/declaration', this.declarationId, 'review']); 
        }
      },
      error: () => { this.loading.set(false); }
    });
  }

  skip(): void { 
    const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
    if (returnTo === 'review') {
      this.router.navigate(['/declaration', this.declarationId, 'review']);
    } else {
      this.router.navigate(['/declaration', this.declarationId, 'review']); 
    }
  }
  goBack(): void { this.router.navigate(['/declaration', this.declarationId, 'cooking']); }
}
