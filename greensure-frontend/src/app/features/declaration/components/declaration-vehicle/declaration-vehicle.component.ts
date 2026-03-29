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
  mode = signal<'LIST' | 'FORM'>('LIST');
  editingVehicleId = signal<number | null>(null);
  uploadingVehicleDocId = signal<number | null>(null);

  // Per-vehicle upload state
  private selectedFileByVehicle = signal<Record<number, File | null>>({});
  private docTypeByVehicle = signal<Record<number, VehicleDocumentType>>({});

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
            this.mode.set('LIST');
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
    this.editingVehicleId.set(null);
    this.mode.set('FORM');
  }

  setListMode(): void {
    this.mode.set('LIST');
  }

  setEditMode(vehicle: VehicleData): void {
    this.form.reset({
      vehicleCategory: vehicle.vehicleCategory,
      vehicleNickname: vehicle.vehicleNickname ?? '',
      vin: '',
      registrationNumber: vehicle.registrationNumber,
      make: vehicle.make,
      model: vehicle.model,
      year: vehicle.year,
      fuelType: vehicle.fuelType,
      mileageBand: vehicle.mileageBand,
    });
    this.editingVehicleId.set(vehicle.vehicleId);
    this.mode.set('FORM');
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
      dataSource: 'MANUAL',
    };

    const editingId = this.editingVehicleId();
    const request = editingId
      ? this.declarationService.updateVehicle(this.declarationId, editingId, data)
      : this.declarationService.addVehicle(this.declarationId, data);

    request.subscribe({
      next: (res) => {
        if (res.success && res.data) {
          if (editingId) {
            this.vehicles.set(this.vehicles().map(v => v.vehicleId === editingId ? res.data! : v));
            this.toastService.success('Vehicle details updated');
          } else {
            const vList = this.vehicles();
            this.vehicles.set([...vList, res.data]);
            this.toastService.success('Vehicle added');
          }
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

  private fileUploadService = inject(FileUploadService);
  private toastService = inject(ToastService);

  onVehicleFileChange(event: Event, vehicleId: number): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.selectedFileByVehicle.update(prev => ({ ...prev, [vehicleId]: file }));
    }
    input.value = '';
  }

  onDocTypeChange(vehicleId: number, value: VehicleDocumentType): void {
    this.docTypeByVehicle.update(prev => ({ ...prev, [vehicleId]: value }));
  }

  getDocType(vehicleId: number): VehicleDocumentType {
    return this.docTypeByVehicle()[vehicleId] ?? 'RC_BOOK';
  }

  getSelectedFileName(vehicleId: number): string | null {
    return this.selectedFileByVehicle()[vehicleId]?.name ?? null;
  }

  onUploadDoc(vehicleId: number): void {
    const selectedFile = this.selectedFileByVehicle()[vehicleId];
    if (!selectedFile) return;

    this.uploadingVehicleDocId.set(vehicleId);
    const docType = this.getDocType(vehicleId);

    this.fileUploadService.uploadFile(selectedFile, 'vehicle').subscribe({
      next: (uploaded: UploadedFile) => {
        const req: UploadVehicleDocumentRequest = {
          vehicleId: vehicleId,
          documentType: docType,
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
              this.selectedFileByVehicle.update(prev => ({ ...prev, [vehicleId]: null }));
              this.toastService.success('Document uploaded');
            }
            this.uploadingVehicleDocId.set(null);
          },
          error: () => {
            this.uploadingVehicleDocId.set(null);
            this.toastService.error('Failed to link uploaded document to this vehicle');
          }
        });
      },
      error: (err: Error) => {
        this.uploadingVehicleDocId.set(null);
        this.toastService.error(err.message);
      }
    });
  }

  removeVehicleDocument(vehicleId: number, documentId: number): void {
    this.declarationService.removeVehicleDocument(this.declarationId, vehicleId, documentId).subscribe({
      next: (res) => {
        if (res.success) {
          this.vehicles.set(this.vehicles().map(v => {
            if (v.vehicleId !== vehicleId) return v;
            return { ...v, documents: (v.documents || []).filter(d => d.documentId !== documentId) };
          }));
          this.toastService.success('Document removed');
        } else {
          this.toastService.error(res.error || 'Failed to remove document');
        }
      },
      error: () => this.toastService.error('Failed to remove document')
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
