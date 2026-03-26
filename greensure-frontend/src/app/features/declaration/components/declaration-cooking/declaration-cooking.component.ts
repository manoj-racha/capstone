import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { FileUploadService, UploadedFile } from '../../../../core/services/file-upload.service';
import { ToastService } from '../../../../core/services/toast.service';
import { DeclarationProgressComponent } from '../../../../shared/components/declaration-progress/declaration-progress.component';
import { CookingFuel } from '../../../../core/models/declaration';

@Component({
  selector: 'app-declaration-cooking',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DeclarationProgressComponent],
  template: `
    <div class="min-h-screen bg-gs-bg py-10 px-4">
      <div class="max-w-2xl mx-auto">
        <app-declaration-progress [currentStep]="5" [completedSteps]="[1,2,3,4]" />

        <div class="bg-white rounded-xl border border-gray-200 p-8 shadow-sm">
          <h2 class="text-2xl font-bold text-gray-900 mb-2">Cooking Fuel</h2>
          <p class="text-gray-500 text-sm mb-6">Select your primary cooking fuel to calculate kitchen emissions.</p>

          <form [formGroup]="form" (ngSubmit)="onSave()" class="space-y-6">
            <!-- Fuel Type Cards -->
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-3">Cooking Fuel Type <span class="text-red-500">*</span></label>
              <div class="grid grid-cols-2 gap-3">
                @for (fuel of fuelOptions; track fuel.value) {
                  <button type="button" (click)="selectFuel(fuel.value)"
                    [class]="form.controls.fuelType.value === fuel.value ? 'p-4 rounded-lg border-2 border-green-500 bg-green-50 text-left transition-all' : 'p-4 rounded-lg border-2 border-gray-200 text-left hover:border-gray-300 transition-all'">
                    <span class="text-2xl">{{ fuel.icon }}</span>
                    <p [class]="form.controls.fuelType.value === fuel.value ? 'text-sm font-bold text-green-700 mt-1' : 'text-sm font-medium text-gray-600 mt-1'">{{ fuel.label }}</p>
                  </button>
                }
              </div>
            </div>

            @if (form.controls.fuelType.value === 'LPG') {
              <div class="space-y-4 animate-in fade-in slide-in-from-top-2">
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-1.5">Annual Cylinders Consumed</label>
                  <input type="number" formControlName="userDeclaredCylinders" placeholder="e.g. 10" min="0"
                    class="w-full px-3 py-2.5 rounded-lg border-2 border-gray-200 focus:border-green-500 focus:outline-none text-sm" />
                  <p class="text-gray-400 text-xs mt-1">Average family of 4 uses 8–12 cylinders/year</p>
                </div>
                
                <div class="bg-amber-50 border border-amber-200 rounded-lg p-5">
                  <h4 class="text-xs font-bold text-amber-900 uppercase tracking-wider mb-2">Upload Booking Receipts</h4>
                  <p class="text-[11px] text-amber-700 mb-4">Please upload your cylinder booking receipts or SMS screenshots for verification.</p>
                  
                  <div class="border-2 border-dashed border-amber-300 rounded-lg p-4 text-center hover:bg-amber-100/50 transition cursor-pointer relative">
                    <input type="file" (change)="onCookingFilesSelected($event)" accept=".pdf,.jpg,.jpeg,.png" multiple
                      class="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
                    <div class="text-amber-700">
                      <p class="text-xs font-bold">Click to upload receipts</p>
                      <p class="text-[10px] opacity-75">PDF, JPG, PNG (Max 10MB)</p>
                    </div>
                  </div>

                  @if (uploadedCookingDocs().length > 0) {
                    <div class="mt-3 space-y-2">
                      @for (doc of uploadedCookingDocs(); track doc.fileUrl) {
                        <div class="flex items-center justify-between bg-white/80 p-2 rounded border border-amber-100">
                          <div class="flex items-center gap-2 truncate">
                            <span class="text-xs text-amber-800 truncate max-w-[180px]">📄 {{ doc.originalFileName }}</span>
                          </div>
                          <button type="button" (click)="removeDoc($index)" class="text-red-400 hover:text-red-600 text-xs">✕</button>
                        </div>
                      }
                    </div>
                  }
                  @if (isUploadingCooking()) {
                    <div class="mt-2 flex items-center gap-2 text-[10px] text-amber-600 font-medium">
                      <span class="w-3 h-3 border-2 border-amber-600 border-t-transparent rounded-full animate-spin"></span>
                      Uploading documents...
                    </div>
                  }
                </div>
              </div>
            }

            @if (form.controls.fuelType.value === 'PNG') {
              <div class="space-y-4 animate-in fade-in slide-in-from-top-2">
                <div>
                  <label class="block text-sm font-semibold text-gray-700 mb-1.5">PNG Consumer Number</label>
                  <input type="text" formControlName="pngConsumerNumber" placeholder="Your PNG consumer ID"
                    class="w-full px-3 py-2.5 rounded-lg border-2 border-gray-200 focus:border-green-500 focus:outline-none text-sm" />
                </div>

                <div class="bg-blue-50 border border-blue-200 rounded-lg p-5">
                  <h4 class="text-xs font-bold text-blue-900 uppercase tracking-wider mb-2">Upload PNG Bills</h4>
                  <p class="text-[11px] text-blue-700 mb-4">Upload your latest gas bills to verify consumption.</p>
                  
                  <div class="border-2 border-dashed border-blue-300 rounded-lg p-4 text-center hover:bg-blue-100/50 transition cursor-pointer relative">
                    <input type="file" (change)="onCookingFilesSelected($event)" accept=".pdf,.jpg,.jpeg,.png" multiple
                      class="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
                    <div class="text-blue-700">
                      <p class="text-xs font-bold">Click to upload gas bills</p>
                    </div>
                  </div>

                  @if (uploadedCookingDocs().length > 0) {
                    <div class="mt-3 space-y-2">
                      @for (doc of uploadedCookingDocs(); track doc.fileUrl) {
                        <div class="flex items-center justify-between bg-white/80 p-2 rounded border border-blue-100">
                          <div class="flex items-center gap-2 truncate">
                            <span class="text-xs text-blue-800 truncate max-w-[180px]">📄 {{ doc.originalFileName }}</span>
                          </div>
                          <button type="button" (click)="removeDoc($index)" class="text-red-400 hover:text-red-600 text-xs">✕</button>
                        </div>
                      }
                    </div>
                  }
                  @if (isUploadingCooking()) {
                    <div class="mt-2 flex items-center gap-2 text-[10px] text-blue-600 font-medium">
                      <span class="w-3 h-3 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></span>
                      Uploading bills...
                    </div>
                  }
                </div>
              </div>
            }

            @if (form.controls.fuelType.value === 'ELECTRIC') {
              <div class="bg-blue-50 border border-blue-100 rounded-lg p-4 flex items-start gap-3 animate-in fade-in slide-in-from-top-2">
                <span class="text-lg">⚡</span>
                <p class="text-blue-700 text-sm">Your induction/electric cooking emissions are calculated based on your total household electricity consumption.</p>
              </div>
            }

            @if (form.controls.fuelType.value === 'BIOGAS') {
              <div class="bg-green-50 border border-green-100 rounded-lg p-4 flex items-start gap-3 animate-in fade-in slide-in-from-top-2">
                <span class="text-lg">🌿</span>
                <p class="text-green-700 text-sm font-medium">Biogas is carbon-neutral! No additional data is required as emissions are negligible.</p>
              </div>
            }

            <div class="flex justify-between pt-6 border-t border-gray-100">
              <button type="button" (click)="goBack()" class="px-6 py-3 bg-gray-100 text-gray-700 rounded-lg font-semibold hover:bg-gray-200 transition-colors">
                ← Back
              </button>
              <button type="submit" [disabled]="loading() || isUploadingCooking()"
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
export class DeclarationCookingComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly declarationService = inject(DeclarationService);
  private readonly fileUploadService = inject(FileUploadService);
  private readonly toastService = inject(ToastService);

  loading = signal(false);
  declarationId = 0;
  
  uploadedCookingDocs = signal<UploadedFile[]>([]);
  isUploadingCooking = signal(false);

  readonly fuelOptions: { value: CookingFuel; label: string; icon: string }[] = [
    { value: 'LPG', label: 'LPG Cylinder', icon: '🔴' },
    { value: 'PNG', label: 'PNG Piped Gas', icon: '🔵' },
    { value: 'ELECTRIC', label: 'Electric Cooking', icon: '⚡' },
    { value: 'BIOGAS', label: 'Biogas', icon: '🌿' },
  ];

  form = this.fb.group({
    fuelType: ['LPG' as CookingFuel, [Validators.required]],
    pngConsumerNumber: [''],
    userDeclaredCylinders: [null as number | null],
  });

  ngOnInit(): void {
    this.declarationId = Number(this.route.snapshot.paramMap.get('id'));
  }

  selectFuel(fuel: CookingFuel): void {
    this.form.controls.fuelType.setValue(fuel);
    // Clear uploaded docs if fuel type changes to something that doesn't need them
    if (fuel === 'ELECTRIC' || fuel === 'BIOGAS') {
      this.uploadedCookingDocs.set([]);
    }
  }

  onCookingFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    const files = Array.from(input.files);
    this.isUploadingCooking.set(true);

    this.fileUploadService.uploadMultipleFiles(files, 'cooking').subscribe({
      next: (uploaded) => {
        this.uploadedCookingDocs.update(docs => [...docs, ...uploaded]);
        this.isUploadingCooking.set(false);
        this.toastService.success(`${uploaded.length} file(s) uploaded successfully`);
      },
      error: (err: Error) => {
        this.isUploadingCooking.set(false);
        this.toastService.error(err.message);
      }
    });
    input.value = '';
  }

  removeDoc(index: number): void {
    this.uploadedCookingDocs.update(docs => docs.filter((_, i) => i !== index));
  }

  onSave(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    if (this.isUploadingCooking()) return;

    this.loading.set(true);
    const f = this.form.controls;
    
    // Serialize bill URLs as JSON array string for the TEXT column in DB
    const billUrlsJson = JSON.stringify(this.uploadedCookingDocs().map(d => d.fileUrl));

    this.declarationService.saveCooking(this.declarationId, {
      fuelType: f.fuelType.value!,
      pngConsumerNumber: f.pngConsumerNumber.value || undefined,
      userDeclaredCylinders: f.userDeclaredCylinders.value ?? undefined,
      billUrls: billUrlsJson
    }).subscribe({
      next: () => { 
        this.loading.set(false); 
        this.toastService.success('Cooking data saved');
        const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
        if (returnTo === 'review') {
          this.router.navigate(['/declaration', this.declarationId, 'review']);
        } else {
          this.router.navigate(['/declaration', this.declarationId, 'lifestyle']); 
        }
      },
      error: () => { 
        this.loading.set(false);
        this.toastService.error('Failed to save cooking data');
      }
    });
  }

  goBack(): void { this.router.navigate(['/declaration', this.declarationId, 'solar']); }
}
