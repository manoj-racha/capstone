import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { ScoreService } from './score.service';
import { environment } from '../../environments/environment';

describe('ScoreService', () => {
  let service: ScoreService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ScoreService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(ScoreService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should get latest user score', () => {
    service.getMyScore().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/score/my-score`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {} });
  });

  it('should get score history', () => {
    service.getMyHistory().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/score/my-history`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [] });
  });
});
