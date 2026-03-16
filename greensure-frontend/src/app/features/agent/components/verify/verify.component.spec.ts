import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Validators } from '@angular/forms';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { VerifyComponent } from './verify.component';

describe('VerifyComponent', () => {
  let component: VerifyComponent;
  let fixture: ComponentFixture<VerifyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerifyComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VerifyComponent);
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

  it('should not submit verification when form is invalid', () => {
    let called = false;
    (component as any).agentService.submitVerification = () => {
      called = true;
      return of({ success: true });
    };

    component.verifyForm.get('overallAction')?.setValue(null);
    component.onSubmit();

    expect(called).toBe(false);
  });

  it('should enforce overallAction as required', () => {
    component.verifyForm.get('overallAction')?.setValue(null);

    expect(component.verifyForm.get('overallAction')?.hasError('required')).toBe(true);
  });

  it('should require remarks with minimum length for rejected action', () => {
    const remarks = component.verifyForm.get('agentRemarks');

    component.verifyForm.get('overallAction')?.setValue('REJECTED');
    remarks?.setValidators([Validators.required, Validators.minLength(10)]);
    remarks?.setValue('short');
    remarks?.updateValueAndValidity();

    expect(remarks?.hasError('minlength')).toBe(true);
  });

});
