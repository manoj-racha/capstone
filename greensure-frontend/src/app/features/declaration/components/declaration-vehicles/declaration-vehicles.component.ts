import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { VehicleResponse } from '../../../../core/models/declaration';

@Component({
    selector: 'app-declaration-vehicles',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    templateUrl: './declaration-vehicles.component.html'
})
export class DeclarationVehiclesComponent implements OnInit {
    private fb = inject(FormBuilder);
    private declarationService = inject(DeclarationService);
    private route = inject(ActivatedRoute);
    private router = inject(Router);

    declarationId = signal<number>(0);
    vehicles = signal<VehicleResponse[]>([]);

    error = signal<string>('');

    vehicleForm: FormGroup = this.fb.group({
        vehicleType: ['TWO_WHEELER', Validators.required],
        fuelType: ['PETROL', Validators.required],
        kmPerMonth: [null, [Validators.required, Validators.min(0)]],
        quantity: [1, [Validators.required, Validators.min(1)]]
    });

    ngOnInit(): void {
        const idParam = this.route.snapshot.paramMap.get('id');
        if (idParam) {
            this.declarationId.set(Number(idParam));
            this.loadDeclaration();
        } else {
            this.error.set('Invalid declaration ID.');
        }
    }

    loadDeclaration(): void {
        this.declarationService.getDeclaration(this.declarationId()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.vehicles.set(res.data.vehicles || []);
                } else {
                    this.error.set(res.error || 'Failed to load vehicles.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load vehicles.');
            }
        });
    }

    onAddVehicle(): void {
        if (this.vehicleForm.invalid) {
            this.vehicleForm.markAllAsTouched();
            this.scrollToFirstInvalid();
            return;
        }

        this.error.set('');

        const formValue = this.vehicleForm.value;

        this.declarationService.addVehicle(this.declarationId(), formValue).subscribe({
            next: (res) => {
                // this.adding.set(false);
                if (res.success && res.data) {
                    this.vehicles.update(v => [...v, res.data!]);
                    // Reset form to defaults
                    this.vehicleForm.reset({
                        vehicleType: 'TWO_WHEELER',
                        fuelType: 'PETROL',
                        quantity: 1
                    });
                } else {
                    this.error.set(res.error || 'Failed to add vehicle.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to add vehicle.');
            }
        });
    }

    onRemoveVehicle(vehicleId: number): void {
        this.error.set('');
        // Optimistic UI update could be done, but let's wait for API success
        this.declarationService.removeVehicle(this.declarationId(), vehicleId).subscribe({
            next: (res) => {
                if (res.success) {
                    this.vehicles.update(v => v.filter(veh => veh.vehicleId !== vehicleId));
                } else {
                    this.error.set(res.error || 'Failed to remove vehicle.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to remove vehicle.');
            }
        });
    }

    onNext(): void {
        this.router.navigate(['/declaration/review', this.declarationId()]);
    }

    private scrollToFirstInvalid(): void {
        setTimeout(() => {
            const firstInvalidControl = document.querySelector('.ng-invalid');
            if (firstInvalidControl) {
                firstInvalidControl.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }, 100);
    }
}
