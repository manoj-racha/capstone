import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule, DecimalPipe } from '@angular/common';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { FileUploadService } from '../../../../core/services/file-upload.service';
import { ToastService } from '../../../../core/services/toast.service';
import { DeclarationProgressComponent } from '../../../../shared/components/declaration-progress/declaration-progress.component';

@Component({
  selector: 'app-declaration-solar',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DeclarationProgressComponent, DecimalPipe],
  template: `
    <div class="min-h-screen bg-gs-bg py-10 px-4">
      <div class="max-w-2xl mx-auto">
        <app-declaration-progress [currentStep]="4" [completedSteps]="[1,2,3]" />

        <div class="bg-white rounded-xl border border-gray-200 p-8 shadow-sm">
          <div class="flex items-center gap-3 mb-2">
            <h2 class="text-2xl font-bold text-gray-900">Solar Installation</h2>
            <span class="px-2.5 py-0.5 bg-gray-100 text-gray-500 rounded-full text-xs font-medium">Optional</span>
          </div>
          <p class="text-gray-500 text-sm mb-6">Solar panels offset your electricity carbon footprint.</p>

          <form [formGroup]="form" (ngSubmit)="onSave()" class="space-y-6">
            <!-- Toggle -->
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-3">Do you have solar panels installed?</label>
              <div class="flex gap-3">
                <button type="button" (click)="setHasSolar(true)"
                  [class]="form.controls.hasSolar.value === true ? 'flex-1 py-3 rounded-lg border-2 border-green-500 bg-green-50 text-green-700 font-semibold text-sm transition-all' : 'flex-1 py-3 rounded-lg border-2 border-gray-200 text-gray-500 text-sm hover:border-gray-300 transition-all'">
                  ☀️ Yes
                </button>
                <button type="button" (click)="setHasSolar(false)"
                  [class]="form.controls.hasSolar.value === false ? 'flex-1 py-3 rounded-lg border-2 border-green-500 bg-green-50 text-green-700 font-semibold text-sm transition-all' : 'flex-1 py-3 rounded-lg border-2 border-gray-200 text-gray-500 text-sm hover:border-gray-300 transition-all'">
                  No
                </button>
              </div>
            </div>

            @if (form.controls.hasSolar.value === true) {
              <div class="space-y-5 animate-in fade-in slide-in-from-top-2">
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-1.5">Installed Capacity (kW)</label>
                  <input type="number" formControlName="capacityKw" placeholder="e.g. 3" min="0" step="0.5"
                    class="w-full px-3 py-2.5 rounded-lg border-2 border-gray-200 focus:border-green-500 focus:outline-none text-sm" />
                  @if (form.controls.capacityKw.value) {
                    <p class="text-green-600 text-[11px] mt-1 font-bold bg-green-50 px-2 py-1 rounded inline-block">
                      ✨ Estimated Offset: {{ (form.controls.capacityKw.value * 1476) | number:'1.0-0' }} kg CO₂ / year
                    </p>
                  }
                </div>

                <div class="bg-green-50 border border-green-200 rounded-lg p-5">
                  <label class="block text-xs font-bold text-green-900 uppercase tracking-wider mb-2">Installer Certificate / MNRE Approval</label>
                  <p class="text-[11px] text-green-700 mb-4">Uploading your solar certificate helps agents verify your installation faster.</p>
                  
                  <div class="border-2 border-dashed border-green-300 rounded-lg p-5 text-center hover:bg-green-100/50 transition cursor-pointer relative">
                    <input type="file" (change)="onCertificateSelected($event)" accept=".pdf,.jpg,.jpeg,.png"
                      class="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
                    <div class="text-green-700">
                      @if (uploadedCertUrl()) {
                        <p class="text-sm font-bold flex items-center justify-center gap-2">
                          <span class="text-xl">📄</span> Certificate Attached
                        </p>
                        <p class="text-[10px] mt-1 opacity-75">Click to replace</p>
                      } @else if (isUploadingCert()) {
                        <div class="flex flex-col items-center gap-2">
                          <span class="w-5 h-5 border-2 border-green-600 border-t-transparent rounded-full animate-spin"></span>
                          <span class="text-xs font-bold">Uploading...</span>
                        </div>
                      } @else {
                        <p class="text-sm font-bold">Click to upload certificate</p>
                        <p class="text-[10px] opacity-75">PDF, JPG, PNG (Max 10MB)</p>
                      }
                    </div>
                  </div>
                </div>
              </div>
            }

            @if (form.controls.hasSolar.value === false) {
              <div class="bg-gs-bg border border-gray-100 rounded-lg p-4 flex items-start gap-3 animate-in fade-in slide-in-from-top-2">
                <span class="text-lg">💡</span>
                <p class="text-gray-600 text-sm">Solar panels can significantly reduce your carbon footprint and energy bills. Consider installing rooftop solar to improve your GreenSure score!</p>
              </div>
            }

            <div class="flex justify-between pt-6 border-t border-gray-100">
              <button type="button" (click)="goBack()" class="px-6 py-3 bg-gray-100 text-gray-700 rounded-lg font-semibold hover:bg-gray-200 transition-colors">
                ← Back
              </button>
              <div class="flex gap-3">
                <button type="button" (click)="skip()" class="px-6 py-3 bg-white text-gray-500 rounded-lg font-medium hover:bg-gray-50 border border-gray-200 transition-colors text-sm">
                  Skip this step
                </button>
                <button type="submit" [disabled]="loading() || isUploadingCert()"
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
export class DeclarationSolarComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly declarationService = inject(DeclarationService);
  private readonly fileUploadService = inject(FileUploadService);
  private readonly toastService = inject(ToastService);

  loading = signal(false);
  declarationId = 0;
  
  uploadedCertUrl = signal<string | null>(null);
  isUploadingCert = signal(false);

  form = this.fb.group({
    hasSolar: [false],
    capacityKw: [null as number | null],
  });

  ngOnInit(): void {
    this.declarationId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadExistingData();
  }

  private loadExistingData(): void {
    this.declarationService.getDeclarationById(this.declarationId).subscribe({
      next: (res) => {
        const s = res?.data?.solarData;
        if (res.success && s) {
          this.form.patchValue({
            hasSolar: !!s.hasSolar,
            capacityKw: s.capacityKw ?? null
          });
          this.uploadedCertUrl.set(s.certificateUrl ?? null);
        }
      }
    });
  }

  setHasSolar(val: boolean): void {
    this.form.controls.hasSolar.setValue(val);
    if (!val) {
      this.form.controls.capacityKw.setValue(null);
      this.uploadedCertUrl.set(null);
    }
  }

  onCertificateSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    
    const file = input.files[0];
    this.isUploadingCert.set(true);

    this.fileUploadService.uploadFile(file, 'solar').subscribe({
      next: (uploaded) => {
        this.uploadedCertUrl.set(uploaded.fileUrl);
        this.isUploadingCert.set(false);
        this.toastService.success('Solar certificate uploaded');
      },
      error: (err: Error) => {
        this.isUploadingCert.set(false);
        this.toastService.error(err.message);
      }
    });
    input.value = '';
  }

  onSave(): void {
    if (this.isUploadingCert()) return;
    
    this.loading.set(true);
    this.declarationService.saveSolar(this.declarationId, {
      hasSolar: this.form.controls.hasSolar.value ?? false,
      capacityKw: this.form.controls.capacityKw.value ?? undefined,
      certificateUrl: this.uploadedCertUrl() ?? undefined
    }).subscribe({
      next: () => { 
        this.loading.set(false); 
        this.toastService.success('Solar data saved');
        const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
        if (returnTo === 'review') {
          this.router.navigate(['/declaration', this.declarationId, 'review']);
        } else {
          this.router.navigate(['/declaration', this.declarationId, 'cooking']); 
        }
      },
      error: () => { 
        this.loading.set(false);
        this.toastService.error('Failed to save solar data');
      }
    });
  }

  skip(): void { 
    this.router.navigate(['/declaration', this.declarationId, 'cooking']); 
  }
  
  goBack(): void { this.router.navigate(['/declaration', this.declarationId, 'electricity']); }
}
