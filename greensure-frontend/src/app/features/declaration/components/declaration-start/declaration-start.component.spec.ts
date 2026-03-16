import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { DeclarationStartComponent } from './declaration-start.component';

describe('DeclarationStartComponent', () => {
  let component: DeclarationStartComponent;
  let fixture: ComponentFixture<DeclarationStartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationStartComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeclarationStartComponent);
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

  it('should set error when start declaration fails', () => {
    (component as any).declarationService.startDeclaration = () => of({ success: false, error: 'Start failed' });

    component.startDeclaration();

    expect(component.error()).toBe('Start failed');
  });

});
