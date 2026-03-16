import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { LandingComponent } from './landing.component';

describe('LandingComponent', () => {
  let component: LandingComponent;
  let fixture: ComponentFixture<LandingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LandingComponent);
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

  it('should redirect logged-in agent to agent dashboard', () => {
    const auth = (component as any).authService;
    const router = (component as any).router;
    let navTo: any = null;

    auth.isLoggedIn = () => true;
    auth.getRole = () => 'AGENT';
    router.navigate = (to: any) => { navTo = to; return Promise.resolve(true); };

    component.ngOnInit();

    expect(navTo).toEqual(['/agent/dashboard']);
  });

});
