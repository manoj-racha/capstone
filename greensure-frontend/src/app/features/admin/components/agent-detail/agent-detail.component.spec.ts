import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { AgentDetailComponent } from './agent-detail.component';

describe('AgentDetailComponent', () => {
  let component: AgentDetailComponent;
  let fixture: ComponentFixture<AgentDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentDetailComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AgentDetailComponent);
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

  it('should show invalid id error when route param is missing', () => {
    component.ngOnInit();

    expect(component.error()).toBe('Invalid agent ID.');
  });

  it('should set error when loading agent fails', () => {
    (component as any).adminService.getAgentById = () =>
      of({ success: false, error: 'Agent not found.' });

    component.agentId.set(99);
    component.loadAgent();

    expect(component.error()).toBe('Agent not found.');
  });

});
