import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { RecommendationsComponent } from './recommendations.component';

describe('RecommendationsComponent', () => {
  let component: RecommendationsComponent;
  let fixture: ComponentFixture<RecommendationsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecommendationsComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RecommendationsComponent);
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

  it('should set error when recommendations API fails', () => {
    (component as any).http.get = () => of({ success: false, error: 'Recommendations failed' });

    component.ngOnInit();

    expect(component.error()).toBe('Recommendations failed');
  });

});
