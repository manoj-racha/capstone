import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeclarationVehiclesComponent } from './declaration-vehicles.component';

describe('DeclarationVehiclesComponent', () => {
  let component: DeclarationVehiclesComponent;
  let fixture: ComponentFixture<DeclarationVehiclesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationVehiclesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeclarationVehiclesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
