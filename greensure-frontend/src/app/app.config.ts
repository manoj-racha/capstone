import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth-interceptor';
import { loadingInterceptor } from './core/interceptors/loading.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    // Tells Angular to use zone-based change detection
    // (more compatible than zoneless for Angular 21 beginners)
    provideZoneChangeDetection({ eventCoalescing: true }),

    // Registers all our routes defined in app.routes.ts
    provideRouter(routes),

    // Enables HttpClient so all our services can make API calls, with Auth & Loading interceptors
    provideHttpClient(withInterceptors([authInterceptor, loadingInterceptor]))
  ]
};