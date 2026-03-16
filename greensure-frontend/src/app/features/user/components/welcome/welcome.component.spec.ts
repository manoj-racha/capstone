import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { WelcomeComponent } from './welcome.component';

describe('WelcomeComponent', () => {
  let component: WelcomeComponent;
  let fixture: ComponentFixture<WelcomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WelcomeComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WelcomeComponent);
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

  it('should derive firstName from auth profile or fallback', () => {
    expect(component.firstName.length).toBeGreaterThan(0);
  });

  it('should fallback to User when fullName is not available', () => {
    localStorage.removeItem('fullName');

    const fallbackFixture = TestBed.createComponent(WelcomeComponent);
    const fallbackComponent = fallbackFixture.componentInstance;

    expect(fallbackComponent.firstName).toBe('User');
  });

});
