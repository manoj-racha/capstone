import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeclarationReviewComponent } from './declaration-review.component';

describe('DeclarationReviewComponent', () => {
  let component: DeclarationReviewComponent;
  let fixture: ComponentFixture<DeclarationReviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationReviewComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeclarationReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
