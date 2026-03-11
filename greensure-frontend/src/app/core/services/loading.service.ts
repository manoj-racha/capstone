import { Injectable, signal } from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class LoadingService {

    private requestCount = 0;
    private hideTimeout: any;

    // Public signal that components can react to
    readonly isLoading = signal<boolean>(false);

    show(): void {
        this.requestCount++;
        // Clear any pending hide
        if (this.hideTimeout) {
            clearTimeout(this.hideTimeout);
            this.hideTimeout = null;
        }
        if (this.requestCount > 0 && !this.isLoading()) {
            this.isLoading.set(true);
        }
    }

    hide(): void {
        this.requestCount--;
        if (this.requestCount <= 0) {
            this.requestCount = 0;
            // Debounce the hiding by 100ms
            this.hideTimeout = setTimeout(() => {
                if (this.requestCount === 0) {
                    this.isLoading.set(false);
                }
            }, 100);
        }
    }
}
