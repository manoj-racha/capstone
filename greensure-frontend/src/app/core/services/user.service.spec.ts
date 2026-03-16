import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { UserService } from './user.service';
import { environment } from '../../environments/environment';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UserService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should get profile', () => {
    service.getProfile().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/user/profile`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should update profile', () => {
    const payload = { fullName: 'Alice' };

    service.updateProfile(payload).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/user/profile`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true, data: payload });
  });

  it('should get dashboard data', () => {
    service.getDashboard().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/user/dashboard`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });
});
