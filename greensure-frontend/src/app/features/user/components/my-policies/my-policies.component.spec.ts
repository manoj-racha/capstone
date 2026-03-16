import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { MyPoliciesComponent } from './my-policies.component';
import { PolicyService } from '../../../../core/services/policy.service';

describe('MyPoliciesComponent', () => {
  let component: MyPoliciesComponent;
  let fixture: ComponentFixture<MyPoliciesComponent>;

  const mockPolicyService = {
    getMyPolicies: vi.fn()
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyPoliciesComponent],
      providers: [{ provide: PolicyService, useValue: mockPolicyService }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(MyPoliciesComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    mockPolicyService.getMyPolicies.mockReset();
  });

  it('should create', () => {
    mockPolicyService.getMyPolicies.mockReturnValue(of({ success: true, data: [] }));

    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('should load policies on init', () => {
    const mockPolicies = [
      {
        id: 1,
        userId: 1,
        planId: 10,
        policyType: 'HOME',
        policyName: 'Home Shield',
        planName: 'Gold',
        coverageAmount: 500000,
        durationMonths: 12,
        finalPrice: 4999,
        purchasedAt: '2026-01-01T00:00:00.000Z'
      }
    ];
    mockPolicyService.getMyPolicies.mockReturnValue(of({ success: true, data: mockPolicies }));

    fixture.detectChanges();

    expect(mockPolicyService.getMyPolicies).toHaveBeenCalled();
    expect(component.policies().length).toBe(1);
    expect(component.error()).toBe('');
    expect(component.loading()).toBe(false);
  });

  it('should set error when API returns unsuccessful response', () => {
    mockPolicyService.getMyPolicies.mockReturnValue(of({ success: false, error: 'Failed to load' }));

    component.loadMyPolicies();

    expect(component.error()).toBe('Failed to load');
    expect(component.loading()).toBe(false);
  });

  it('should set fallback error on API failure', () => {
    mockPolicyService.getMyPolicies.mockReturnValue(
      throwError(() => ({ error: { error: 'Server unavailable' } }))
    );

    component.loadMyPolicies();

    expect(component.error()).toBe('Server unavailable');
    expect(component.loading()).toBe(false);
  });

  it('should format currency and coverage values', () => {
    expect(component.formatCurrency(150000)).toBe('₹1,50,000');
    expect(component.formatCoverage(2500000)).toBe('₹25 lakhs');
    expect(component.formatCoverage(20000000)).toBe('₹2 crore');
  });

  it('should identify expiring policies', () => {
    vi.spyOn(component, 'getDaysRemaining').mockReturnValue(10);
    expect(component.isExpiringSoon('2026-01-01T00:00:00.000Z', 12)).toBe(true);

    vi.spyOn(component, 'getDaysRemaining').mockReturnValue(0);
    expect(component.isExpiringSoon('2026-01-01T00:00:00.000Z', 12)).toBe(false);
  });
});
