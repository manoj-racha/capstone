import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { DeclarationProgressComponent } from '../../../../shared/components/declaration-progress/declaration-progress.component';
import {
  FuelType, MileageBand, MILEAGE_BAND_LABELS,
  VehicleCategory, VEHICLE_CATEGORY_LABELS, AddVehicleRequest, VehicleData, UploadVehicleDocumentRequest,
  VehicleDocumentType
} from '../../../../core/models/declaration';
import { FileUploadService, UploadedFile } from '../../../../core/services/file-upload.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-declaration-vehicle',
  standalone: true,
  imports: [ReactiveFormsModule, DeclarationProgressComponent],
  templateUrl: './declaration-vehicle.component.html'
})
export class DeclarationVehicleComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly declarationService = inject(DeclarationService);

  declarationId = 0;
  
  // State
  vehicles = signal<VehicleData[]>([]);
  loading = signal(false);
  mode = signal<'LIST' | 'ADD'>('LIST');
  vahanMode = signal<'VAHAN' | 'MANUAL'>('VAHAN');
  vahanVerified = signal(false);

  // Constants
  readonly fuelTypes: FuelType[] = ['EV', 'PETROL', 'DIESEL', 'CNG'];
  readonly mileageBands: MileageBand[] = ['BAND_1', 'BAND_2', 'BAND_3', 'BAND_4', 'BAND_5'];
  readonly mileageBandLabels = MILEAGE_BAND_LABELS;
  readonly categories: VehicleCategory[] = ['TWO_WHEELER', 'THREE_WHEELER', 'FOUR_WHEELER', 'OTHER'];
  readonly categoryLabels = VEHICLE_CATEGORY_LABELS;

  // Form for Add Vehicle
  form = this.fb.group({
    vehicleCategory: ['FOUR_WHEELER' as VehicleCategory, [Validators.required]],
    vehicleNickname: [''],
    vin: [''],
    registrationNumber: ['', [Validators.required]],
    make: ['', [Validators.required]],
    model: ['', [Validators.required]],
    year: [2023, [Validators.required, Validators.min(1990), Validators.max(2030)]],
    fuelType: ['PETROL' as FuelType, [Validators.required]],
    mileageBand: ['BAND_3' as MileageBand, [Validators.required]],
  });

  // Selected vehicle for document upload
  selectedVehicleForDoc = signal<VehicleData | null>(null);
  
  // Form for Document Upload
  docForm = this.fb.group({
    documentType: ['RC_BOOK' as VehicleDocumentType, [Validators.required]],
    documentUrl: ['', [Validators.required]],
    originalFileName: ['', [Validators.required]]
  });
  
  uploadingDoc = signal(false);

  ngOnInit(): void {
    this.declarationId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadVehicles();
  }

  loadVehicles(): void {
    this.loading.set(true);
    this.declarationService.getVehicles(this.declarationId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.vehicles.set(res.data);
          if (res.data.length === 0) {
            this.mode.set('ADD');
          }
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  setAddMode(): void {
    this.form.reset({
      vehicleCategory: 'FOUR_WHEELER',
      year: 2023,
      fuelType: 'PETROL',
      mileageBand: 'BAND_3'
    });
    this.vahanMode.set('VAHAN');
    this.vahanVerified.set(false);
    this.mode.set('ADD');
  }

  setListMode(): void {
    this.mode.set('LIST');
  }

  setVahanMode(m: 'VAHAN' | 'MANUAL'): void {
    this.vahanMode.set(m);
    this.vahanVerified.set(false);
  }

  onSaveVehicle(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    const f = this.form.controls;
    const data: AddVehicleRequest = {
      vehicleCategory: f.vehicleCategory.value!,
      vehicleNickname: f.vehicleNickname.value || undefined,
      vin: f.vin.value || undefined,
      registrationNumber: f.registrationNumber.value ?? '',
      make: f.make.value ?? '',
      model: f.model.value ?? '',
      year: f.year.value ?? 2023,
      fuelType: f.fuelType.value!,
      mileageBand: f.mileageBand.value!,
      dataSource: this.vahanMode(),
    };
    
    this.declarationService.addVehicle(this.declarationId, data).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const vList = this.vehicles();
          this.vehicles.set([...vList, res.data]);
          this.setListMode();
        }
        this.loading.set(false);
      },
      error: () => { this.loading.set(false); }
    });
  }

  removeVehicle(vehicleId: number): void {
    if (!confirm('Are you sure you want to remove this vehicle?')) return;
    this.loading.set(true);
    this.declarationService.removeVehicle(this.declarationId, vehicleId).subscribe({
      next: () => {
        const vList = this.vehicles().filter(x => x.vehicleId !== vehicleId);
        this.vehicles.set(vList);
        if (vList.length === 0) {
          this.setAddMode();
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  openDocModal(vehicle: VehicleData): void {
    this.selectedVehicleForDoc.set(vehicle);
    this.docForm.reset({ documentType: 'RC_BOOK' });
  }

  closeDocModal(): void {
    this.selectedVehicleForDoc.set(null);
  }

  private fileUploadService = inject(FileUploadService);
  private toastService = inject(ToastService);

  selectedFile = signal<File | null>(null);

  // Handle mock file upload for UI
  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.selectedFile.set(file);
      this.docForm.patchValue({
        documentUrl: 'placeholder',
        originalFileName: file.name
      });
    }
  }

  onUploadDoc(): void {
    if (this.docForm.invalid || !this.selectedVehicleForDoc() || !this.selectedFile()) return;
    this.uploadingDoc.set(true);
    
    const f = this.docForm.controls;
    const vehicleId = this.selectedVehicleForDoc()!.vehicleId;
    
    this.fileUploadService.uploadFile(this.selectedFile()!, 'vehicle').subscribe({
      next: (uploaded: UploadedFile) => {
        const req: UploadVehicleDocumentRequest = {
          vehicleId: vehicleId,
          documentType: f.documentType.value!,
          documentUrl: uploaded.fileUrl,
          originalFileName: uploaded.originalFileName,
          mimeType: uploaded.mimeType,
          fileSizeBytes: uploaded.fileSizeBytes
        };

        this.declarationService.addVehicleDocument(this.declarationId, vehicleId, req).subscribe({
          next: (res) => {
            if (res.success && res.data) {
              const updatedVs = this.vehicles().map(v => {
                if (v.vehicleId === vehicleId) {
                  return { ...v, documents: [...(v.documents || []), res.data!] };
                }
                return v;
              });
              this.vehicles.set(updatedVs);
              this.toastService.success('Document safely uploaded');
              this.closeDocModal();
            }
            this.uploadingDoc.set(false);
          },
          error: () => {
            this.uploadingDoc.set(false);
            this.toastService.error('Failed to link document proxy to vehicle');
          }
        });
      },
      error: (err: Error) => {
        this.uploadingDoc.set(false);
        this.toastService.error(err.message);
      }
    });
  }

  continueToNext(): void {
    const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
    if (returnTo === 'review') {
      this.router.navigate(['/declaration', this.declarationId, 'review']);
    } else {
      this.router.navigate(['/declaration', this.declarationId, 'electricity']);
    }
  }

  goBack(): void { 
    this.router.navigate(['/declaration', this.declarationId, 'household']); 
  }
}
