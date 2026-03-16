import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { TaskDetailComponent } from './task-detail.component';

describe('TaskDetailComponent', () => {
  let component: TaskDetailComponent;
  let fixture: ComponentFixture<TaskDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskDetailComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TaskDetailComponent);
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

  it('should navigate directly to verify when task is already started', () => {
    component.task.set({ status: 'IN_PROGRESS' } as any);
    component.assignmentId.set(99);

    let navTo: any = null;
    (component as any).router.navigate = (to: any) => { navTo = to; return Promise.resolve(true); };

    component.onStartVerification();

    expect(navTo).toEqual(['/agent/verify', 99]);
  });

});
