import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { AgentDashboardComponent } from './agent-dashboard.component';

describe('AgentDashboardComponent', () => {
  let component: AgentDashboardComponent;
  let fixture: ComponentFixture<AgentDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentDashboardComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AgentDashboardComponent);
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

  it('should set filter when loading tasks', () => {
    (component as any).agentService.getAssignments = () => of({ success: true, data: [] });

    component.loadTasks('IN_PROGRESS');

    expect(component.currentFilter()).toBe('IN_PROGRESS');
  });

  it('should set error when assignments request is unsuccessful', () => {
    (component as any).agentService.getAssignments = () =>
      of({ success: false, error: 'Failed to load assignments.' });

    component.loadTasks('ASSIGNED');

    expect(component.error()).toBe('Failed to load assignments.');
  });

  it('should return overdue badge class for non-completed overdue tasks', () => {
    const badge = component.getStatusBadgeClass('IN_PROGRESS', true);

    expect(badge).toContain('text-red-500');
  });

  it('should display human readable status text', () => {
    expect(component.getStatusDisplay('IN_PROGRESS')).toBe('IN PROGRESS');
  });

});
