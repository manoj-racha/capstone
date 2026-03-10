import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response';
import { DeclarationResponse, DeclarationRequest, VehicleResponse, VehicleRequest } from '../models/declaration';

@Injectable({
    providedIn: 'root'
})
export class DeclarationService {

    private httpClient: HttpClient = inject(HttpClient);
    private apiUrl: string = environment.apiUrl;

    // POST /declaration/start
    // Creates a new DRAFT declaration for the current year.
    // Backend enforces one declaration per year per user.
    // Returns the new DeclarationResponse with declarationId.
    // After this, navigate to the fill form using that id.
    startDeclaration(): Observable<ApiResponse<DeclarationResponse>> {
        return this.httpClient.post<ApiResponse<DeclarationResponse>>(
            `${this.apiUrl}/declaration/start`,
            {}
        );
    }

    // PUT /declaration/{id}/save
    // Saves the declaration form as a draft (auto-save or manual save).
    // All fields are optional — only changed fields need to be sent.
    // Does NOT submit — user can keep editing after saving.
    saveDraft(id: number, data: DeclarationRequest): Observable<ApiResponse<DeclarationResponse>> {
        return this.httpClient.put<ApiResponse<DeclarationResponse>>(
            `${this.apiUrl}/declaration/${id}/save`,
            data
        );
    }

    // PUT /declaration/{id}/submit
    // Submits the declaration for verification.
    // Changes status from DRAFT → SUBMITTED.
    // Backend auto-assigns an agent within 30 minutes via scheduler.
    submitDeclaration(id: number): Observable<ApiResponse<DeclarationResponse>> {
        return this.httpClient.put<ApiResponse<DeclarationResponse>>(
            `${this.apiUrl}/declaration/${id}/submit`,
            {}
        );
    }

    // POST /declaration/{id}/vehicle
    // Adds a vehicle entry to the declaration.
    // Each vehicle has: vehicleType, fuelType, kmPerMonth, quantity.
    addVehicle(id: number, vehicle: VehicleRequest): Observable<ApiResponse<VehicleResponse>> {
        return this.httpClient.post<ApiResponse<VehicleResponse>>(
            `${this.apiUrl}/declaration/${id}/vehicle`,
            vehicle
        );
    }

    // DELETE /declaration/{id}/vehicle/{vehicleId}
    // Removes a specific vehicle from the declaration.
    removeVehicle(id: number, vehicleId: number): Observable<ApiResponse<void>> {
        return this.httpClient.delete<ApiResponse<void>>(
            `${this.apiUrl}/declaration/${id}/vehicle/${vehicleId}`
        );
    }

    // GET /declaration/{id}
    // Fetches a single declaration with all fields + vehicles.
    // Used by the review page and fill form (to resume editing).
    getDeclaration(id: number): Observable<ApiResponse<DeclarationResponse>> {
        return this.httpClient.get<ApiResponse<DeclarationResponse>>(
            `${this.apiUrl}/declaration/${id}`
        );
    }

    // GET /declaration/history
    // Fetches all past declarations for the logged-in user.
    // Returns array sorted by year descending.
    getHistory(): Observable<ApiResponse<DeclarationResponse[]>> {
        return this.httpClient.get<ApiResponse<DeclarationResponse[]>>(
            `${this.apiUrl}/declaration/history`
        );
    }
}
