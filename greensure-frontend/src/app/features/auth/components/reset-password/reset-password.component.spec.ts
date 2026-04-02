import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

import { ResetPasswordComponent } from './reset-password.component';

describe('ResetPasswordComponent', () => {
  let component: ResetPasswordComponent;
  let fixture: ComponentFixture<ResetPasswordComponent>;

  const authServiceSpy = {
    resetPassword: vi.fn()
  };

  const toastServiceSpy = {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  };

  beforeEach(async () => {
    authServiceSpy.resetPassword.mockReturnValue(of({ success: true, data: 'ok' }));

    await TestBed.configureTestingModule({
      imports: [ResetPasswordComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    authServiceSpy.resetPassword.mockReset();
    toastServiceSpy.success.mockReset();
    toastServiceSpy.error.mockReset();
    toastServiceSpy.warning.mockReset();
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

  it('should validate password confirmation match', () => {
    component.token.set('token123');
    component.form.patchValue({
      newPassword: 'password123',
      confirmPassword: 'password456'
    });

    expect(component.passwordMismatch()).toBe(true);
  });

  it('should validate that token exists before submit', () => {
    component.token.set('');
    component.form.patchValue({
      newPassword: 'password123',
      confirmPassword: 'password123'
    });

    component.onSubmit();

    expect(component.tokenExpired()).toBe(true);
  });

  it('should validate minimum password length', () => {
    component.form.patchValue({
      newPassword: 'short',
      confirmPassword: 'short'
    });

    expect(component.form.controls.newPassword.invalid).toBe(true);
    expect(component.form.controls.newPassword.errors?.['minlength']).toBeTruthy();
  });

});
