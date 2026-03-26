import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, Page } from '../../../core/models/api-response';
import { CreateAgentRequest, AdminAnalytics, ManualAssignRequest, ReassignRequest } from '../../../core/models/admin';
import { UserProfile } from '../../../core/models/user';
import { AgentProfile, AgentTaskSummary } from '../../../core/models/agent';
import { DeclarationSummary, DeclarationDetail } from '../../../core/models/declaration';

@Injectable({ providedIn: 'root' })
export class AdminService {

  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/admin`;

  // ── User Management ─────────────────────────────────────

  getUsers(userType?: string, status?: string, page = 0, size = 10): Observable<ApiResponse<Page<UserProfile>>> {
    let url = `${this.base}/users`;
    const params: string[] = [];
    if (userType) params.push(`userType=${userType}`);
    if (status) params.push(`status=${status}`);
    params.push(`page=${page}`, `size=${size}`);
    if (params.length) url += '?' + params.join('&');
    return this.http.get<ApiResponse<Page<UserProfile>>>(url);
  }

  getUserById(id: number): Observable<ApiResponse<UserProfile>> {
    return this.http.get<ApiResponse<UserProfile>>(`${this.base}/users/${id}`);
  }

  updateUserStatus(id: number, status: string): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/users/${id}/status?status=${status}`, {});
  }

  unlockResubmission(id: number): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/users/${id}/unlock-resubmission`, {});
  }

  // ── Agent Management ────────────────────────────────────

  createAgent(data: CreateAgentRequest): Observable<ApiResponse<AgentProfile>> {
    return this.http.post<ApiResponse<AgentProfile>>(`${this.base}/agents/create`, data);
  }

  getAgents(page = 0, size = 10): Observable<ApiResponse<Page<AgentProfile>>> {
    return this.http.get<ApiResponse<Page<AgentProfile>>>(`${this.base}/agents?page=${page}&size=${size}`);
  }

  getAllAgents(): Observable<ApiResponse<AgentProfile[]>> {
    return this.http.get<ApiResponse<AgentProfile[]>>(`${this.base}/agents`);
  }

  getAgentById(id: number): Observable<ApiResponse<AgentProfile>> {
    return this.http.get<ApiResponse<AgentProfile>>(`${this.base}/agents/${id}`);
  }

  updateAgentStatus(id: number, status: string): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/agents/${id}/status?status=${status}`, {});
  }

  suspendAgent(id: number): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/agents/${id}/suspend`, null);
  }

  activateAgent(id: number): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/agents/${id}/activate`, null);
  }

  clearStrikes(id: number): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/agents/${id}/clear-strikes`, {});
  }

  getAvailableAgents(): Observable<ApiResponse<AgentProfile[]>> {
    return this.http.get<ApiResponse<AgentProfile[]>>(`${this.base}/agents/available`);
  }

  // ── Declaration Management ──────────────────────────────

  getDeclarations(status?: string, page = 0, size = 10): Observable<ApiResponse<any>> {
    let url = status ? `${this.base}/declarations/status/${status}` : `${this.base}/declarations`;
    const params: string[] = [];
    params.push(`page=${page}`, `size=${size}`);
    url += '?' + params.join('&');
    return this.http.get<ApiResponse<any>>(url);
  }

  getAllDeclarations(): Observable<ApiResponse<DeclarationSummary[]>> {
    return this.http.get<ApiResponse<DeclarationSummary[]>>(`${this.base}/declarations`);
  }

  getDeclarationById(id: number): Observable<ApiResponse<DeclarationDetail>> {
    return this.http.get<ApiResponse<DeclarationDetail>>(`${environment.apiUrl}/declaration/${id}`);
  }

  getUnassignedDeclarations(): Observable<ApiResponse<DeclarationSummary[]>> {
    return this.http.get<ApiResponse<DeclarationSummary[]>>(`${this.base}/declarations/unassigned`);
  }

  unlockDeclaration(id: number): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/declarations/${id}/unlock`, {});
  }

  // ── Assignment Management ───────────────────────────────

  getAssignments(status?: string, page = 0, size = 10): Observable<ApiResponse<Page<AgentTaskSummary>>> {
    let url = `${this.base}/assignments`;
    const params: string[] = [];
    if (status) params.push(`status=${status}`);
    params.push(`page=${page}`, `size=${size}`);
    url += '?' + params.join('&');
    return this.http.get<ApiResponse<Page<AgentTaskSummary>>>(url);
  }

  getAssignedDeclarations(): Observable<ApiResponse<AgentTaskSummary[]>> {
    return this.http.get<ApiResponse<AgentTaskSummary[]>>(`${this.base}/declarations/assigned`);
  }

  assignAgent(declarationId: number, agentId: number): Observable<ApiResponse<AgentTaskSummary>> {
    return this.http.post<ApiResponse<AgentTaskSummary>>(`${this.base}/assignment/assign`, { declarationId, agentId });
  }

  reassignDeclaration(declarationId: number, newAgentId: number, reason: string): Observable<ApiResponse<AgentTaskSummary>> {
    return this.http.put<ApiResponse<AgentTaskSummary>>(`${this.base}/assignment/reassign`, { declarationId, newAgentId, reason });
  }

  changeAssignmentAgent(assignmentId: number, newAgentId: number, reason: string): Observable<ApiResponse<AgentTaskSummary>> {
    return this.http.put<ApiResponse<AgentTaskSummary>>(`${this.base}/assignment/change-agent`, { assignmentId, newAgentId, reason });
  }

  cancelAssignment(declarationId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/assignment/cancel/${declarationId}`);
  }

  reassignTask(id: number, newAgentId: number): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.base}/assignments/reassign/${id}?newAgentId=${newAgentId}`, {});
  }

  // ── Reports / Analytics ─────────────────────────────────

  getAnalytics(): Observable<ApiResponse<AdminAnalytics>> {
    return this.http.get<ApiResponse<AdminAnalytics>>(`${this.base}/analytics`);
  }

  getOverview(): Observable<ApiResponse<AdminAnalytics>> {
    return this.http.get<ApiResponse<AdminAnalytics>>(`${this.base}/reports/overview`);
  }

  getAgentPerformanceReport(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.base}/reports/performance`);
  }

  getCarbonHeatmap(): Observable<ApiResponse<any[]>> {
    return this.http.get<ApiResponse<any[]>>(`${this.base}/reports/carbon-heatmap`);
  }

  // ── Policies ────────────────────────────────────────────

  createPolicy(data: Record<string, unknown>): Observable<ApiResponse<Record<string, unknown>>> {
    return this.http.post<ApiResponse<Record<string, unknown>>>(`${this.base}/policies`, data);
  }

  getPolicies(): Observable<ApiResponse<Record<string, unknown>[]>> {
    return this.http.get<ApiResponse<Record<string, unknown>[]>>(`${this.base}/policies`);
  }

  updatePolicy(id: number, data: Record<string, unknown>): Observable<ApiResponse<Record<string, unknown>>> {
    return this.http.put<ApiResponse<Record<string, unknown>>>(`${this.base}/policies/${id}`, data);
  }
}
