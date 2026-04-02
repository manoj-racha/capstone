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
    component.profileForm.patchValue({ pincode: '12ab' });

    expect(component.profileForm.get('pincode')?.hasError('pattern')).toBe(true);
  });

  it('should patch profile data into current form fields', () => {
    (component as any).userService.getProfile = () =>
      of({
        success: true,
        data: {
          fullName: 'Test User',
          phone: '9876543210',
          address: 'A',
          city: 'B',
          pincode: '560001',
          householdSize: 4
        }
      });

    (component as any).loadProfile();

    expect(component.profileForm.get('fullName')?.value).toBe('Test User');
    expect(component.profileForm.get('phone')?.value).toBe('9876543210');
    expect(component.profileForm.get('pincode')?.value).toBe('560001');
    expect(component.profileForm.get('householdSize')?.value).toBe(4);
  });

  it('should enforce minimum household size validation', () => {
    component.profileForm.patchValue({ householdSize: 0 });

    expect(component.profileForm.get('householdSize')?.hasError('min')).toBe(true);
  });

});
