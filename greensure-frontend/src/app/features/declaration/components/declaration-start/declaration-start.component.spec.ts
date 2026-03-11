import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeclarationStartComponent } from './declaration-start.component';

describe('DeclarationStartComponent', () => {
  let component: DeclarationStartComponent;
  let fixture: ComponentFixture<DeclarationStartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationStartComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeclarationStartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
