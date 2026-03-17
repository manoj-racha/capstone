import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { DeclarationService } from '../../../../features/declaration/services/declaration.service';
import { DeclarationRequest, DeclarationResponse } from '../../../../core/models/declaration';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
    selector: 'app-declaration-history',
    standalone: true,
    imports: [CommonModule, RouterLink, DatePipe],
    templateUrl: './declaration-history.component.html'
})
export class DeclarationHistoryComponent implements OnInit {
    private declarationService = inject(DeclarationService);
    private toastService = inject(ToastService);

    history = signal<DeclarationResponse[]>([]);

    error = signal<string>('');
    selectedDeclaration = signal<DeclarationResponse | null>(null);
    declarationEditorJson = signal('');
    saving = signal(false);

    ngOnInit(): void {
        this.declarationService.getHistory().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.history.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load history.');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load history.');
            }
        });
    }

    getStatusBadgeClass(status: string): string {
        switch (status) {
            case 'DRAFT': return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
            case 'SUBMITTED': return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
            case 'UNDER_VERIFICATION': return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
            case 'VERIFIED': return 'bg-gs-dark/10 text-gs-dark border-gs-dark/20';
            case 'REJECTED': return 'bg-red-500/20 text-red-400 border-red-500/30';
            default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
        }
    }

    getStatusDisplay(status: string): string {
        return status.replace(/_/g, ' ');
    }

    selectedRejectionReason = signal<string | null>(null);

    showRejectionReason(reason: string | undefined): void {
        this.selectedRejectionReason.set(reason || 'No reason provided by the agent.');
    }

    closeModal(): void {
        this.selectedRejectionReason.set(null);
    }

    viewDetails(item: DeclarationResponse): void {
        this.selectedDeclaration.set(item);
        this.declarationEditorJson.set(JSON.stringify(this.toEditablePayload(item), null, 2));
    }

    closeDetails(): void {
        this.selectedDeclaration.set(null);
        this.declarationEditorJson.set('');
    }

    canEditSelected(): boolean {
        const selected = this.selectedDeclaration();
        if (!selected) {
            return false;
        }
        return selected.status === 'DRAFT' || selected.status === 'REJECTED';
    }

    isReadOnlySelected(): boolean {
        const selected = this.selectedDeclaration();
        if (!selected) {
            return true;
        }
        return !this.canEditSelected();
    }

    onEditorInput(value: string): void {
        this.declarationEditorJson.set(value);
    }

    saveDetails(): void {
        const selected = this.selectedDeclaration();
        if (!selected || !this.canEditSelected()) {
            return;
        }

        let payload: DeclarationRequest;
        try {
            payload = JSON.parse(this.declarationEditorJson());
        } catch {
            this.toastService.error('Invalid JSON format. Fix the details before saving.');
            return;
        }

        this.saving.set(true);
        this.declarationService.saveDraft(selected.declarationId, payload).subscribe({
            next: (res) => {
                this.saving.set(false);
                if (!res.success || !res.data) {
                    this.toastService.error(res.error || 'Failed to save declaration changes.');
                    return;
                }

                this.selectedDeclaration.set(res.data);
                this.declarationEditorJson.set(JSON.stringify(this.toEditablePayload(res.data), null, 2));
                this.history.update((entries) =>
                    entries.map((entry) => entry.declarationId === res.data!.declarationId ? res.data! : entry)
                );
            },
            error: (err) => {
                this.saving.set(false);
                this.toastService.error(err.error?.error || 'Failed to save declaration changes.');
            }
        });
    }

    private toEditablePayload(item: DeclarationResponse): DeclarationRequest {
        return {
            electricityUnits: item.electricityUnits,
            hasSolar: item.hasSolar,
            solarUnits: item.solarUnits,
            cookingFuelType: item.cookingFuelType,
            lpgCylinders: item.lpgCylinders,
            pngUnits: item.pngUnits,
            biomassKgPerDay: item.biomassKgPerDay,
            numAcUnits: item.numAcUnits,
            acHoursPerDay: item.acHoursPerDay,
            hasGenerator: item.hasGenerator,
            generatorHoursPerMonth: item.generatorHoursPerMonth,
            usesPublicTransport: item.usesPublicTransport,
            publicTransportKm: item.publicTransportKm,
            dietaryPattern: item.dietaryPattern,
            shoppingOrdersPerMonth: item.shoppingOrdersPerMonth,
            hasCommercialVehicles: item.hasCommercialVehicles,
            commercialVehicleKm: item.commercialVehicleKm,
            thirdPartyShipments: item.thirdPartyShipments,
            employeesPrivateVehicle: item.employeesPrivateVehicle,
            employeesPublicTransport: item.employeesPublicTransport,
            generatorLitersPerMonth: item.generatorLitersPerMonth,
            hasBoiler: item.hasBoiler,
            boilerFuelType: item.boilerFuelType,
            boilerCoalKg: item.boilerCoalKg,
            boilerGasScm: item.boilerGasScm,
            paperReamsPerMonth: item.paperReamsPerMonth,
            usesRecycledPaper: item.usesRecycledPaper,
            rawMaterialType: item.rawMaterialType,
            rawMaterialKg: item.rawMaterialKg
        };
    }
}
