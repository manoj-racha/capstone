import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { PolicyService } from './policy.service';

describe('PolicyService', () => {
  let service: PolicyService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:9090/api/policies';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PolicyService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(PolicyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should get available policies', () => {
    service.getPolicies().subscribe();

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [] });
  });

  it('should buy a policy', () => {
    const request = { planId: 1, durationMonths: 12, finalPrice: 1999 };

    service.buyPolicy(request).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/buy`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ success: true, data: {} });
  });

  it('should get current user policies', () => {
    service.getMyPolicies().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/my-policies`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [] });
  });
});
