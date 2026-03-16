import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { AgentNotificationsComponent } from './agent-notifications.component';

describe('AgentNotificationsComponent', () => {
  let component: AgentNotificationsComponent;
  let fixture: ComponentFixture<AgentNotificationsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentNotificationsComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AgentNotificationsComponent);
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

  it('should mark agent notification as read in local state', () => {
    component.notifications.set([{ notificationId: 11, status: 'UNREAD' } as any]);
    (component as any).notificationService.markAsRead = () => of({ success: true });

    component.markAsRead(11);

    expect(component.notifications()[0].status).toBe('READ');
  });

});
