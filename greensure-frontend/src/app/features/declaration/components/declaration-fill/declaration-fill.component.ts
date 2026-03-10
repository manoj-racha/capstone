import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { DeclarationRequest } from '../../../../core/models/declaration';

@Component({
  selector: 'app-declaration-fill',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './declaration-fill.component.html'
})
export class DeclarationFillComponent implements OnInit {
  private fb = inject(FormBuilder);
  private declarationService = inject(DeclarationService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  declarationId = signal<number>(0);
  userType = signal<string>('HOUSEHOLD');

  error = signal<string>('');

  fillForm: FormGroup = this.fb.group({
    // Energy
    electricityUnits: [null],
    hasSolar: [false],
    solarUnits: [null],
    cookingFuelType: ['NONE'],
    lpgCylinders: [null],
    pngUnits: [null],
    biomassKgPerDay: [null],
    numAcUnits: [null],
    acHoursPerDay: [null],
    hasGenerator: [false],
    generatorHoursPerMonth: [null],

    // Transport
    usesPublicTransport: [false],
    publicTransportKm: [null],

    // Lifestyle — Household
    dietaryPattern: ['VEGETARIAN'],
    shoppingOrdersPerMonth: ['ZERO_TO_FIVE'],

    // Operations — MSME
    hasCommercialVehicles: [false],
    commercialVehicleKm: [null],
    thirdPartyShipments: [null],
    employeesPrivateVehicle: [null],
    employeesPublicTransport: [null],
    generatorLitersPerMonth: [null],
    hasBoiler: [false],
    boilerFuelType: ['NONE'],
    boilerCoalKg: [null],
    boilerGasScm: [null],
    paperReamsPerMonth: [null],
    usesRecycledPaper: [false],
    rawMaterialType: ['VIRGIN'],
    rawMaterialKg: [null]
  });

  ngOnInit(): void {
    const type = localStorage.getItem('userType');
    if (type) {
      this.userType.set(type);
    }

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
          this.fillForm.patchValue(res.data);
        } else {
          this.error.set(res.error || 'Failed to load declaration.');
        }
      },
      error: (err) => {
        this.error.set(err.error?.error || 'Failed to load declaration.');
      }
    });
  }

  onSaveAndContinue(): void {
    this.error.set('');

    const formValue = this.fillForm.value as DeclarationRequest;

    this.declarationService.saveDraft(this.declarationId(), formValue).subscribe({
      next: (res) => {
        if (res.success) {
          // Navigate to add vehicles step
          this.router.navigate(['/declaration/vehicles', this.declarationId()]);
        } else {
          this.error.set(res.error || 'Failed to save draft.');
        }
      },
      error: (err) => {
        this.error.set(err.error?.error || 'An error occurred while saving.');
      }
    });
  }
}
