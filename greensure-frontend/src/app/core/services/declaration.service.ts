import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response';
import {
  DeclarationSummary,
  DeclarationDetail,
  HouseholdDataRequest,
  AddVehicleRequest,
  UploadVehicleDocumentRequest,
  VehicleData,
  VehicleDocument,
  ElectricityDataRequest,
  SolarDataRequest,
  CookingDataRequest,
  LifestyleDataRequest,
} from '../models/declaration';

@Injectable({ providedIn: 'root' })
export class DeclarationService {

  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/declaration`;

  /** Tracks the current active declaration being filled */
  readonly activeDeclarationId = signal<number | null>(null);

  /** POST /declaration/start — creates a new declaration for the current year */
  startDeclaration(): Observable<ApiResponse<number>> {
    return this.http.post<ApiResponse<number>>(`${this.base}/start`, {}).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.activeDeclarationId.set(res.data);
        }
      })
    );
  }

  /** PUT /declaration/{id}/household */
  saveHousehold(id: number, data: HouseholdDataRequest): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/${id}/household`, data);
  }

  /** POST /declaration/{id}/vehicles */
  addVehicle(id: number, data: AddVehicleRequest): Observable<ApiResponse<VehicleData>> {
    return this.http.post<ApiResponse<VehicleData>>(`${this.base}/${id}/vehicles`, data);
  }

  /** POST /declaration/{id}/vehicles/{vehicleId}/documents */
  addVehicleDocument(id: number, vehicleId: number, data: UploadVehicleDocumentRequest): Observable<ApiResponse<VehicleDocument>> {
    return this.http.post<ApiResponse<VehicleDocument>>(`${this.base}/${id}/vehicles/${vehicleId}/documents`, data);
  }

  /** DELETE /declaration/{id}/vehicles/{vehicleId} */
  removeVehicle(id: number, vehicleId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}/vehicles/${vehicleId}`);
  }

  /** GET /declaration/{id}/vehicles */
  getVehicles(id: number): Observable<ApiResponse<VehicleData[]>> {
    return this.http.get<ApiResponse<VehicleData[]>>(`${this.base}/${id}/vehicles`);
  }

  /** PUT /declaration/{id}/electricity */
  saveElectricity(id: number, data: ElectricityDataRequest): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/${id}/electricity`, data);
  }

  /** POST /declaration/{id}/electricity/bills */
  addElectricityBill(id: number, billingMonth: string, unitsKwh: number, amount?: number, billUrl?: string, confidence: number = 1.0): Observable<ApiResponse<void>> {
    let params = new HttpParams()
      .set('billingMonth', billingMonth)
      .set('unitsKwh', unitsKwh.toString())
      .set('confidence', confidence.toString());
    
    if (amount !== undefined && amount !== null) params = params.set('amount', amount.toString());
    if (billUrl) params = params.set('billUrl', billUrl);

    return this.http.post<ApiResponse<void>>(`${this.base}/${id}/electricity/bills`, {}, { params });
  }

  /** PUT /declaration/{id}/solar */
  saveSolar(id: number, data: SolarDataRequest): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/${id}/solar`, data);
  }

  /** PUT /declaration/{id}/cooking */
  saveCooking(id: number, data: CookingDataRequest): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/${id}/cooking`, data);
  }

  /** PUT /declaration/{id}/lifestyle */
  saveLifestyle(id: number, data: LifestyleDataRequest): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/${id}/lifestyle`, data);
  }

  /** POST /declaration/{id}/submit */
  submitDeclaration(id: number): Observable<ApiResponse<DeclarationDetail>> {
    return this.http.post<ApiResponse<DeclarationDetail>>(`${this.base}/${id}/submit`, {});
  }

  /** POST /declaration/{id}/resubmit */
  resubmitDeclaration(id: number): Observable<ApiResponse<DeclarationDetail>> {
    return this.http.post<ApiResponse<DeclarationDetail>>(`${this.base}/${id}/resubmit`, {});
  }

  /** GET /declaration/my — current user's declarations */
  getMyDeclarations(): Observable<ApiResponse<DeclarationSummary[]>> {
    return this.http.get<ApiResponse<DeclarationSummary[]>>(`${this.base}/my`);
  }

  /** GET /declaration/{id} — full declaration detail */
  getDeclarationById(id: number): Observable<ApiResponse<DeclarationDetail>> {
    return this.http.get<ApiResponse<any>>(`${this.base}/${id}`).pipe(
      map((res) => {
        if (!res?.data) {
          return res as ApiResponse<DeclarationDetail>;
        }

        const raw = res.data;
        const normalized: DeclarationDetail = {
          ...raw,
          householdSize: raw.householdSize ?? raw.householdMembers ?? 0,
          electricityBills: Array.isArray(raw.electricityBills) ? raw.electricityBills : [],
          electricityData: raw.electricityData ?? (
            raw.provider
              ? {
                  provider: raw.provider,
                  consumerNumber: raw.consumerNumber ?? '',
                  userDeclaredMonthlyKwh: raw.userDeclaredMonthlyKwh ?? 0,
                  ocrComputedMonthlyKwh: raw.ocrComputedMonthlyKwh ?? null,
                  billsUploaded: raw.billsUploaded ?? 0,
                  agentCorrectedMonthlyKwh: raw.agentCorrectedMonthlyKwh ?? null,
                  billUrls: raw.electricityBillUrls ?? []
                }
              : null
          ),
          cookingData: raw.cookingData ?? (
            raw.cookingFuelType
              ? {
                  fuelType: raw.cookingFuelType,
                  pngConsumerNumber: raw.pngConsumerNumber ?? null,
                  userDeclaredCylinders: raw.userDeclaredCylinders ?? raw.cylinders ?? null,
                  ocrComputedCylinders: raw.ocrComputedCylinders ?? null,
                  agentCorrectedFuelType: raw.agentCorrectedFuelType ?? null,
                  agentCorrectedCylinders: raw.agentCorrectedCylinders ?? null,
                  billUrls: raw.billUrls ? this.safeParseBillUrls(raw.billUrls) : []
                }
              : null
          ),
          solarData: raw.solarData ?? (
            raw.hasSolar !== undefined
              ? {
                  hasSolar: !!raw.hasSolar,
                  capacityKw: raw.solarCapacityKw ?? null,
                  certificateUrl: raw.certificateUrl ?? null,
                  mnreVerified: !!raw.mnreVerified,
                  agentCorrectedCapacityKw: raw.agentCorrectedCapacityKw ?? null,
                  agentVerifiedSolar: !!raw.agentVerifiedSolar
                }
              : null
          ),
          lifestyleData: raw.lifestyleData ?? (
            raw.publicTransportUsage
              ? {
                  publicTransportUsage: raw.publicTransportUsage,
                  wastesRecycling: !!raw.wastesRecycling
                }
              : null
          )
        };

        return {
          ...res,
          data: normalized
        } as ApiResponse<DeclarationDetail>;
      })
    );
  }

  /** GET /declaration/history — all past declarations */
  getHistory(): Observable<ApiResponse<DeclarationSummary[]>> {
    return this.http.get<ApiResponse<DeclarationSummary[]>>(`${this.base}/history`);
  }

  private safeParseBillUrls(value: unknown): string[] {
    if (Array.isArray(value)) {
      return value.filter((u): u is string => typeof u === 'string' && u.trim().length > 0);
    }
    if (typeof value !== 'string' || value.trim().length === 0) {
      return [];
    }
    try {
      const parsed = JSON.parse(value);
      return Array.isArray(parsed)
        ? parsed.filter((u): u is string => typeof u === 'string' && u.trim().length > 0)
        : [];
    } catch {
      return [];
    }
  }
}
