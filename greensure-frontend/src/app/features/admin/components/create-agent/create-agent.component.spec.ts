import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { CreateAgentComponent } from './create-agent.component';

describe('CreateAgentComponent', () => {
  let component: CreateAgentComponent;
  let fixture: ComponentFixture<CreateAgentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateAgentComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateAgentComponent);
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

  it('should not submit when create form is invalid', () => {
    let called = false;
    (component as any).adminService.createAgent = () => {
      called = true;
      return of({ success: true });
    };

    component.createForm.patchValue({ fullName: '', email: '' });
    component.onSubmit();

    expect(called).toBe(false);
  });

  it('should enforce create form validator rules', () => {
    component.createForm.patchValue({
      fullName: 'Al',
      email: 'invalid-email',
      mobile: '12345',
      password: '123',
      employeeId: '',
      assignedZones: ''
    });

    expect(component.createForm.get('fullName')?.hasError('minlength')).toBe(true);
    expect(component.createForm.get('email')?.hasError('email')).toBe(true);
    expect(component.createForm.get('mobile')?.hasError('pattern')).toBe(true);
    expect(component.createForm.get('password')?.hasError('minlength')).toBe(true);
    expect(component.createForm.get('employeeId')?.hasError('required')).toBe(true);
    expect(component.createForm.get('assignedZones')?.hasError('required')).toBe(true);
  });

});
