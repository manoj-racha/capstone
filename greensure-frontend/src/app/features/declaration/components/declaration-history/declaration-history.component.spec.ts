import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeclarationHistoryComponent } from './declaration-history.component';

describe('DeclarationHistoryComponent', () => {
  let component: DeclarationHistoryComponent;
  let fixture: ComponentFixture<DeclarationHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationHistoryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeclarationHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
