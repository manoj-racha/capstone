import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { DeclarationReviewComponent } from './declaration-review.component';

describe('DeclarationReviewComponent', () => {
  let component: DeclarationReviewComponent;
  let fixture: ComponentFixture<DeclarationReviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationReviewComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeclarationReviewComponent);
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

  it('should set error when submit declaration fails', () => {
    component.declarationId.set(22);
    (component as any).declarationService.submitDeclaration = () => of({ success: false, error: 'Submit failed' });

    component.onSubmit();

    expect(component.error()).toBe('Submit failed');
  });

});
