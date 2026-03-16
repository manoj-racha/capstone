import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { ProfileComponent } from './profile.component';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
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

  it('should not update profile when form is invalid', () => {
    let called = false;
    (component as any).userService.updateProfile = () => {
      called = true;
      return of({ success: true, data: {} });
    };

    component.profileForm.patchValue({ fullName: '' });
    component.onSubmit();

    expect(called).toBe(false);
  });

  it('should enforce pin code pattern validation', () => {
    component.profileForm.patchValue({ pinCode: '12ab' });

    expect(component.profileForm.get('pinCode')?.hasError('pattern')).toBe(true);
  });

  it('should add household-specific validators from profile data', () => {
    (component as any).userService.getProfile = () =>
      of({
        success: true,
        data: {
          userType: 'HOUSEHOLD',
          fullName: 'Test User',
          mobile: '9876543210',
          address: 'A',
          city: 'B',
          pinCode: '12345',
          numberOfMembers: null
        }
      });

    (component as any).loadProfile();
    component.profileForm.get('numberOfMembers')?.setValue(null);

    expect(component.profileForm.get('numberOfMembers')?.hasError('required')).toBe(true);
  });

  it('should add msme-specific validators from profile data', () => {
    (component as any).userService.getProfile = () =>
      of({
        success: true,
        data: {
          userType: 'MSME',
          fullName: 'Biz User',
          mobile: '9876543210',
          address: 'A',
          city: 'B',
          pinCode: '12345',
          businessName: '',
          industrySector: '',
          employeeCount: 0
        }
      });

    (component as any).loadProfile();
    component.profileForm.patchValue({ businessName: '', industrySector: '', employeeCount: 0 });

    expect(component.profileForm.get('businessName')?.hasError('required')).toBe(true);
    expect(component.profileForm.get('industrySector')?.hasError('required')).toBe(true);
    expect(component.profileForm.get('employeeCount')?.hasError('min')).toBe(true);
  });

});
