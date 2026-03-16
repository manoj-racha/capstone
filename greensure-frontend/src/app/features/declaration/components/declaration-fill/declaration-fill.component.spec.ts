import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { DeclarationFillComponent } from './declaration-fill.component';

describe('DeclarationFillComponent', () => {
  let component: DeclarationFillComponent;
  let fixture: ComponentFixture<DeclarationFillComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationFillComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeclarationFillComponent);
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

  it('should not save draft when declaration form is invalid', () => {
    let called = false;
    (component as any).declarationService.saveDraft = () => {
      called = true;
      return of({ success: true });
    };

    component.fillForm.get('electricityUnits')?.setValue(null);
    component.onSaveAndContinue();

    expect(called).toBe(false);
  });

  it('should enforce non-negative validator rules for numeric fields', () => {
    component.fillForm.get('electricityUnits')?.setValue(-1);
    component.fillForm.get('solarUnits')?.setValue(-10);

    expect(component.fillForm.get('electricityUnits')?.hasError('min')).toBe(true);
    expect(component.fillForm.get('solarUnits')?.hasError('min')).toBe(true);
  });

  it('should mark required field as touched when invalid submit happens', () => {
    component.fillForm.get('electricityUnits')?.setValue(null);

    component.onSaveAndContinue();

    expect(component.fillForm.get('electricityUnits')?.touched).toBe(true);
  });

});
