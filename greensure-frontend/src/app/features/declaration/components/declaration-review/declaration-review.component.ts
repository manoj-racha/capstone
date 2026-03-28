import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DeclarationService } from '../../../../core/services/declaration.service';
import { ToastService } from '../../../../core/services/toast.service';
import { DeclarationProgressComponent } from '../../../../shared/components/declaration-progress/declaration-progress.component';
import { DeclarationDetail, MILEAGE_BAND_LABELS, MileageBand } from '../../../../core/models/declaration';
import { ZONE_LABELS, Zone } from '../../../../core/models/score';

@Component({
  selector: 'app-declaration-review',
  imports: [RouterLink, DeclarationProgressComponent],
  templateUrl: './declaration-review.component.html'
})
export class DeclarationReviewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly declarationService = inject(DeclarationService);
  private readonly toast = inject(ToastService);

  loading = signal(true);
  submitting = signal(false);
  declaration = signal<DeclarationDetail | null>(null);
  declarationId = 0;

  readonly zoneLabels = ZONE_LABELS;
  readonly mileageBandLabels = MILEAGE_BAND_LABELS;

  ngOnInit(): void {
    this.declarationId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadDeclaration();
  }

  loadDeclaration(): void {
    this.loading.set(true);
    this.declarationService.getDeclarationById(this.declarationId).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.declaration.set(res.data);
        }
      },
      error: () => { this.loading.set(false); }
    });
  }

  getMileageLabel(band: MileageBand | undefined): string {
    return band ? this.mileageBandLabels[band] : 'N/A';
  }

  getZoneLabel(zone: Zone | undefined): string {
    return zone ? this.zoneLabels[zone] : 'N/A';
  }

  billConfidenceLabel(score: number | null | undefined): { text: string; barClass: string } {
    if (score == null || Number.isNaN(score)) {
      return { text: 'Confidence unknown', barClass: 'bg-gray-300' };
    }
    if (score > 0.8) {
      return { text: 'High confidence', barClass: 'bg-green-500' };
    }
    if (score >= 0.5) {
      return { text: 'Medium confidence', barClass: 'bg-amber-500' };
    }
    return { text: 'Low confidence — agent will verify', barClass: 'bg-red-500' };
  }

  electricityAnomalyCount(d: { electricityBills?: { aiAnomalyFlag?: boolean | null }[] }): number {
    if (!d.electricityBills?.length) return 0;
    return d.electricityBills.filter((b) => b.aiAnomalyFlag === true).length;
  }

  onSubmit(): void {
    this.submitting.set(true);
    this.declarationService.submitDeclaration(this.declarationId).subscribe({
      next: (res) => {
        this.submitting.set(false);
        if (res.success) {
          this.toast.success('Declaration submitted! An agent will visit within 72 hours.');
          this.router.navigate(['/user/dashboard']);
        }
      },
      error: () => { this.submitting.set(false); }
    });
  }
}
