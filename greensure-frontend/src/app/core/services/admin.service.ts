import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response';
import { DeclarationSummary } from '../models/declaration';
import { AgentProfile } from '../models/agent';
import {
  AdminAnalytics,
  CreateAgentRequest,
  ManualAssignRequest,
  ReassignRequest,
} from '../models/admin';

@Injectable({ providedIn: 'root' })
export class AdminService {

  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/admin`;

  // ── Declarations ───────────────────────────────────────────

  /** GET /admin/declarations */
  getAllDeclarations(): Observable<ApiResponse<DeclarationSummary[]>> {
    return this.http.get<ApiResponse<DeclarationSummary[]>>(`${this.base}/declarations`);
  }

  /** GET /admin/declarations/unassigned */
  getUnassignedDeclarations(): Observable<ApiResponse<DeclarationSummary[]>> {
    return this.http.get<ApiResponse<DeclarationSummary[]>>(`${this.base}/declarations/unassigned`);
  }

  /** GET /admin/declarations/assigned */
  getAssignedDeclarations(): Observable<ApiResponse<any[]>> {
    return this.http.get<ApiResponse<any[]>>(`${this.base}/declarations/assigned`);
  }

  // ── Agents ─────────────────────────────────────────────────

  /** GET /admin/agents */
  getAllAgents(): Observable<ApiResponse<AgentProfile[]>> {
    return this.http.get<ApiResponse<AgentProfile[]>>(`${this.base}/agents`);
  }

  /** GET /admin/agents/available */
  getAvailableAgents(): Observable<ApiResponse<AgentProfile[]>> {
    return this.http.get<ApiResponse<AgentProfile[]>>(`${this.base}/agents/available`);
  }

  /** POST /admin/agents/create */
  createAgent(data: CreateAgentRequest): Observable<ApiResponse<AgentProfile>> {
    return this.http.post<ApiResponse<AgentProfile>>(`${this.base}/agents/create`, data);
  }

  /** PUT /admin/agents/{agentId}/status?status=SUSPENDED */
  suspendAgent(agentId: number): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/agents/${agentId}/status?status=SUSPENDED`, null);
  }

  /** PUT /admin/agents/{agentId}/status?status=ACTIVE */
  activateAgent(agentId: number): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/agents/${agentId}/status?status=ACTIVE`, null);
  }

  // ── Assignments ────────────────────────────────────────────

  /** POST /admin/assignment/assign */
  assignAgent(declarationId: number, agentId: number, reason?: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.base}/assignment/assign`, { declarationId, agentId, reason });
  }

  /** PUT /admin/assignment/reassign */
  reassignDeclaration(declarationId: number, agentId: number, reason: string): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/assignment/reassign`, { declarationId, agentId, reason });
  }

  /** DELETE /admin/assignment/cancel/{id} */
  cancelAssignment(declarationId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/assignment/cancel/${declarationId}`);
  }

  // ── Analytics ──────────────────────────────────────────────

  /** GET /admin/analytics */
  getAnalytics(): Observable<ApiResponse<AdminAnalytics>> {
    return this.http.get<ApiResponse<AdminAnalytics>>(`${this.base}/analytics`);
  }

  // ── Policies ───────────────────────────────────────────────

  /** POST /admin/policies */
  createPolicy(data: Record<string, unknown>): Observable<ApiResponse<Record<string, unknown>>> {
    return this.http.post<ApiResponse<Record<string, unknown>>>(`${this.base}/policies`, data);
  }

  /** GET /admin/policies */
  getPolicies(): Observable<ApiResponse<Record<string, unknown>[]>> {
    return this.http.get<ApiResponse<Record<string, unknown>[]>>(`${this.base}/policies`);
  }

  /** PUT /admin/policies/{id} */
  updatePolicy(id: number, data: Record<string, unknown>): Observable<ApiResponse<Record<string, unknown>>> {
    return this.http.put<ApiResponse<Record<string, unknown>>>(`${this.base}/policies/${id}`, data);
  }
}
