import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { AgentService } from './agent.service';
import { environment } from '../../environments/environment';

describe('AgentService', () => {
  let service: AgentService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AgentService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AgentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should get dashboard tasks', () => {
    service.getDashboard().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/agent/dashboard`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [] });
  });

  it('should get assignments with status filter', () => {
    service.getAssignments('ASSIGNED').subscribe();

    const req = httpMock.expectOne(`${baseUrl}/agent/assignments?status=ASSIGNED`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [] });
  });

  it('should get assignments without status filter', () => {
    service.getAssignments().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/agent/assignments`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [] });
  });

  it('should get assignment detail', () => {
    service.getAssignment(9).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/agent/assignment/9`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should get declaration for assignment', () => {
    service.getDeclarationForAssignment(9).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/agent/assignment/9/declaration`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should start assignment', () => {
    service.startAssignment(9).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/agent/assignment/9/start`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({});
    req.flush({ success: true, data: {} });
  });

  it('should submit verification', () => {
    const body = {
      overallAction: 'CONFIRMED',
      agentGpsLat: 18,
      agentGpsLng: 73
    } as any;

    service.submitVerification(9, body).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/agent/verification/9/submit`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ success: true });
  });

  it('should get performance report', () => {
    service.getPerformance().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/agent/performance`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });
});
