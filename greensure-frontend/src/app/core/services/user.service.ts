import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response';
import { UserProfile, DashboardResponse } from '../models/user';

@Injectable({
    providedIn: 'root'
})
export class UserService {

    private httpClient: HttpClient = inject(HttpClient);
    private apiUrl: string = environment.apiUrl;

    // GET /user/profile
    // Fetches the full profile of the logged-in user.
    // Returns UserProfile with personal + conditional fields
    // (household: numberOfMembers, dwellingType / MSME: businessName, etc.)
    getProfile(): Observable<ApiResponse<UserProfile>> {
        return this.httpClient.get<ApiResponse<UserProfile>>(
            `${this.apiUrl}/user/profile`
        );
    }

    // PUT /user/profile
    // Sends updated profile fields. Backend merges changes.
    updateProfile(data: Partial<UserProfile>): Observable<ApiResponse<UserProfile>> {
        return this.httpClient.put<ApiResponse<UserProfile>>(
            `${this.apiUrl}/user/profile`,
            data
        );
    }

    // GET /user/dashboard
    // Returns summary: hasDeclaration, declarationStatus, latestScore,
    // zone, renewalDue, unreadNotifications — everything dashboard needs.
    getDashboard(): Observable<ApiResponse<DashboardResponse>> {
        return this.httpClient.get<ApiResponse<DashboardResponse>>(
            `${this.apiUrl}/user/dashboard`
        );
    }
}
