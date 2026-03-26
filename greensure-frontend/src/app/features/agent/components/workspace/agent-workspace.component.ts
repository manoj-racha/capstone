import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AgentService } from '../../../../core/services/agent.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AgentWorkspace, MatchStatus } from '../../../../core/models/agent';

@Component({
  selector: 'app-agent-workspace',
  imports: [ReactiveFormsModule],
  templateUrl: './agent-workspace.component.html'
})
export class AgentWorkspaceComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly agentService = inject(AgentService);
  private readonly toast = inject(ToastService);

  loading = signal(true);
  submitting = signal(false);
  workspace = signal<AgentWorkspace | null>(null);
  actionMode = signal<'confirm' | 'modify' | 'reject' | null>(null);
  assignmentId = 0;

  modifyForm = this.fb.group({
    correctedFuelType: [''],
    correctedMileageBand: [''],
    correctedMonthlyKwh: [null as number | null],
    correctedCookingFuelType: [''],
    correctedAnnualCylinders: [null as number | null],
    correctedSolarCapacityKw: [null as number | null],
    agentVerifiedSolar: [false],
    correctionNotes: ['', [Validators.required]],
  });

  rejectForm = this.fb.group({
    rejectionReason: ['', [Validators.required, Validators.minLength(10)]],
  });

  ngOnInit(): void {
    this.assignmentId = Number(this.route.snapshot.paramMap.get('assignmentId'));
    this.loadWorkspace();
  }

  loadWorkspace(): void {
    this.loading.set(true);
    this.agentService.getWorkspace(this.assignmentId).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) this.workspace.set(res.data);
      },
      error: () => { this.loading.set(false); }
    });
  }

  getStatusClass(status: MatchStatus): string {
    switch (status) {
      case 'MATCH': return 'bg-green-100 text-green-700';
      case 'MISMATCH': return 'bg-red-100 text-red-700';
      case 'UNVERIFIED': return 'bg-yellow-100 text-yellow-700';
      case 'NOT_APPLICABLE': return 'bg-gray-100 text-gray-500';
    }
  }

  getRiskClass(level: string): string {
    switch (level) {
      case 'HIGH': return 'bg-red-500 text-white';
      case 'MEDIUM': return 'bg-amber-500 text-white';
      default: return 'bg-green-500 text-white';
    }
  }

  async onConfirm(): Promise<void> {
    this.submitting.set(true);
    try {
      const pos = await this.agentService.getCurrentPosition();
      this.agentService.confirmVerification(
        this.assignmentId, pos.coords.latitude, pos.coords.longitude
      ).subscribe({
        next: () => {
          this.submitting.set(false);
          this.toast.success('Declaration verified successfully!');
          this.router.navigate(['/agent/dashboard']);
        },
        error: () => { this.submitting.set(false); }
      });
    } catch {
      this.submitting.set(false);
      this.toast.error('GPS required. Please enable location access.');
    }
  }

  async onModify(): Promise<void> {
    if (this.modifyForm.invalid) { this.modifyForm.markAllAsTouched(); return; }
    this.submitting.set(true);
    try {
      const pos = await this.agentService.getCurrentPosition();
      const f = this.modifyForm.controls;
      this.agentService.modifyAndVerify(
        this.assignmentId,
        {
          correctedFuelType: f.correctedFuelType.value || undefined,
          correctedMileageBand: f.correctedMileageBand.value || undefined,
          correctedMonthlyKwh: f.correctedMonthlyKwh.value ?? undefined,
          correctedCookingFuelType: f.correctedCookingFuelType.value || undefined,
          correctedAnnualCylinders: f.correctedAnnualCylinders.value ?? undefined,
          correctedSolarCapacityKw: f.correctedSolarCapacityKw.value ?? undefined,
          agentVerifiedSolar: f.agentVerifiedSolar.value ?? undefined,
          correctionNotes: f.correctionNotes.value ?? '',
        },
        pos.coords.latitude, pos.coords.longitude
      ).subscribe({
        next: () => {
          this.submitting.set(false);
          this.toast.success('Declaration modified and verified!');
          this.router.navigate(['/agent/dashboard']);
        },
        error: () => { this.submitting.set(false); }
      });
    } catch {
      this.submitting.set(false);
      this.toast.error('GPS required. Please enable location access.');
    }
  }

  async onReject(): Promise<void> {
    if (this.rejectForm.invalid) { this.rejectForm.markAllAsTouched(); return; }
    this.submitting.set(true);
    try {
      const pos = await this.agentService.getCurrentPosition();
      this.agentService.rejectDeclaration(
        this.assignmentId,
        { rejectionReason: this.rejectForm.controls.rejectionReason.value ?? '' },
        pos.coords.latitude, pos.coords.longitude
      ).subscribe({
        next: () => {
          this.submitting.set(false);
          this.toast.success('Declaration rejected.');
          this.router.navigate(['/agent/dashboard']);
        },
        error: () => { this.submitting.set(false); }
      });
    } catch {
      this.submitting.set(false);
      this.toast.error('GPS required. Please enable location access.');
    }
  }
}
