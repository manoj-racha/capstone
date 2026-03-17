import { HttpEvent, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ToastService } from '../services/toast.service';

interface ApiLikeResponse {
  success?: boolean;
  message?: string;
  error?: string;
}

export const toastInterceptor: HttpInterceptorFn = (req, next): Observable<HttpEvent<unknown>> => {
  const toastService = inject(ToastService);

  return next(req).pipe(
    tap((event) => {
      if (!(event instanceof HttpResponse)) {
        return;
      }

      const body = event.body as ApiLikeResponse | null;
      if (!body) {
        return;
      }

      const isMutatingRequest = req.method !== 'GET';
      if (isMutatingRequest && body.success && body.message) {
        toastService.success(body.message);
      }
    })
  );
};
