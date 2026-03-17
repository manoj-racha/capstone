import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-global-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './global-toast.component.html'
})
export class GlobalToastComponent {
  readonly toastService = inject(ToastService);

  toastClass(type: string): string {
    switch (type) {
      case 'success':
        return 'border-green-500/40 bg-green-50 text-green-700';
      case 'error':
        return 'border-red-500/40 bg-red-50 text-red-700';
      case 'warning':
        return 'border-amber-500/40 bg-amber-50 text-amber-700';
      default:
        return 'border-blue-500/40 bg-blue-50 text-blue-700';
    }
  }
}
