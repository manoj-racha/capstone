import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response';
import { AgentTaskSummary, AgentWorkspace, AgentPerformance } from '../../../core/models/agent';

@Injectable({
    providedIn: 'root'
})
export class AgentService {

    private httpClient: HttpClient = inject(HttpClient);
    private apiUrl: string = environment.apiUrl;

    getDashboard(): Observable<ApiResponse<AgentTaskSummary[]>> {
        return this.httpClient.get<ApiResponse<AgentTaskSummary[]>>(
            `${this.apiUrl}/agent/dashboard`
        );
    }

    getAssignments(status?: string): Observable<ApiResponse<AgentTaskSummary[]>> {
        let url = `${this.apiUrl}/agent/assignments`;
        if (status) {
            url += `?status=${status}`;
        }
        return this.httpClient.get<ApiResponse<AgentTaskSummary[]>>(url);
    }

    getAssignment(id: number): Observable<ApiResponse<AgentTaskSummary>> {
        return this.httpClient.get<ApiResponse<AgentTaskSummary>>(
            `${this.apiUrl}/agent/assignment/${id}`
        );
    }

    getDeclarationForAssignment(id: number): Observable<ApiResponse<AgentWorkspace>> {
        return this.httpClient.get<ApiResponse<AgentWorkspace>>(
            `${this.apiUrl}/agent/assignment/${id}/declaration`
        );
    }

    startAssignment(id: number): Observable<ApiResponse<AgentTaskSummary>> {
        return this.httpClient.put<ApiResponse<AgentTaskSummary>>(
            `${this.apiUrl}/agent/assignment/${id}/start`,
            {}
        );
    }

    submitVerification(assignmentId: number, body: any): Observable<ApiResponse<void>> {
        return this.httpClient.post<ApiResponse<void>>(
            `${this.apiUrl}/agent/verification/${assignmentId}/submit`,
            body
        );
    }

    getPerformance(): Observable<ApiResponse<AgentPerformance>> {
        return this.httpClient.get<ApiResponse<AgentPerformance>>(
            `${this.apiUrl}/agent/performance`
        );
    }
}
