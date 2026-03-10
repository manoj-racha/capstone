import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response';
import { AgentTaskResponse, AgentPerformanceResponse, VerificationRequest } from '../models/agent';

@Injectable({
    providedIn: 'root'
})
export class AgentService {

    private httpClient: HttpClient = inject(HttpClient);
    private apiUrl: string = environment.apiUrl;

    // GET /agent/dashboard
    // Fetches all tasks assigned to this agent (all statuses).
    // Returns array of AgentTaskResponse — each has user info,
    // declaration info, deadline, isOverdue flag, etc.
    getDashboard(): Observable<ApiResponse<AgentTaskResponse[]>> {
        return this.httpClient.get<ApiResponse<AgentTaskResponse[]>>(
            `${this.apiUrl}/agent/dashboard`
        );
    }

    // GET /agent/assignments?status=ASSIGNED
    // Fetches assignments filtered by status.
    // status is optional — if omitted, returns all assignments.
    getAssignments(status?: string): Observable<ApiResponse<AgentTaskResponse[]>> {
        let url = `${this.apiUrl}/agent/assignments`;
        if (status) {
            url += `?status=${status}`;
        }
        return this.httpClient.get<ApiResponse<AgentTaskResponse[]>>(
            url
        );
    }

    // GET /agent/assignment/{id}
    // Fetches a single assignment by its ID.
    // Used by task-detail page to show user info and declaration details.
    getAssignment(id: number): Observable<ApiResponse<AgentTaskResponse>> {
        return this.httpClient.get<ApiResponse<AgentTaskResponse>>(
            `${this.apiUrl}/agent/assignment/${id}`
        );
    }

    // GET /agent/assignment/{id}/declaration
    // Fetches the specific declaration attached to this assignment.
    // Includes all fields and vehicle details for the verify page.
    getDeclarationForAssignment(id: number): Observable<ApiResponse<any>> {
        return this.httpClient.get<ApiResponse<any>>(
            `${this.apiUrl}/agent/assignment/${id}/declaration`
        );
    }

    // PUT /agent/assignment/{id}/start
    // Marks assignment status: ASSIGNED → IN_PROGRESS.
    // Agent clicks this when they begin the field visit.
    startAssignment(id: number): Observable<ApiResponse<AgentTaskResponse>> {
        return this.httpClient.put<ApiResponse<AgentTaskResponse>>(
            `${this.apiUrl}/agent/assignment/${id}/start`,
            {}
        );
    }

    // POST /agent/verification/{assignmentId}/submit
    // Submits the verification form with corrected values.
    // body includes: corrected fields (null = confirmed),
    //   overallAction (CONFIRMED/MODIFIED/REJECTED),
    //   agentRemarks (required if MODIFIED/REJECTED),
    //   agentGpsLat, agentGpsLng (auto-captured by browser).
    // After this, backend triggers carbon score calculation.
    submitVerification(assignmentId: number, body: VerificationRequest): Observable<ApiResponse<void>> {
        return this.httpClient.post<ApiResponse<void>>(
            `${this.apiUrl}/agent/verification/${assignmentId}/submit`,
            body
        );
    }

    // GET /agent/performance
    // Fetches performance stats for the logged-in agent.
    // Returns: totalAssignments, completedAssignments, strikeCount,
    //   verification breakdown (confirmed/modified/rejected counts),
    //   rates (completion, modification, confirmation percentages).
    getPerformance(): Observable<ApiResponse<AgentPerformanceResponse>> {
        return this.httpClient.get<ApiResponse<AgentPerformanceResponse>>(
            `${this.apiUrl}/agent/performance`
        );
    }
}
