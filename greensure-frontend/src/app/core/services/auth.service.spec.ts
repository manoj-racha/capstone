import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  function buildJwt(expOffsetSeconds: number): string {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + expOffsetSeconds }));
    return `${header}.${payload}.signature`;
  }

  it('should post login request', () => {
    const body = { email: 'a@example.com', password: 'secret' };

    service.login(body).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ success: true, data: {} });
  });

  it('should post register request', () => {
    const body = {
      userType: 'HOUSEHOLD',
      fullName: 'Alice',
      email: 'a@example.com',
      password: 'secret'
    } as any;

    service.register(body).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/auth/register`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ success: true, data: {} });
  });

  it('should post logout request', () => {
    service.logout().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/auth/logout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ success: true });
  });

  it('should post forgot-password request', () => {
    service.forgotPassword('a@example.com').subscribe();

    const req = httpMock.expectOne(`${baseUrl}/auth/forgot-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@example.com' });
    req.flush({ success: true });
  });

  it('should post reset-password request', () => {
    service.resetPassword('token123', 'new-pass').subscribe();

    const req = httpMock.expectOne(`${baseUrl}/auth/reset-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ token: 'token123', newPassword: 'new-pass' });
    req.flush({ success: true });
  });

  it('should return true when a valid token exists', () => {
    localStorage.setItem('token', buildJwt(3600));

    expect(service.isLoggedIn()).toBe(true);
  });

  it('should clear session and return false when token is expired', () => {
    localStorage.setItem('token', buildJwt(-3600));
    localStorage.setItem('role', 'USER');

    expect(service.isLoggedIn()).toBe(false);
    expect(localStorage.getItem('role')).toBeNull();
  });

  it('should return false for malformed token', () => {
    localStorage.setItem('token', 'bad-token');

    expect(service.isLoggedIn()).toBe(false);
  });

  it('should read role fullName and userId from localStorage', () => {
    localStorage.setItem('role', 'ADMIN');
    localStorage.setItem('fullName', 'Alice Doe');
    localStorage.setItem('userId', '99');

    expect(service.getRole()).toBe('ADMIN');
    expect(service.getFullName()).toBe('Alice Doe');
    expect(service.getUserId()).toBe('99');
  });

  it('should clear session manually', () => {
    localStorage.setItem('token', 't');

    service.clearSession();

    expect(localStorage.getItem('token')).toBeNull();
  });
});
