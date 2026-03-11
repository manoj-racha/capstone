import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse, Page } from '../models/api-response';
import { AdminOverview, CreateAgentRequest } from '../models/admin';
import { UserProfile } from '../models/user';
import { AgentPerformanceResponse, AgentTaskResponse } from '../models/agent';
import { DeclarationResponse } from '../models/declaration';

@Injectable({
    providedIn: 'root'
})
export class AdminService {

    private httpClient: HttpClient = inject(HttpClient);
    private apiUrl: string = environment.apiUrl;

    // ═══════════════════════════════════════════════════════════
    // USER MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    // GET /admin/users?userType=X&status=Y&page=0&size=10
    // Fetches all users, optionally filtered by userType and status, paginated.
    getUsers(userType?: string, status?: string, page: number = 0, size: number = 10): Observable<ApiResponse<Page<UserProfile>>> {
        let url = `${this.apiUrl}/admin/users`;
        const params: string[] = [];
        if (userType) params.push(`userType=${userType}`);
        if (status) params.push(`status=${status}`);
        params.push(`page=${page}`);
        params.push(`size=${size}`);

        if (params.length > 0) url += '?' + params.join('&');

        return this.httpClient.get<ApiResponse<Page<UserProfile>>>(url);
    }

    // GET /admin/users/{id}
    // Fetches a single user by ID — full profile for detail page.
    getUserById(id: number): Observable<ApiResponse<UserProfile>> {
        return this.httpClient.get<ApiResponse<UserProfile>>(
            `${this.apiUrl}/admin/users/${id}`
        );
    }

    // PUT /admin/users/{id}/status?status=SUSPENDED
    // Changes user status: ACTIVE ↔ SUSPENDED.
    updateUserStatus(id: number, status: string): Observable<ApiResponse<void>> {
        return this.httpClient.put<ApiResponse<void>>(
            `${this.apiUrl}/admin/users/${id}/status?status=${status}`,
            {}
        );
    }

    // PUT /admin/users/{id}/unlock-resubmission
    // Resets resubmission count so user can submit again after 3 rejections.
    unlockResubmission(id: number): Observable<ApiResponse<void>> {
        return this.httpClient.put<ApiResponse<void>>(
            `${this.apiUrl}/admin/users/${id}/unlock-resubmission`,
            {}
        );
    }

    // ═══════════════════════════════════════════════════════════
    // AGENT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    // POST /admin/agents/create
    // Creates a new agent account with employeeId and assigned zones.
    createAgent(data: CreateAgentRequest): Observable<ApiResponse<any>> {
        return this.httpClient.post<ApiResponse<any>>(
            `${this.apiUrl}/admin/agents/create`,
            data
        );
    }

    // GET /admin/agents?page=0&size=10
    // Fetches all agents — used by agents management table, paginated.
    getAgents(page: number = 0, size: number = 10): Observable<ApiResponse<Page<any>>> {
        return this.httpClient.get<ApiResponse<Page<any>>>(
            `${this.apiUrl}/admin/agents?page=${page}&size=${size}`
        );
    }

    // GET /admin/agents/{id}
    // Fetches full agent details.
    getAgentById(id: number): Observable<ApiResponse<any>> {
        return this.httpClient.get<ApiResponse<any>>(
            `${this.apiUrl}/admin/agents/${id}`
        );
    }

    // PUT /admin/agents/{id}/status?status=ACTIVE
    // Changes agent status: ACTIVE ↔ SUSPENDED.
    updateAgentStatus(id: number, status: string): Observable<ApiResponse<void>> {
        return this.httpClient.put<ApiResponse<void>>(
            `${this.apiUrl}/admin/agents/${id}/status?status=${status}`,
            {}
        );
    }

    // PUT /admin/agents/{id}/clear-strikes
    // Resets agent's strike count to 0.
    clearStrikes(id: number): Observable<ApiResponse<void>> {
        return this.httpClient.put<ApiResponse<void>>(
            `${this.apiUrl}/admin/agents/${id}/clear-strikes`,
            {}
        );
    }

    // ═══════════════════════════════════════════════════════════
    // DECLARATION MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    // GET /admin/declarations?status=SUBMITTED&page=0&size=10
    // Fetches all declarations, optionally filtered by status, paginated.
    getDeclarations(status?: string, page: number = 0, size: number = 10): Observable<ApiResponse<Page<DeclarationResponse>>> {
        let url = `${this.apiUrl}/admin/declarations`;
        const params: string[] = [];
        if (status) params.push(`status=${status}`);
        params.push(`page=${page}`);
        params.push(`size=${size}`);

        url += '?' + params.join('&');

        return this.httpClient.get<ApiResponse<Page<DeclarationResponse>>>(url);
    }

    // GET /admin/declarations/{id}
    // Fetches a specific declaration by ID for the admin detailed view.
    getDeclarationById(id: number): Observable<ApiResponse<DeclarationResponse>> {
        return this.httpClient.get<ApiResponse<DeclarationResponse>>(
            `${this.apiUrl}/admin/declarations/${id}`
        );
    }

    // PUT /admin/declarations/{id}/unlock
    // Unlocks a rejected declaration so user can edit and resubmit.
    unlockDeclaration(id: number): Observable<ApiResponse<void>> {
        return this.httpClient.put<ApiResponse<void>>(
            `${this.apiUrl}/admin/declarations/${id}/unlock`,
            {}
        );
    }

    // ═══════════════════════════════════════════════════════════
    // ASSIGNMENT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    // GET /admin/assignments?status=ASSIGNED&page=0&size=10
    // Fetches all agent assignments, optionally filtered by status, paginated.
    getAssignments(status?: string, page: number = 0, size: number = 10): Observable<ApiResponse<Page<AgentTaskResponse>>> {
        let url = `${this.apiUrl}/admin/assignments`;
        const params: string[] = [];
        if (status) params.push(`status=${status}`);
        params.push(`page=${page}`);
        params.push(`size=${size}`);

        url += '?' + params.join('&');

        return this.httpClient.get<ApiResponse<Page<AgentTaskResponse>>>(url);
    }

    // POST /admin/assignments/reassign/{id}?newAgentId=X
    // Reassigns an assignment to a different agent.
    // Used when original agent is unavailable or flagged.
    reassignTask(id: number, newAgentId: number): Observable<ApiResponse<void>> {
        return this.httpClient.post<ApiResponse<void>>(
            `${this.apiUrl}/admin/assignments/reassign/${id}?newAgentId=${newAgentId}`,
            {}
        );
    }

    // ═══════════════════════════════════════════════════════════
    // REPORTS
    // ═══════════════════════════════════════════════════════════

    // GET /admin/reports/overview
    // Returns AdminOverview: totalUsers, totalAgents, totalDeclarations,
    //   pendingVerifications, totalScoresGenerated, flaggedAgents.
    getOverview(): Observable<ApiResponse<AdminOverview>> {
        return this.httpClient.get<ApiResponse<AdminOverview>>(
            `${this.apiUrl}/admin/reports/overview`
        );
    }

    // GET /admin/reports/performance
    // Returns AgentPerformanceResponse for all active agents.
    getAgentPerformanceReport(): Observable<ApiResponse<AgentPerformanceResponse>> {
        return this.httpClient.get<ApiResponse<AgentPerformanceResponse>>(
            `${this.apiUrl}/admin/reports/performance`
        );
    }

    // GET /admin/reports/carbon-heatmap
    // Returns carbon score data aggregated by zone for heatmap display.
    getCarbonHeatmap(): Observable<ApiResponse<any[]>> {
        return this.httpClient.get<ApiResponse<any[]>>(
            `${this.apiUrl}/admin/reports/carbon-heatmap`
        );
    }
}
