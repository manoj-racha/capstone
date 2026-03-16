import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { DeclarationsComponent } from './declarations.component';

describe('DeclarationsComponent', () => {
  let component: DeclarationsComponent;
  let fixture: ComponentFixture<DeclarationsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationsComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeclarationsComponent);
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

  it('should set error when loadDeclarations returns failure', () => {
    (component as any).adminService.getDeclarations = () => of({ success: false, error: 'Declarations failed' });

    component.loadDeclarations();

    expect(component.error()).toBe('Declarations failed');
  });

});
