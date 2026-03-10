import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { LoadingService } from '../services/loading.service';

export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
    const loadingService = inject(LoadingService);

    // Show loader before passing request handle
    loadingService.show();

    return next(req).pipe(
        // Ensure loader is hidden when request finishes (success or error)
        finalize(() => {
            loadingService.hide();
        })
    );
};
