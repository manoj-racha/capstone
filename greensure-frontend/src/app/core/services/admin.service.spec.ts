import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { AdminService } from './admin.service';
import { environment } from '../../environments/environment';

describe('AdminService', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AdminService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should get users with filters', () => {
    service.getUsers('HOUSEHOLD', 'ACTIVE', 1, 20).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/users?userType=HOUSEHOLD&status=ACTIVE&page=1&size=20`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should get user by id', () => {
    service.getUserById(2).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/users/2`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should update user status', () => {
    service.updateUserStatus(2, 'SUSPENDED').subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/users/2/status?status=SUSPENDED`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({});
    req.flush({ success: true });
  });

  it('should unlock user resubmission', () => {
    service.unlockResubmission(2).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/users/2/unlock-resubmission`);
    expect(req.request.method).toBe('PUT');
    req.flush({ success: true });
  });

  it('should create agent', () => {
    const payload = { fullName: 'Agent A', email: 'agent@example.com' } as any;

    service.createAgent(payload).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/agents/create`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true, data: {} });
  });

  it('should get agents list', () => {
    service.getAgents(2, 25).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/agents?page=2&size=25`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should get agent by id', () => {
    service.getAgentById(5).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/agents/5`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should update agent status', () => {
    service.updateAgentStatus(5, 'ACTIVE').subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/agents/5/status?status=ACTIVE`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({});
    req.flush({ success: true });
  });

  it('should clear agent strikes', () => {
    service.clearStrikes(5).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/agents/5/clear-strikes`);
    expect(req.request.method).toBe('PUT');
    req.flush({ success: true });
  });

  it('should get declarations', () => {
    service.getDeclarations('SUBMITTED', 0, 10).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/declarations?status=SUBMITTED&page=0&size=10`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should get declaration by id', () => {
    service.getDeclarationById(10).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/declarations/10`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should unlock declaration', () => {
    service.unlockDeclaration(10).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/declarations/10/unlock`);
    expect(req.request.method).toBe('PUT');
    req.flush({ success: true });
  });

  it('should get assignments', () => {
    service.getAssignments('ASSIGNED', 1, 5).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/assignments?status=ASSIGNED&page=1&size=5`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should reassign task', () => {
    service.reassignTask(14, 3).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/assignments/reassign/14?newAgentId=3`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ success: true });
  });

  it('should get overview report', () => {
    service.getOverview().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/reports/overview`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should get agent performance report', () => {
    service.getAgentPerformanceReport().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/reports/performance`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should get carbon heatmap report', () => {
    service.getCarbonHeatmap().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/admin/reports/carbon-heatmap`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [] });
  });
});
