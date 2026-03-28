import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { ToastService } from '../../../../core/services/toast.service';
import { DeclarationProgressComponent } from '../../../../shared/components/declaration-progress/declaration-progress.component';

@Component({
  selector: 'app-declaration-household',
  imports: [ReactiveFormsModule, DeclarationProgressComponent],
  template: `
    <div class="min-h-screen bg-gs-bg py-10 px-4">
      <div class="max-w-2xl mx-auto">
        <app-declaration-progress [currentStep]="1" [completedSteps]="[]" />

        <div class="bg-white rounded-xl border border-gray-200 p-8 shadow-sm">
          <h2 class="text-2xl font-bold text-gray-900 mb-2">Household Information</h2>
          <p class="text-gray-500 text-sm mb-6">Tell us about your household so we can calculate per-capita emissions.</p>

          <form [formGroup]="form" (ngSubmit)="onSave()" class="space-y-6">
            <div>
              <label for="numberOfMembers" class="block text-sm font-semibold text-gray-700 mb-1.5">
                Number of Family Members <span class="text-red-500">*</span>
              </label>
              <input id="numberOfMembers" type="number" min="1" max="20" formControlName="numberOfMembers"
                class="w-full px-4 py-3 rounded-lg border-2 border-gray-200 focus:border-green-500 focus:outline-none text-sm"
                placeholder="e.g. 4" />
              <p class="text-gray-400 text-xs mt-1">Include all family members living in the household</p>
              @if (form.controls.numberOfMembers.invalid && form.controls.numberOfMembers.touched) {
                <p class="text-red-500 text-xs mt-1 font-medium">Please enter a number between 1 and 20</p>
              }
            </div>

            <div class="flex justify-end">
              <button type="submit" [disabled]="loading()"
                class="px-8 py-3 bg-green-700 text-white rounded-lg font-semibold hover:bg-green-800 transition-colors disabled:opacity-50">
                @if (loading()) { Saving... } @else { Save & Continue → }
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `
})
export class DeclarationHouseholdComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly declarationService = inject(DeclarationService);
  private readonly toast = inject(ToastService);

  loading = signal(false);
  declarationId = 0;

  form = this.fb.group({
    numberOfMembers: [null as number | null, [Validators.required, Validators.min(1), Validators.max(20)]]
  });

  ngOnInit(): void {
    this.declarationId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadExistingData();
  }

  private loadExistingData(): void {
    this.declarationService.getDeclarationById(this.declarationId).subscribe({
      next: (res) => {
        const members = res?.data?.householdSize;
        if (res.success && typeof members === 'number' && members > 0) {
          this.form.patchValue({ numberOfMembers: members });
        }
      }
    });
  }

  onSave(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.declarationService.saveHousehold(this.declarationId, {
      numberOfMembers: this.form.controls.numberOfMembers.value!
    }).subscribe({
      next: () => {
        this.loading.set(false);
        const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
        if (returnTo === 'review') {
          this.router.navigate(['/declaration', this.declarationId, 'review']);
        } else {
          this.router.navigate(['/declaration', this.declarationId, 'vehicle']);
        }
      },
      error: () => { this.loading.set(false); }
    });
  }
}
