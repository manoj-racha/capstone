import { Component, inject } from '@angular/core';
import { LoadingService } from '../../../core/services/loading.service';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-global-loader',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './global-loader.component.html'
})
export class GlobalLoaderComponent {
    loadingService = inject(LoadingService);
}
