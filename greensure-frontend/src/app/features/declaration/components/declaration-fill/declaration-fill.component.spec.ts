import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeclarationFillComponent } from './declaration-fill.component';

describe('DeclarationFillComponent', () => {
  let component: DeclarationFillComponent;
  let fixture: ComponentFixture<DeclarationFillComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationFillComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeclarationFillComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
