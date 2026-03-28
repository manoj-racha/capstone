import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AdminService } from './admin.service';

describe('AdminService', () => {
  let service: AdminService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AdminService, provideHttpClient()]
    });
    service = TestBed.inject(AdminService);
  });

  it('should create', () => {
    expect(service).toBeTruthy();
  });
});
