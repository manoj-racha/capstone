import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { NotificationsComponent } from './notifications.component';

describe('NotificationsComponent', () => {
  let component: NotificationsComponent;
  let fixture: ComponentFixture<NotificationsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationsComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NotificationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render non-empty template content', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect((compiled.textContent || '').trim().length).toBeGreaterThan(0);
  });

  it('should mark all notifications as read on success', () => {
    component.notifications.set([{ notificationId: 1, status: 'UNREAD' } as any]);
    (component as any).notificationService.markAllAsRead = () => of({ success: true });

    component.markAllAsRead();

    expect(component.notifications()[0].status).toBe('READ');
  });

});
