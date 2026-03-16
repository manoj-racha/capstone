import { TestBed } from '@angular/core/testing';

import { LoadingService } from './loading.service';

describe('LoadingService', () => {
  let service: LoadingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LoadingService]
    });

    service = TestBed.inject(LoadingService);
  });

  it('should start as not loading', () => {
    expect(service.isLoading()).toBe(false);
  });

  it('should set loading true on show', () => {
    service.show();

    expect(service.isLoading()).toBe(true);
  });

  it('should keep loading true until all requests are hidden', async () => {
    service.show();
    service.show();

    service.hide();
    await new Promise((resolve) => setTimeout(resolve, 120));
    expect(service.isLoading()).toBe(true);

    service.hide();
    await new Promise((resolve) => setTimeout(resolve, 120));
    expect(service.isLoading()).toBe(false);
  });

  it('should cancel pending hide when show is called again', async () => {
    service.show();
    service.hide();

    await new Promise((resolve) => setTimeout(resolve, 50));
    service.show();

    await new Promise((resolve) => setTimeout(resolve, 120));
    expect(service.isLoading()).toBe(true);
  });
});
