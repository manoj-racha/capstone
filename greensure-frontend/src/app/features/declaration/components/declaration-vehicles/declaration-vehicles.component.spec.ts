import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { DeclarationVehiclesComponent } from './declaration-vehicles.component';

describe('DeclarationVehiclesComponent', () => {
  let component: DeclarationVehiclesComponent;
  let fixture: ComponentFixture<DeclarationVehiclesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationVehiclesComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeclarationVehiclesComponent);
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

  it('should not add vehicle when form is invalid', () => {
    let called = false;
    (component as any).declarationService.addVehicle = () => {
      called = true;
      return of({ success: true, data: {} });
    };

    component.vehicleForm.get('kmPerMonth')?.setValue(null);
    component.onAddVehicle();

    expect(called).toBe(false);
  });

  it('should enforce vehicle form validator constraints', () => {
    component.vehicleForm.patchValue({
      vehicleType: null,
      kmPerMonth: -2,
      quantity: 0
    });

    expect(component.vehicleForm.get('vehicleType')?.hasError('required')).toBe(true);
    expect(component.vehicleForm.get('kmPerMonth')?.hasError('min')).toBe(true);
    expect(component.vehicleForm.get('quantity')?.hasError('min')).toBe(true);
  });

});
