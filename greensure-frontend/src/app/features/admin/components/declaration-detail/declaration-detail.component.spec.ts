import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { DeclarationDetailComponent } from './declaration-detail.component';

describe('DeclarationDetailComponent', () => {
  let component: DeclarationDetailComponent;
  let fixture: ComponentFixture<DeclarationDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeclarationDetailComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeclarationDetailComponent);
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

  it('should show invalid id error when route param is missing', () => {
    component.ngOnInit();

    expect(component.error()).toBe('Invalid declaration ID.');
  });

  it('should set error when declaration load is unsuccessful', () => {
    (component as any).adminService.getDeclarationById = () =>
      of({ success: false, error: 'Declaration not found.' });

    component.declarationId.set(123);
    component.loadDeclaration();

    expect(component.error()).toBe('Declaration not found.');
  });

});
