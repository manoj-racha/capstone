import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
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

  it('should show error when passwords do not match', () => {
    component.password.set('password123');
    component.confirmPassword.set('different123');

    component.onRegister();

    expect(component.errorMessage()).toBe('Passwords do not match');
  });

  it('should show pin code validation error for invalid pin', () => {
    component.password.set('password123');
    component.confirmPassword.set('password123');
    component.pinCode.set('1234');

    component.onRegister();

    expect(component.errorMessage()).toContain('6-digit pin code');
  });

  it('should show required pin code validation message when pin is empty', () => {
    component.password.set('password123');
    component.confirmPassword.set('password123');
    component.pinCode.set('');

    component.onRegister();

    expect(component.errorMessage()).toBe('Pin code is required');
  });

  it('should require dwelling type for household registration', () => {
    component.userType.set('HOUSEHOLD');
    component.password.set('password123');
    component.confirmPassword.set('password123');
    component.pinCode.set('123456');
    component.dwellingType.set('');

    component.onRegister();

    expect(component.errorMessage()).toBe('Please select your dwelling type');
  });

  it('should return early with invalid template form input', () => {
    component.onRegister({ invalid: true });

    expect(component.errorMessage()).toBe('Please fill out all required fields correctly.');
  });

});
