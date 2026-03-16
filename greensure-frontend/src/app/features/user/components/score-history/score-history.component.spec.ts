import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { ScoreHistoryComponent } from './score-history.component';

describe('ScoreHistoryComponent', () => {
  let component: ScoreHistoryComponent;
  let fixture: ComponentFixture<ScoreHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ScoreHistoryComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ScoreHistoryComponent);
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

  it('should set error when history request fails', () => {
    (component as any).scoreService.getMyHistory = () => of({ success: false, error: 'History failed' });

    component.ngOnInit();

    expect(component.error()).toBe('History failed');
  });

});
