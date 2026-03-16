import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { DeclarationService } from './declaration.service';
import { environment } from '../../environments/environment';

describe('DeclarationService', () => {
  let service: DeclarationService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DeclarationService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(DeclarationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should start declaration', () => {
    service.startDeclaration().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/declaration/start`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ success: true, data: {} });
  });

  it('should save declaration draft', () => {
    const payload = { declarationYear: 2026 } as any;

    service.saveDraft(1, payload).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/declaration/1/save`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true, data: {} });
  });

  it('should submit declaration', () => {
    service.submitDeclaration(1).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/declaration/1/submit`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({});
    req.flush({ success: true, data: {} });
  });

  it('should add vehicle', () => {
    const vehicle = { vehicleType: 'CAR', fuelType: 'PETROL', kmPerMonth: 500, quantity: 1 } as any;

    service.addVehicle(3, vehicle).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/declaration/3/vehicle`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(vehicle);
    req.flush({ success: true, data: {} });
  });

  it('should remove vehicle', () => {
    service.removeVehicle(3, 9).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/declaration/3/vehicle/9`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true });
  });

  it('should get declaration by id', () => {
    service.getDeclaration(11).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/declaration/11`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should get declaration history', () => {
    service.getHistory().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/declaration/history`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [] });
  });
});
