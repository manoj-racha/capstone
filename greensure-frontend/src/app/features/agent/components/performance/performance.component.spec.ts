import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { PerformanceComponent } from './performance.component';

describe('PerformanceComponent', () => {
  let component: PerformanceComponent;
  let fixture: ComponentFixture<PerformanceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PerformanceComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PerformanceComponent);
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

  it('should format empty percentage as zero', () => {
    expect(component.formatPct(undefined)).toBe('0%');
  });

});
