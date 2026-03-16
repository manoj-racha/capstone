import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { PoliciesComponent } from './policies.component';

describe('PoliciesComponent', () => {
  let component: PoliciesComponent;
  let fixture: ComponentFixture<PoliciesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PoliciesComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PoliciesComponent);
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

  it('should cap total discount at 35 percent', () => {
    const result = component.calculateDiscount({
      perCapitaCo2: 200,
      zone: 'GREEN_CHAMPION',
      improvementPercentage: 50
    } as any);

    expect(result.totalDiscount).toBe(35);
  });

});
