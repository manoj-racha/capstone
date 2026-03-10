import { Injectable, signal } from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class LoadingService {

    private requestCount = 0;

    // Public signal that components can react to
    readonly isLoading = signal<boolean>(false);

    show(): void {
        this.requestCount++;
        if (this.requestCount === 1) {
            this.isLoading.set(true);
        }
    }

    hide(): void {
        this.requestCount--;
        if (this.requestCount <= 0) {
            this.requestCount = 0;
            this.isLoading.set(false);
        }
    }
}
