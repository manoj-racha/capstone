import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;

  const authServiceSpy = {
    register: vi.fn()
  };

  const toastServiceSpy = {
    warning: vi.fn(),
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn()
  };

  beforeEach(async () => {
    authServiceSpy.register.mockReturnValue(of({ success: true, data: 'ok' }));

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    authServiceSpy.register.mockReset();
    toastServiceSpy.warning.mockReset();
    toastServiceSpy.success.mockReset();
    toastServiceSpy.error.mockReset();
    toastServiceSpy.info.mockReset();
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
    component.registerForm.patchValue({
      password: 'password123',
      confirmPassword: 'different123'
    });

    component.registerForm.updateValueAndValidity();

    expect(component.registerForm.errors?.['passwordsMismatch']).toBe(true);
  });

  it('should show pin code validation error for invalid pin', () => {
    component.registerForm.patchValue({ pinCode: '1234' });
    const pinCodeControl = component.registerForm.controls.pinCode;

    expect(pinCodeControl.invalid).toBe(true);
    expect(pinCodeControl.errors?.['pattern']).toBeTruthy();
  });

  it('should show required pin code validation message when pin is empty', () => {
    component.registerForm.patchValue({ pinCode: '' });
    const pinCodeControl = component.registerForm.controls.pinCode;

    expect(pinCodeControl.invalid).toBe(true);
    expect(pinCodeControl.errors?.['required']).toBeTruthy();
  });

  it('should require dwelling type for household registration', () => {
    component.registerForm.patchValue({ dwellingType: '' });
    const dwellingTypeControl = component.registerForm.controls.dwellingType;

    expect(dwellingTypeControl.invalid).toBe(true);
    expect(dwellingTypeControl.errors?.['required']).toBeTruthy();
  });

  it('should return early with invalid template form input', () => {
    component.onRegister();

    expect(component.errorMessage()).toBe('Please fill out all required fields correctly.');
    expect(toastServiceSpy.warning).toHaveBeenCalled();
  });

  it('should call register for valid form submission', () => {
    component.registerForm.patchValue({
      fullName: 'John Doe',
      email: 'john@example.com',
      mobile: '9876543210',
      address: '123 Main Street',
      pinCode: '560001',
      city: 'Bangalore',
      state: 'Karnataka',
      numberOfMembers: 4,
      dwellingType: 'APARTMENT',
      password: 'password123',
      confirmPassword: 'password123'
    });

    component.onRegister();

    expect(authServiceSpy.register).toHaveBeenCalled();
  });

});
