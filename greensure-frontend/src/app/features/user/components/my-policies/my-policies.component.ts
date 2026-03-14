import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PolicyService, UserPolicy } from '../../../../core/services/policy.service';

@Component({
  selector: 'app-my-policies',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-policies.component.html'
})
export class MyPoliciesComponent implements OnInit {
  private policyService = inject(PolicyService);

  policies = signal<UserPolicy[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit(): void {
    this.loadMyPolicies();
  }

  loadMyPolicies(): void {
    this.loading.set(true);
    this.policyService.getMyPolicies().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.policies.set(res.data);
        } else {
          this.error.set(res.error || 'Failed to load your policies');
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.error || 'An error occurred while fetching policies');
        this.loading.set(false);
      }
    });
  }

  getDaysRemaining(purchasedAt: string, durationMonths: number): number {
    const purchaseDate = new Date(purchasedAt);
    const expiryDate = new Date(purchaseDate);
    expiryDate.setMonth(expiryDate.getMonth() + durationMonths);
    
    const now = new Date();
    const diff = expiryDate.getTime() - now.getTime();
    return Math.max(0, Math.ceil(diff / (1000 * 60 * 60 * 24)));
  }

  isExpiringSoon(purchasedAt: string, durationMonths: number): boolean {
    const days = this.getDaysRemaining(purchasedAt, durationMonths);
    return days > 0 && days < 30;
  }

  // ── Helpers ──────────────────────────────────────────────
  formatCurrency(amount: number): string {
    return '₹' + (amount || 0).toLocaleString('en-IN');
  }

  formatCoverage(amount: number): string {
    if (amount >= 10000000) return '₹' + (amount / 10000000) + ' crore';
    if (amount >= 100000) return '₹' + (amount / 100000) + ' lakhs';
    return '₹' + (amount || 0).toLocaleString('en-IN');
  }
}
