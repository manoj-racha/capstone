import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response';
import { DeclarationSummary } from '../models/declaration';
import {
  AgentWorkspace,
  AgentProfile,
  AgentModifyRequest,
  AgentRejectRequest,
  AgentTaskSummary,
} from '../models/agent';

@Injectable({ providedIn: 'root' })
export class AgentService {

  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/agent`;

  // ── Queue & Workspace ──────────────────────────────────────

  /** GET /agent/queue — agent's assigned declarations */
  getQueue(): Observable<ApiResponse<AgentTaskSummary[]>> {
    return this.http.get<ApiResponse<AgentTaskSummary[]>>(`${this.base}/queue`);
  }

  /** GET /agent/declarations/{assignmentId}/workspace */
  getWorkspace(assignmentId: number): Observable<ApiResponse<AgentWorkspace>> {
    return this.http.get<ApiResponse<AgentWorkspace>>(
      `${this.base}/declarations/${assignmentId}/workspace`
    );
  }

  // ── Verification Actions ───────────────────────────────────

  /** POST /agent/verify/{assignmentId}/confirm */
  confirmVerification(
    assignmentId: number,
    gpsLat: number,
    gpsLng: number,
    agentNotes?: string
  ): Observable<ApiResponse<void>> {
    let params = new HttpParams()
      .set('gpsLat', gpsLat.toString())
      .set('gpsLng', gpsLng.toString());
    if (agentNotes) {
      params = params.set('agentNotes', agentNotes);
    }
    return this.http.post<ApiResponse<void>>(
      `${this.base}/verify/${assignmentId}/confirm`, null, { params }
    );
  }

  /** POST /agent/verify/{assignmentId}/modify */
  modifyAndVerify(
    assignmentId: number,
    data: AgentModifyRequest,
    gpsLat: number,
    gpsLng: number
  ): Observable<ApiResponse<void>> {
    const params = new HttpParams()
      .set('gpsLat', gpsLat.toString())
      .set('gpsLng', gpsLng.toString());
    return this.http.post<ApiResponse<void>>(
      `${this.base}/verify/${assignmentId}/modify`, data, { params }
    );
  }

  /** POST /agent/verify/{assignmentId}/reject */
  rejectDeclaration(
    assignmentId: number,
    data: AgentRejectRequest,
    gpsLat: number,
    gpsLng: number
  ): Observable<ApiResponse<void>> {
    const params = new HttpParams()
      .set('gpsLat', gpsLat.toString())
      .set('gpsLng', gpsLng.toString());
    return this.http.post<ApiResponse<void>>(
      `${this.base}/verify/${assignmentId}/reject`, data, { params }
    );
  }

  // ── Profile & History ──────────────────────────────────────

  /** GET /agent/profile */
  getProfile(): Observable<ApiResponse<AgentProfile>> {
    return this.http.get<ApiResponse<AgentProfile>>(`${this.base}/profile`);
  }

  /** GET /agent/history */
  getHistory(): Observable<ApiResponse<AgentTaskSummary[]>> {
    return this.http.get<ApiResponse<AgentTaskSummary[]>>(`${this.base}/history`);
  }

  // ── GPS Helper ─────────────────────────────────────────────

  /** Returns browser GPS coordinates. Rejects if not supported or denied. */
  getCurrentPosition(): Promise<GeolocationPosition> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Geolocation is not supported by this browser.'));
        return;
      }
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      });
    });
  }
}
