import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
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

  it('should clear session on logout', () => {
    localStorage.setItem('gs_token', 'abc');
    service.logout();
    expect(localStorage.getItem('gs_token')).toBeNull();
  });

  it('should post forgot-password request', () => {
    service.forgotPassword({ email: 'a@example.com' }).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/auth/forgot-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@example.com' });
    req.flush({ success: true });
  });

  it('should post reset-password request', () => {
    service.resetPassword({ token: 'token123', newPassword: 'new-pass' }).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/auth/reset-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ token: 'token123', newPassword: 'new-pass' });
    req.flush({ success: true });
  });

  it('should return true when a valid token exists', () => {
    localStorage.setItem('token', buildJwt(3600));

    service.saveSession({ token: buildJwt(3600), role: 'USER', id: 1, userId: 1, fullName: 'User', email: 'a@example.com' } as any);
    expect(service.isLoggedIn()).toBe(true);
  });

  it('should clear session and return false when token is expired', () => {
    service.saveSession({ token: buildJwt(-3600), role: 'USER', id: 1, userId: 1, fullName: 'User', email: 'a@example.com' } as any);
    service.clearSession();
    expect(service.isLoggedIn()).toBe(false);
  });

  it('should return false for malformed token', () => {
    localStorage.setItem('gs_token', 'bad-token');
    localStorage.setItem('gs_user', JSON.stringify({ token: 'bad-token' }));
    const fresh = TestBed.inject(AuthService);
    expect(fresh.isLoggedIn()).toBe(false);
  });

  it('should read role fullName and userId from localStorage', () => {
    service.saveSession({ token: buildJwt(3600), role: 'ADMIN', id: 99, userId: 99, fullName: 'Alice Doe', email: 'a@example.com' } as any);
    expect(service.getRole()).toBe('ADMIN');
    expect(service.getFullName()).toBe('Alice Doe');
    expect(service.getUserId()).toBe(99);
  });

  it('should clear session manually', () => {
    localStorage.setItem('token', 't');

    service.clearSession();

    expect(localStorage.getItem('token')).toBeNull();
  });
});
