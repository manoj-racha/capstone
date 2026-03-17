import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastMessage {
  id: number;
  type: ToastType;
  message: string;
  createdAt: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private nextId = 1;
  private readonly timeoutMs = 3000;

  readonly toasts = signal<ToastMessage[]>([]);

  show(type: ToastType, message: string): void {
    const toast: ToastMessage = {
      id: this.nextId++,
      type,
      message,
      createdAt: Date.now()
    };

    this.toasts.update((current) => [...current, toast]);

    setTimeout(() => {
      this.remove(toast.id);
    }, this.timeoutMs);
  }

  success(message: string): void {
    this.show('success', message);
  }

  error(message: string): void {
    this.show('error', message);
  }

  warning(message: string): void {
    this.show('warning', message);
  }

  info(message: string): void {
    this.show('info', message);
  }

  remove(id: number): void {
    this.toasts.update((current) => current.filter((toast) => toast.id !== id));
  }
}
