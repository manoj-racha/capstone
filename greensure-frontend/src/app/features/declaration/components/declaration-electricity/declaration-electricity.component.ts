import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { FileUploadService } from '../../../../core/services/file-upload.service';
import { ToastService } from '../../../../core/services/toast.service';
import { DeclarationProgressComponent } from '../../../../shared/components/declaration-progress/declaration-progress.component';

interface BillEntry {
  file: File;
  fileUrl: string | null;
  isUploading: boolean;
  uploadError: string | null;
}

@Component({
  selector: 'app-declaration-electricity',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DeclarationProgressComponent],
  template: `
    <div class="min-h-screen bg-gs-bg py-10 px-4">
      <div class="max-w-2xl mx-auto">
        <app-declaration-progress [currentStep]="3" [completedSteps]="[1,2]" />

        <div class="bg-white rounded-xl border border-gray-200 p-8 shadow-sm">
          <h2 class="text-2xl font-bold text-gray-900 mb-2">Electricity Usage</h2>
          <p class="text-gray-500 text-sm mb-6">Provide your electricity consumption details for energy emission calculation.</p>

          <form [formGroup]="form" (ngSubmit)="onSave()" class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-1.5">Electricity Provider <span class="text-red-500">*</span></label>
                <select formControlName="provider"
                  class="w-full px-3 py-2.5 rounded-lg border-2 border-gray-200 focus:border-green-500 focus:outline-none text-sm">
                  <option value="">Select provider</option>
                  @for (p of providers; track p) {
                    <option [value]="p">{{ p }}</option>
                  }
                </select>
              </div>

              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-1.5">Consumer Number <span class="text-red-500">*</span></label>
                <input type="text" formControlName="consumerNumber" placeholder="Your consumer number"
                  class="w-full px-3 py-2.5 rounded-lg border-2 border-gray-200 focus:border-green-500 focus:outline-none text-sm" />
              </div>
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-1.5">Estimated Annual Avg. Monthly Usage (kWh) <span class="text-red-500">*</span></label>
              <input type="number" formControlName="userDeclaredMonthlyKwh" placeholder="e.g. 200" min="0"
                class="w-full px-3 py-2.5 rounded-lg border-2 border-gray-200 focus:border-green-500 focus:outline-none text-sm bg-gray-50 font-semibold" />
              <p class="text-gray-400 text-xs mt-1">This remains your manual value. Bill uploads will not override it.</p>
            </div>

            <!-- Bill Upload Section -->
            <div class="bg-blue-50 border border-blue-200 rounded-lg p-6 space-y-4">
              <div class="flex justify-between items-center">
                <h3 class="text-sm font-bold text-blue-900 flex items-center gap-2">
                  <span>📄 Upload Bills & Verification</span>
                  <span class="text-xs font-normal text-blue-600 bg-blue-100 px-2 py-0.5 rounded-full">Secure Upload</span>
                </h3>
              </div>
              
              <p class="text-xs text-blue-700">Upload one PDF containing the previous 12 months electricity bills.</p>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-3 bg-white border border-blue-100 rounded-lg p-3">
                <div>
                  <label class="text-[10px] uppercase font-bold text-blue-800 block mb-1">Starting Month</label>
                  <input
                    type="month"
                    [max]="currentMonth"
                    [ngModel]="billingStartMonth()"
                    [ngModelOptions]="{standalone: true}"
                    (ngModelChange)="onBillingRangeChange($event, billingEndMonth())"
                    class="w-full text-xs border border-blue-200 rounded px-2 py-1.5 focus:border-blue-400 outline-none bg-white" />
                </div>
                <div>
                  <label class="text-[10px] uppercase font-bold text-blue-800 block mb-1">Ending Month</label>
                  <input
                    type="month"
                    [max]="currentMonth"
                    [ngModel]="billingEndMonth()"
                    [ngModelOptions]="{standalone: true}"
                    (ngModelChange)="onBillingRangeChange(billingStartMonth(), $event)"
                    class="w-full text-xs border border-blue-200 rounded px-2 py-1.5 focus:border-blue-400 outline-none bg-white" />
                </div>
              </div>
              @if (billingRangeError()) {
                <p class="text-xs text-red-600 font-medium">{{ billingRangeError() }}</p>
              } @else if (billingStartMonth() && billingEndMonth()) {
                <p class="text-xs text-blue-700">Billing period validated for 12 months.</p>
              }
              
              <div class="border-2 border-dashed border-blue-300 rounded-lg p-6 text-center hover:bg-blue-100/50 transition cursor-pointer relative">
                <input type="file" (change)="onBillFilesSelected($event)" accept=".pdf"
                  class="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
                <div class="text-blue-600">
                  <p class="text-sm font-bold">Click to upload one PDF (12 bills)</p>
                  <p class="text-[10px] opacity-75 mt-1">PDF only (Max 10MB)</p>
                </div>
              </div>

              @if (uploadedBill()) {
                <div class="bg-white border border-blue-100 rounded-lg p-3 shadow-sm mt-4">
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2 truncate">
                      <span class="text-blue-500">📄</span>
                      <span class="text-xs font-medium text-gray-700 truncate max-w-[200px]">{{ uploadedBill()!.file.name }}</span>
                      @if (uploadedBill()!.isUploading) {
                        <span class="w-3 h-3 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></span>
                      }
                      @if (uploadedBill()!.fileUrl && !uploadedBill()!.isUploading) {
                        <span class="text-green-600 text-[10px] font-bold">✓ Uploaded</span>
                      }
                    </div>
                    <button type="button" (click)="removeBill()" class="text-red-400 hover:text-red-600">✕</button>
                  </div>

                  @if (uploadedBill()!.uploadError) {
                    <p class="text-[10px] text-red-500 mt-2 font-medium">{{ uploadedBill()!.uploadError }}</p>
                  }
                </div>
              }
            </div>

            <div class="flex justify-between pt-6 border-t border-gray-100">
              <button type="button" (click)="goBack()" class="px-6 py-3 bg-gray-100 text-gray-700 rounded-lg font-semibold hover:bg-gray-200 transition-colors">
                ← Back
              </button>
              <button type="submit" [disabled]="loading() || isAnyUploading()"
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
export class DeclarationElectricityComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly declarationService = inject(DeclarationService);
  private readonly fileUploadService = inject(FileUploadService);
  private readonly toastService = inject(ToastService);

  loading = signal(false);
  declarationId = 0;
  uploadedBill = signal<BillEntry | null>(null);
  billingStartMonth = signal<string>('');
  billingEndMonth = signal<string>('');
  billingRangeError = signal<string | null>(null);

  readonly providers = [
    'BESCOM', 'MESCOM', 'HESCOM', 'GESCOM', 'CESC',
    'TPDDL', 'BSES Rajdhani', 'BSES Yamuna', 'MSEDCL',
    'TNEB', 'KSEB', 'APEPDCL', 'TSSPDCL', 'Other'
  ];

  readonly currentMonth = new Date().toISOString().slice(0, 7);

  form = this.fb.group({
    provider: ['', [Validators.required]],
    consumerNumber: ['', [Validators.required]],
    userDeclaredMonthlyKwh: [null as number | null, [Validators.required, Validators.min(0)]]
  });

  ngOnInit(): void {
    this.declarationId = Number(this.route.snapshot.paramMap.get('id'));
    this.initializeFixedBillingRange();
    this.loadExistingData();
  }

  private loadExistingData(): void {
    this.declarationService.getDeclarationById(this.declarationId).subscribe({
      next: (res) => {
        const e = res?.data?.electricityData;
        if (res.success && e) {
          this.form.patchValue({
            provider: e.provider ?? '',
            consumerNumber: e.consumerNumber ?? '',
            userDeclaredMonthlyKwh: e.userDeclaredMonthlyKwh ?? null
          });
        }
      }
    });
  }

  onBillingRangeChange(startMonth: string, endMonth: string): void {
    this.billingStartMonth.set(startMonth || '');
    this.billingEndMonth.set(endMonth || '');
    this.validateBillingRange();
  }

  onBillFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const entry: BillEntry = {
      file,
      fileUrl: null,
      isUploading: true,
      uploadError: null
    };

    this.uploadedBill.set(entry);

    this.fileUploadService.uploadFile(file, 'electricity').subscribe({
      next: (uploaded) => {
        this.uploadedBill.update(current => {
          if (!current) return current;
          return { ...current, fileUrl: uploaded.fileUrl, isUploading: false, uploadError: null };
        });
      },
      error: (err: Error) => {
        this.uploadedBill.update(current => {
          if (!current) return current;
          return { ...current, isUploading: false, uploadError: err.message };
        });
      }
    });

    input.value = '';
  }

  private initializeFixedBillingRange(): void {
    const months = this.previous12Months();
    if (months.length === 0) {
      this.billingStartMonth.set('');
      this.billingEndMonth.set('');
      return;
    }

    this.billingStartMonth.set(months[0]);
    this.billingEndMonth.set(months[months.length - 1]);
    this.validateBillingRange();
  }

  private toMonthIndex(value: string): number {
    const [y, m] = value.split('-').map(Number);
    return (y * 12) + (m - 1);
  }

  private isFutureMonth(value: string): boolean {
    return this.toMonthIndex(value) > this.toMonthIndex(this.currentMonth);
  }

  private validateBillingRange(): boolean {
    const start = this.billingStartMonth();
    const end = this.billingEndMonth();

    if (!start || !end) {
      this.billingRangeError.set('Please select both starting and ending months.');
      return false;
    }

    if (this.isFutureMonth(start) || this.isFutureMonth(end)) {
      this.billingRangeError.set('Future months are not allowed.');
      return false;
    }

    const startIndex = this.toMonthIndex(start);
    const endIndex = this.toMonthIndex(end);

    if (startIndex > endIndex) {
      this.billingRangeError.set('Starting month must be before ending month.');
      return false;
    }

    if ((endIndex - startIndex + 1) !== 12) {
      this.billingRangeError.set('Billing period must span exactly 12 months.');
      return false;
    }

    this.billingRangeError.set(null);
    return true;
  }

  private monthRangeFromIndexes(startIndex: number, endIndex: number): string[] {
    const result: string[] = [];
    let i = startIndex;
    const max = endIndex;

    while (i <= max) {
      const year = Math.floor(i / 12);
      const month = (i % 12) + 1;
      result.push(`${year}-${String(month).padStart(2, '0')}`);
      i += 1;
    }

    return result;
  }

  private previous12Months(): string[] {
    const endIndex = this.toMonthIndex(this.currentMonth);
    const startIndex = endIndex - 11;
    return this.monthRangeFromIndexes(startIndex, endIndex);
  }

  private selected12Months(): string[] {
    if (!this.validateBillingRange()) {
      return [];
    }

    const start = this.billingStartMonth();
    const end = this.billingEndMonth();
    const startIndex = this.toMonthIndex(start);
    const endIndex = this.toMonthIndex(end);
    return this.monthRangeFromIndexes(startIndex, endIndex);
  }

  removeBill(): void {
    this.uploadedBill.set(null);
  }

  isAnyUploading(): boolean {
    return this.uploadedBill()?.isUploading ?? false;
  }

  onSave(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    if (this.isAnyUploading()) return;

    if (!this.validateBillingRange()) {
      this.toastService.error(this.billingRangeError() || 'Invalid billing period.');
      return;
    }

    this.loading.set(true);
    const f = this.form.controls;
    
    // Save main electricity data
    this.declarationService.saveElectricity(this.declarationId, {
      provider: f.provider.value ?? '',
      consumerNumber: f.consumerNumber.value ?? '',
      userDeclaredMonthlyKwh: f.userDeclaredMonthlyKwh.value!
    }).subscribe({
      next: () => {
        const uploaded = this.uploadedBill();
        const billUrl = uploaded?.fileUrl;

        if (!billUrl) {
          this.completeNavigation();
          return;
        }

        const usage = f.userDeclaredMonthlyKwh.value!;
        const selectedMonths = this.selected12Months();
        if (selectedMonths.length !== 12) {
          this.loading.set(false);
          this.toastService.error('Billing period must span exactly 12 months.');
          return;
        }

        const billSaves = selectedMonths.map(month =>
          this.declarationService.addElectricityBill(
            this.declarationId,
            month,
            usage,
            undefined,
            billUrl
          )
        );

        if (billSaves.length === 0) {
          this.completeNavigation();
          return;
        }

        import('rxjs').then(({ forkJoin }) => {
          forkJoin(billSaves).subscribe({
            next: () => this.completeNavigation(),
            error: () => {
              this.loading.set(false);
              this.toastService.error('Main data saved, but some bills failed to link.');
            }
          });
        });
      },
      error: () => { 
        this.loading.set(false);
        this.toastService.error('Failed to save electricity data.');
      }
    });
  }

  private completeNavigation(): void {
    this.loading.set(false); 
    const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
    if (returnTo === 'review') {
      this.router.navigate(['/declaration', this.declarationId, 'review']);
    } else {
      this.router.navigate(['/declaration', this.declarationId, 'solar']); 
    }
  }

  goBack(): void { this.router.navigate(['/declaration', this.declarationId, 'vehicle']); }
}
