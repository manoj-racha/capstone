import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ScoreService } from '../../../../core/services/score.service';
import { CarbonScoreResponse } from '../../../../core/models/score';

interface PolicyPlan {
    planId: number;
    planName: string;
    coverageAmount: number;
    basePremiumYearly: number;
    features: string[];
}

interface PolicyType {
    policyType: string;
    name: string;
    icon: string;
    description: string;
    eligibility?: string;
    plans: PolicyPlan[];
}

interface DiscountResult {
    zoneDiscount: number;
    improvementBonus: number;
    totalDiscount: number;
}

interface PriceResult {
    baseForDuration: number;
    carbonSaving: number;
    durationSaving: number;
    durationDiscountPct: number;
    finalPrice: number;
}

@Component({
    selector: 'app-policies',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './policies.component.html'
})
export class PoliciesComponent implements OnInit {
    private scoreService = inject(ScoreService);

    // ── Score state ──────────────────────────────────────────
    score = signal<CarbonScoreResponse | null>(null);
    scoreLoaded = signal(false);

    // ── Selection state ──────────────────────────────────────
    selectedPolicyIndex = signal<number | null>(null);
    selectedPlanIndex = signal<number | null>(null);
    durationMonths = signal<number>(12);
    showConfirmModal = signal(false);
    policyConfirmed = signal(false);

    // ── User type ────────────────────────────────────────────
    userType = signal(localStorage.getItem('userType') || 'HOUSEHOLD');

    // ── Static policy data ───────────────────────────────────
    policies: PolicyType[] = [
        {
            policyType: 'HOME_SHIELD',
            name: 'Home Shield',
            icon: '🏠',
            description: 'Complete protection for your home or business premises',
            plans: [
                {
                    planId: 1, planName: 'Basic', coverageAmount: 500000, basePremiumYearly: 4999,
                    features: ['Fire and natural disaster coverage', 'Burglary protection', '24x7 claim support']
                },
                {
                    planId: 2, planName: 'Standard', coverageAmount: 1500000, basePremiumYearly: 9999,
                    features: ['Everything in Basic', 'Flood and earthquake coverage', 'Temporary accommodation benefit', 'Personal belongings cover']
                },
                {
                    planId: 3, planName: 'Premium', coverageAmount: 5000000, basePremiumYearly: 19999,
                    features: ['Everything in Standard', 'Full reconstruction coverage', 'Loss of rent benefit', 'Dedicated claim manager']
                }
            ]
        },
        {
            policyType: 'VEHICLE_GUARD',
            name: 'Vehicle Guard',
            icon: '🚗',
            description: 'Comprehensive coverage for all your declared vehicles',
            plans: [
                {
                    planId: 4, planName: 'Basic', coverageAmount: 500000, basePremiumYearly: 2999,
                    features: ['Third party liability', 'Theft protection', 'Basic own damage']
                },
                {
                    planId: 5, planName: 'Standard', coverageAmount: 1500000, basePremiumYearly: 5999,
                    features: ['Everything in Basic', 'Full own damage coverage', 'Zero depreciation', 'Roadside assistance']
                },
                {
                    planId: 6, planName: 'Premium', coverageAmount: 2500000, basePremiumYearly: 9999,
                    features: ['Everything in Standard', 'Engine protection', 'Key replacement', 'Return to invoice cover']
                }
            ]
        },
        {
            policyType: 'BUSINESS_PROTECT',
            name: 'Business Protect',
            icon: '🏭',
            description: 'Protect your MSME operations against unexpected losses',
            eligibility: 'MSME',
            plans: [
                {
                    planId: 7, planName: 'Basic', coverageAmount: 1000000, basePremiumYearly: 9999,
                    features: ['Equipment breakdown cover', 'Fire and theft protection', 'Basic liability coverage']
                },
                {
                    planId: 8, planName: 'Standard', coverageAmount: 5000000, basePremiumYearly: 19999,
                    features: ['Everything in Basic', 'Business interruption cover', 'Employee liability', 'Inventory protection']
                },
                {
                    planId: 9, planName: 'Premium', coverageAmount: 10000000, basePremiumYearly: 34999,
                    features: ['Everything in Standard', 'Directors liability', 'Cyber risk cover', 'Export credit insurance']
                }
            ]
        }
    ];

    // ── Computed: visible policies based on user type ─────────
    visiblePolicies = computed(() => {
        if (this.userType() === 'HOUSEHOLD') {
            return this.policies.filter(p => p.eligibility !== 'MSME');
        }
        return this.policies;
    });

    // ── Computed: discount from score ────────────────────────
    discount = computed<DiscountResult>(() => {
        const s = this.score();
        if (!s) return { zoneDiscount: 0, improvementBonus: 0, totalDiscount: 0 };
        return this.calculateDiscount(s);
    });

    ngOnInit(): void {
        this.scoreService.getMyScore().subscribe({
            next: (res) => {
                this.scoreLoaded.set(true);
                if (res.success && res.data) {
                    this.score.set(res.data);
                }
            },
            error: () => {
                this.scoreLoaded.set(true);
            }
        });
    }

    // ── Discount calculation ─────────────────────────────────
    calculateDiscount(score: CarbonScoreResponse): DiscountResult {
        let zoneDiscount = 0;
        const co2 = score.perCapitaCo2;
        const zone = score.zone;

        if (zone === 'GREEN_CHAMPION') {
            if (co2 < 500) zoneDiscount = 30;
            else if (co2 < 1000) zoneDiscount = 25;
            else zoneDiscount = 20;
        } else if (zone === 'GREEN_IMPROVER') {
            if (co2 < 1800) zoneDiscount = 15;
            else if (co2 < 2100) zoneDiscount = 10;
            else zoneDiscount = 5;
        } else {
            zoneDiscount = 0;
        }

        let improvementBonus = 0;
        if (score.improvementPercentage) {
            const imp = score.improvementPercentage;
            if (imp >= 30) improvementBonus = 12;
            else if (imp >= 20) improvementBonus = 8;
            else if (imp >= 10) improvementBonus = 5;
        }

        const total = Math.min(zoneDiscount + improvementBonus, 35);
        return { zoneDiscount, improvementBonus, totalDiscount: total };
    }

    // ── Price calculation ────────────────────────────────────
    calculateFinalPrice(basePremium: number): PriceResult {
        const dur = this.durationMonths();
        let durationMultiplier = 1;
        let durationDiscount = 0;

        if (dur === 6) { durationMultiplier = 0.5; durationDiscount = 0; }
        else if (dur === 12) { durationMultiplier = 1; durationDiscount = 5; }
        else if (dur === 24) { durationMultiplier = 2; durationDiscount = 10; }

        const baseForDuration = basePremium * durationMultiplier;
        const totalDisc = this.discount().totalDiscount;
        const carbonSaving = baseForDuration * (totalDisc / 100);
        const durationSaving = baseForDuration * (durationDiscount / 100);
        const finalPrice = baseForDuration - carbonSaving - durationSaving;

        return {
            baseForDuration: Math.round(baseForDuration),
            carbonSaving: Math.round(carbonSaving),
            durationSaving: Math.round(durationSaving),
            durationDiscountPct: durationDiscount,
            finalPrice: Math.round(finalPrice)
        };
    }

    // ── Selection handlers ───────────────────────────────────
    selectPolicy(index: number): void {
        this.selectedPolicyIndex.set(index);
        this.selectedPlanIndex.set(null);
    }

    selectPlan(index: number): void {
        this.selectedPlanIndex.set(index);
    }

    setDuration(months: number): void {
        this.durationMonths.set(months);
    }

    openConfirm(): void {
        this.showConfirmModal.set(true);
    }

    closeConfirm(): void {
        this.showConfirmModal.set(false);
    }

    confirmPolicy(): void {
        const pi = this.selectedPolicyIndex()!;
        const pli = this.selectedPlanIndex()!;
        const policy = this.visiblePolicies()[pi];
        const plan = policy.plans[pli];
        const price = this.calculateFinalPrice(plan.basePremiumYearly);

        const selectedPolicy = {
            policyType: policy.policyType,
            policyName: policy.name,
            planName: plan.planName,
            coverageAmount: plan.coverageAmount,
            durationMonths: this.durationMonths(),
            finalPrice: price.finalPrice,
            selectedAt: new Date().toISOString()
        };

        localStorage.setItem('selectedPolicy', JSON.stringify(selectedPolicy));
        this.showConfirmModal.set(false);
        this.policyConfirmed.set(true);
    }

    // ── Helpers ──────────────────────────────────────────────
    formatCurrency(amount: number): string {
        return '₹' + amount.toLocaleString('en-IN');
    }

    formatCoverage(amount: number): string {
        if (amount >= 10000000) return '₹' + (amount / 10000000) + ' crore';
        if (amount >= 100000) return '₹' + (amount / 100000) + ' lakhs';
        return '₹' + amount.toLocaleString('en-IN');
    }
}
