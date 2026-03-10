import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AgentService } from '../../../../core/services/agent.service';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { AgentTaskResponse, VerificationRequest } from '../../../../core/models/agent';
import { DeclarationResponse } from '../../../../core/models/declaration';

@Component({
    selector: 'app-verify',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    templateUrl: './verify.component.html'
})
export class VerifyComponent implements OnInit {
    private agentService = inject(AgentService);
    private declarationService = inject(DeclarationService);
    private fb = inject(FormBuilder);
    private route = inject(ActivatedRoute);
    private router = inject(Router);

    assignmentId = signal<number>(0);
    task = signal<AgentTaskResponse | null>(null);
    declaration = signal<DeclarationResponse | null>(null);


    submitting = signal<boolean>(false);
    error = signal<string>('');
    gpsStatus = signal<string>('Detecting location...');

    verifyForm: FormGroup = this.fb.group({
        overallAction: ['CONFIRMED', Validators.required],
        agentRemarks: [''],

        // Explicit correction toggles to show/hide fields
        correctElectricity: [false],
        correctSolar: [false],
        correctPublicTransport: [false],
        correctAc: [false],
        correctGenerator: [false],
        correctShopping: [false],

        // Actual correction values
        correctedElectricityUnits: [null],
        correctedSolarUnits: [null],
        correctedPublicTransportKm: [null],
        correctedGeneratorHours: [null],
        correctedShoppingOrders: [null],
    });

    ngOnInit(): void {
        const idParam = this.route.snapshot.paramMap.get('id');
        if (idParam) {
            this.assignmentId.set(Number(idParam));
            this.loadAssignmentAndDeclaration();

            // Setup dynamic validation for remarks
            this.verifyForm.get('overallAction')?.valueChanges.subscribe(action => {
                const remarksCtrl = this.verifyForm.get('agentRemarks');
                if (action === 'MODIFIED' || action === 'REJECTED') {
                    remarksCtrl?.setValidators([Validators.required, Validators.minLength(10)]);
                } else {
                    remarksCtrl?.clearValidators();
                }
                remarksCtrl?.updateValueAndValidity();
            });
        } else {
            this.error.set('Invalid assignment ID.');
        }
    }

    loadAssignmentAndDeclaration(): void {
        // First get assignment to find declarationId
        this.agentService.getAssignment(this.assignmentId()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.task.set(res.data);
                    // Then get the declaration details
                    this.loadDeclaration(res.data.declarationId);
                } else {
                    this.error.set(res.error || 'Failed to load assignment.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load assignment.');
            }
        });
    }

    loadDeclaration(declarationId: number): void {
        this.agentService.getDeclarationForAssignment(this.assignmentId()).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.declaration.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load declaration.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load declaration.');
            }
        });
    }

    onSubmit(): void {
        if (this.verifyForm.invalid) {
            this.verifyForm.markAllAsTouched();
            return;
        }

        this.submitting.set(true);
        this.error.set('');
        this.gpsStatus.set('Acquiring GPS coordinates...');

        if ('geolocation' in navigator) {
            navigator.geolocation.getCurrentPosition(
                (position) => {
                    this.gpsStatus.set('Coordinates acquired. Submitting...');
                    this.finalizeSubmission(position.coords.latitude, position.coords.longitude);
                },
                (err) => {
                    // If denied/failed, we send dummy coordinates or handle it accordingly
                    console.warn('Geolocation failed, proceeding with 0,0', err);
                    this.gpsStatus.set('GPS failed, submitting with unknown location.');
                    this.finalizeSubmission(0, 0); // Real app might block this
                },
                { timeout: 10000 }
            );
        } else {
            this.gpsStatus.set('GPS unavailable, submitting without location data.');
            this.finalizeSubmission(0, 0);
        }
    }

    private finalizeSubmission(lat: number, lng: number): void {
        const formVals = this.verifyForm.value;

        const request: VerificationRequest = {
            overallAction: formVals.overallAction,
            agentRemarks: formVals.agentRemarks || undefined,
            agentGpsLat: lat,
            agentGpsLng: lng,

            // Conditionally include corrected fields ONLY if toggled
            correctedElectricityUnits: formVals.correctElectricity ? formVals.correctedElectricityUnits : undefined,
            correctedSolarUnits: formVals.correctSolar ? formVals.correctedSolarUnits : undefined,
            correctedPublicTransportKm: formVals.correctPublicTransport ? formVals.correctedPublicTransportKm : undefined,
            correctedGeneratorHours: formVals.correctGenerator ? formVals.correctedGeneratorHours : undefined,
            correctedShoppingOrders: formVals.correctShopping ? formVals.correctedShoppingOrders : undefined,
        };

        console.log('Sending verification payload:', request);

        this.agentService.submitVerification(this.assignmentId(), request).subscribe({
            next: (res) => {
                this.submitting.set(false);
                if (res.success) {
                    this.router.navigate(['/agent/dashboard']);
                } else {
                    this.error.set(res.error || 'Verification failed unexpectedly.');
                }
            },
            error: (err) => {
                this.submitting.set(false);
                this.error.set(err.error?.error || 'An error occurred during submission.');
            }
        });
    }
}
