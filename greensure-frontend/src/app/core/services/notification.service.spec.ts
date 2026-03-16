import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { NotificationService } from './notification.service';
import { environment } from '../../environments/environment';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [NotificationService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch notifications', () => {
    service.getMyNotifications().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/notifications/my`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [] });
  });

  it('should mark one notification as read', () => {
    service.markAsRead(12).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/notifications/12/read`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({});
    req.flush({ success: true });
  });

  it('should mark all notifications as read', () => {
    service.markAllAsRead().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/notifications/mark-all-read`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({});
    req.flush({ success: true });
  });

  it('should fetch unread count', () => {
    service.getUnreadCount().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/notifications/unread-count`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: 5 });
  });
});
