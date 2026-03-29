import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, Page } from '../../../core/models/api-response';
import { CreateAgentRequest, AdminAnalytics, ManualAssignRequest, ReassignRequest, UnassignedDeclaration, AvailableAgent } from '../../../core/models/admin';
import { UserProfile } from '../../../core/models/user';
import { AgentProfile, AgentTaskSummary } from '../../../core/models/agent';
import { DeclarationSummary, DeclarationDetail } from '../../../core/models/declaration';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private normalizeAgent(agent: any): AgentProfile {
    const status = agent?.status ?? (agent?.active ? 'ACTIVE' : 'SUSPENDED');
    return {
      ...agent,
      strikes: Number(agent?.strikes ?? agent?.strikeCount ?? 0),
      activeAssignments: Number(agent?.activeAssignments ?? 0),
      active: status === 'ACTIVE',
      status
    } as AgentProfile;
  }


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

  unlockResubmission(id: number): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/users/${id}/unlock-resubmission`, {});
  }

  // ── Agent Management ────────────────────────────────────

  createAgent(data: CreateAgentRequest): Observable<ApiResponse<AgentProfile>> {
    return this.http.post<ApiResponse<AgentProfile>>(`${this.base}/agents/create`, data);
  }

  getAgents(page = 0, size = 10): Observable<ApiResponse<Page<AgentProfile>>> {
    return this.http
      .get<ApiResponse<Page<any>>>(`${this.base}/agents?page=${page}&size=${size}`)
      .pipe(
        map((res) => {
          const content = (res?.data?.content ?? []).map((a: any) => this.normalizeAgent(a));
          return {
            ...res,
            data: res.data
              ? { ...res.data, content }
              : (res.data as any)
          } as ApiResponse<Page<AgentProfile>>;
        })
      );
  }

  getAllAgents(): Observable<ApiResponse<AgentProfile[]>> {
    return this.http.get<ApiResponse<any[]>>(`${this.base}/agents`).pipe(
      map((res) => ({
        ...res,
        data: (res.data ?? []).map((a: any) => this.normalizeAgent(a))
      }) as ApiResponse<AgentProfile[]>)
    );
  }

  getAgentById(id: number): Observable<ApiResponse<AgentProfile>> {
    return this.http.get<ApiResponse<any>>(`${this.base}/agents/${id}`).pipe(
      map((res) => ({
        ...res,
        data: res.data ? this.normalizeAgent(res.data) : res.data
      }) as ApiResponse<AgentProfile>)
    );
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

  getAvailableAgents(pinCode?: string): Observable<ApiResponse<AvailableAgent[]>> {
    const url = pinCode
      ? `${this.base}/agents/available?pinCode=${encodeURIComponent(pinCode)}`
      : `${this.base}/agents/available`;
    return this.http.get<ApiResponse<AvailableAgent[]>>(url);
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
    return this.http.get<ApiResponse<DeclarationDetail>>(`${this.base}/declarations/${id}`);
  }

  getUnassignedDeclarations(): Observable<ApiResponse<UnassignedDeclaration[]>> {
    return this.http.get<ApiResponse<UnassignedDeclaration[]>>(`${this.base}/declarations/unassigned`);
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
