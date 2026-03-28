import { Component, inject, signal, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { AdminService } from '../../../../features/admin/services/admin.service';
import { AdminAnalytics } from '../../../../core/models/admin';

@Component({
  selector: 'app-admin-analytics',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './admin-analytics.component.html'
})
export class AdminAnalyticsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly router = inject(Router);

  loading = signal(true);
  analytics = signal<AdminAnalytics | null>(null);

  ngOnInit(): void {
    this.loadAnalytics();
  }

  loadAnalytics(): void {
    this.loading.set(true);
    this.adminService.getAnalytics().subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) this.analytics.set(res.data);
      },
      error: () => { this.loading.set(false); }
    });
  }

  getStatusEntries(): { key: string; value: number }[] {
    const data = this.analytics()?.declarationsByStatus;
    return data ? Object.entries(data).map(([key, value]) => ({ key, value: value as number })) : [];
  }

  getZoneEntries(): { key: string; value: number }[] {
    const data = this.analytics()?.zoneDistribution;
    return data ? Object.entries(data).map(([key, value]) => ({ key, value: value as number })) : [];
  }

  getFraudEntries(): { key: string; value: number }[] {
    const data = this.analytics()?.fraudRiskBreakdown;
    return data ? Object.entries(data).map(([key, value]) => ({ key, value: value as number })) : [];
  }

  getZoneColor(zone: string): string {
    if (zone.includes('GREEN') || zone.includes('CHAMPION')) return 'bg-green-500';
    if (zone.includes('IMPROVER')) return 'bg-amber-500';
    return 'bg-red-500';
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'VERIFIED': return 'bg-green-500';
      case 'SUBMITTED': case 'UNDER_VERIFICATION': return 'bg-blue-500';
      case 'REJECTED': return 'bg-red-500';
      default: return 'bg-gray-400';
    }
  }
}
