import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ResetPasswordComponent } from './reset-password.component';

describe('ResetPasswordComponent', () => {
  let component: ResetPasswordComponent;
  let fixture: ComponentFixture<ResetPasswordComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResetPasswordComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ResetPasswordComponent);
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

  it('should validate password confirmation match', () => {
    component.token.set('token123');
    component.newPassword.set('password123');
    component.confirmPassword.set('password456');

    component.onSubmit();

    expect(component.errorMessage()).toBe('Passwords do not match');
  });

  it('should validate that token exists before submit', () => {
    component.token.set('');
    component.newPassword.set('password123');
    component.confirmPassword.set('password123');

    component.onSubmit();

    expect(component.errorMessage()).toContain('No reset token found');
  });

  it('should validate minimum password length', () => {
    component.token.set('token123');
    component.newPassword.set('short');
    component.confirmPassword.set('short');

    component.onSubmit();

    expect(component.errorMessage()).toBe('Password must be at least 8 characters');
  });

});
