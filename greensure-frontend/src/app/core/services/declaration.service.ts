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
    return this.http.get<ApiResponse<DeclarationDetail>>(`${this.base}/${id}`);
  }

  /** GET /declaration/history — all past declarations */
  getHistory(): Observable<ApiResponse<DeclarationSummary[]>> {
    return this.http.get<ApiResponse<DeclarationSummary[]>>(`${this.base}/history`);
  }
}
