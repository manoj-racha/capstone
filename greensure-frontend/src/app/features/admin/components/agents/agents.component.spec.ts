import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { AgentsComponent } from './agents.component';

describe('AgentsComponent', () => {
  let component: AgentsComponent;
  let fixture: ComponentFixture<AgentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentsComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AgentsComponent);
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

  it('should set error when loadAgents returns failure', () => {
    (component as any).adminService.getAgents = () => of({ success: false, error: 'Agents failed' });

    component.loadAgents();

    expect(component.error()).toBe('Agents failed');
  });

});
