import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { DeclarationHistoryComponent } from './declaration-history.component';

describe('DeclarationHistoryComponent', () => {
  let component: DeclarationHistoryComponent;
  let fixture: ComponentFixture<DeclarationHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationHistoryComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeclarationHistoryComponent);
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

  it('should format status display from underscore text', () => {
    expect(component.getStatusDisplay('UNDER_VERIFICATION')).toBe('UNDER VERIFICATION');
  });

});
