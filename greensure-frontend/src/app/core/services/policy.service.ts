import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../models/api-response';

export interface PolicyPlan {
    planId: number;
    planName: string;
    coverageAmount: number;
    basePremiumYearly: number;
    features: string[];
}

export interface Policy {
    policyType: string;
    name: string;
    icon: string;
    description: string;
    eligibility?: string;
    plans: PolicyPlan[];
}

export interface UserPolicy {
    id: number;
    userId: number;
    planId: number;
    policyType: string;
    policyName: string;
    planName: string;
    coverageAmount: number;
    durationMonths: number;
    finalPrice: number;
    purchasedAt: string;
}

@Injectable({
    providedIn: 'root'
})
export class PolicyService {
    private http = inject(HttpClient);
    private apiUrl = 'http://localhost:9090/api/policies';

    getPolicies(): Observable<ApiResponse<Policy[]>> {
        return this.http.get<ApiResponse<Policy[]>>(this.apiUrl);
    }

    buyPolicy(request: { planId: number; durationMonths: number; finalPrice: number }): Observable<ApiResponse<UserPolicy>> {
        return this.http.post<ApiResponse<UserPolicy>>(`${this.apiUrl}/buy`, request);
    }

    getMyPolicies(): Observable<ApiResponse<UserPolicy[]>> {
        return this.http.get<ApiResponse<UserPolicy[]>>(`${this.apiUrl}/my-policies`);
    }
}
