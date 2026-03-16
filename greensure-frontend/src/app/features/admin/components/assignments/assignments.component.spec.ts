import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { AssignmentsComponent } from './assignments.component';

describe('AssignmentsComponent', () => {
  let component: AssignmentsComponent;
  let fixture: ComponentFixture<AssignmentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssignmentsComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AssignmentsComponent);
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

  it('should set error when loadAssignments returns failure', () => {
    (component as any).adminService.getAssignments = () => of({ success: false, error: 'Assignments failed' });

    component.loadAssignments();

    expect(component.error()).toBe('Assignments failed');
  });

});
