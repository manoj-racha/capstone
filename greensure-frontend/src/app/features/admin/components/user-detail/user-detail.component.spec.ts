import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserDetailComponent } from './user-detail.component';

describe('UserDetailComponent', () => {
  let component: UserDetailComponent;
  let fixture: ComponentFixture<UserDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserDetailComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserDetailComponent);
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

  it('should show invalid id error when route param is missing', () => {
    component.ngOnInit();

    expect(component.error()).toBe('Invalid user ID.');
  });

  it('should set error when user load is unsuccessful', () => {
    (component as any).adminService.getUserById = () =>
      of({ success: false, error: 'User not found.' });

    component.userId.set(55);
    component.loadUser();

    expect(component.error()).toBe('User not found.');
  });

});
